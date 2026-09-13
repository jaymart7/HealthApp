package ph.mart.healthapp.feature.progress.ui.measurement

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import org.koin.androidx.compose.koinViewModel
import org.orbitmvi.orbit.compose.collectAsState
import ph.mart.healthapp.core.data.profile.UnitSystem
import ph.mart.healthapp.core.data.progress.MeasurementEntry
import ph.mart.healthapp.core.data.progress.MeasurementPart
import ph.mart.healthapp.core.data.progress.toDisplay
import ph.mart.healthapp.core.data.progress.unitLabel
import ph.mart.healthapp.core.data.todayEpochDay
import ph.mart.healthapp.core.designsystem.component.AppTopBar
import ph.mart.healthapp.core.designsystem.component.FullScreenState
import ph.mart.healthapp.core.designsystem.component.MascotAvatar
import ph.mart.healthapp.core.designsystem.component.MascotState
import ph.mart.healthapp.core.designsystem.component.PrimaryButton
import ph.mart.healthapp.core.designsystem.icon.AppIcons
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.progress.R
import ph.mart.healthapp.feature.progress.ui.measurement.components.BodyCompositionCard
import ph.mart.healthapp.feature.progress.ui.measurement.components.MeasurementRow
import ph.mart.healthapp.feature.progress.ui.measurement.components.WaistToHeightCard
import ph.mart.healthapp.feature.progress.ui.progress.Subject
import ph.mart.healthapp.feature.progress.ui.progress.components.SubjectSwitcher

/**
 * A list, not a chart — six readings each with their own sparse history, which is a table of rows
 * rather than a series with an axis. No range toggle for the same reason: there is nothing to
 * slice, and every part's whole history fits in its row's sparkline.
 *
 * A route of its own, `SleepScreen`'s shape, and one of the three converted pages that writes:
 * tapping a row opens the add sheet pre-filled with that part.
 */
@Composable
internal fun MeasurementsScreen(
    onSwitchSubject: (Subject) -> Unit,
    onOpenRecap: () -> Unit,
    onExitFlow: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MeasurementsViewModel = koinViewModel(),
) {
    val uiState by viewModel.collectAsState()
    MeasurementsContent(
        measurements = uiState.measurements,
        latestWeightKg = uiState.latestWeightKg,
        heightCm = uiState.heightCm,
        unit = uiState.unit,
        onSwitchSubject = onSwitchSubject,
        onOpenRecap = onOpenRecap,
        onExitFlow = onExitFlow,
        modifier = modifier,
    )
}

@Composable
private fun MeasurementsContent(
    measurements: Map<MeasurementPart, List<MeasurementEntry>>,
    latestWeightKg: Double?,
    heightCm: Double?,
    unit: UnitSystem,
    onSwitchSubject: (Subject) -> Unit,
    onOpenRecap: () -> Unit,
    onExitFlow: () -> Unit,
    modifier: Modifier = Modifier,
    state: MeasurementsState = rememberMeasurementsState(),
) {
    val tracked = measurements.filterValues { it.isNotEmpty() }

    Surface(color = MaterialTheme.colorScheme.surface, modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Zero insets: this bar sits in the scaffold's content, which its `innerPadding` has
            // already cleared of the status bar. See `SleepScreen`.
            AppTopBar(
                title = stringResource(Subject.Measurements.label),
                onBack = onExitFlow,
                windowInsets = WindowInsets(0),
                actions = {
                    IconButton(onClick = onOpenRecap) {
                        Icon(
                            imageVector = AppIcons.Share,
                            contentDescription = stringResource(R.string.progress_recap),
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                },
            )
            if (tracked.isEmpty()) {
                Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                    Box(modifier = Modifier.weight(1f)) {
                        FullScreenState(
                            icon = { MascotAvatar(state = MascotState.Idle, size = 64.dp) },
                            heading = stringResource(R.string.progress_empty_measurements_heading),
                            body = stringResource(R.string.progress_empty_measurements_body),
                            // A button here for the reason Cycle's and Blood pressure's have one:
                            // the sheet it opens is already on this page.
                            actions = {
                                PrimaryButton(
                                    label = stringResource(R.string.progress_measurement_add),
                                    onClick = { state.openSheet(null) },
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            },
                        )
                    }
                    SubjectSwitcher(
                        subject = Subject.Measurements,
                        onSelect = onSwitchSubject,
                        modifier = Modifier.padding(bottom = 16.dp),
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    MeasurementsBody(
                        measurements = measurements,
                        latestWeightKg = latestWeightKg,
                        heightCm = heightCm,
                        unit = unit,
                        state = state,
                    )
                    SubjectSwitcher(subject = Subject.Measurements, onSelect = onSwitchSubject)
                }
            }
        }
    }

    if (state.sheetOpen) {
        AddMeasurementSheet(
            trackedParts = measurements.keys,
            preselectedPart = state.part,
            unit = unit,
            onDismiss = { state.sheetOpen = false },
        )
    }
}

@Composable
private fun ColumnScope.MeasurementsBody(
    measurements: Map<MeasurementPart, List<MeasurementEntry>>,
    latestWeightKg: Double?,
    heightCm: Double?,
    unit: UnitSystem,
    state: MeasurementsState,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        WaistToHeightCard(
            waistCm = newest(measurements, MeasurementPart.Waist),
            heightCm = heightCm,
        )
        BodyCompositionCard(
            bodyFatPercent = newest(measurements, MeasurementPart.BodyFat),
            weightKg = latestWeightKg,
            unit = unit,
        )
        MeasurementPart.entries.filter { it in measurements }.forEach { part ->
            MeasurementRow(
                name = stringResource(part.label),
                history = measurements[part]
                    .orEmpty()
                    .sortedBy { it.dateEpochDay }
                    .map { part.toDisplay(it.value, unit) },
                unitLabel = part.unitLabel(unit),
                onTap = { state.openSheet(part) },
            )
        }
        PrimaryButton(
            label = stringResource(R.string.progress_measurement_add),
            onClick = { state.openSheet(null) },
            modifier = Modifier.padding(top = 12.dp),
        )
    }
}

/** The latest reading for [part], in stored units — what both derived cards are priced against. */
private fun newest(
    measurements: Map<MeasurementPart, List<MeasurementEntry>>,
    part: MeasurementPart,
): Double? = measurements[part].orEmpty().maxByOrNull { it.dateEpochDay }?.value

private fun measurementsPreview(today: Long): Map<MeasurementPart, List<MeasurementEntry>> = mapOf(
    MeasurementPart.Waist to listOf(
        MeasurementEntry(MeasurementPart.Waist, today - 30, 88.0),
        MeasurementEntry(MeasurementPart.Waist, today, 85.5),
    ),
    MeasurementPart.BodyFat to listOf(
        MeasurementEntry(MeasurementPart.BodyFat, today - 30, 19.5),
        MeasurementEntry(MeasurementPart.BodyFat, today, 18.0),
    ),
)

@PreviewLightDark
@Composable
private fun MeasurementsScreenPreview() {
    AppTheme {
        MeasurementsContent(
            measurements = measurementsPreview(todayEpochDay()),
            latestWeightKg = 82.0,
            heightCm = 178.0,
            unit = UnitSystem.Metric,
            onSwitchSubject = {},
            onOpenRecap = {},
            onExitFlow = {},
        )
    }
}

/** Nothing measured yet — an empty page with a button, because its sheet is already here. */
@PreviewLightDark
@Composable
private fun MeasurementsScreenEmptyPreview() {
    AppTheme {
        MeasurementsContent(
            measurements = emptyMap(),
            latestWeightKg = null,
            heightCm = null,
            unit = UnitSystem.Metric,
            onSwitchSubject = {},
            onOpenRecap = {},
            onExitFlow = {},
        )
    }
}
