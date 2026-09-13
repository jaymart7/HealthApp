package ph.mart.healthapp.feature.progress.ui.pressure

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import ph.mart.healthapp.core.data.bloodpressure.BloodPressureReading
import ph.mart.healthapp.core.data.bloodpressure.averages
import ph.mart.healthapp.core.data.bloodpressure.byDay
import ph.mart.healthapp.core.data.bloodpressure.inRange
import ph.mart.healthapp.core.data.todayEpochDay
import ph.mart.healthapp.core.designsystem.component.AppTopBar
import ph.mart.healthapp.core.designsystem.component.DiscardConfirmDialog
import ph.mart.healthapp.core.designsystem.component.FullScreenState
import ph.mart.healthapp.core.designsystem.component.MascotAvatar
import ph.mart.healthapp.core.designsystem.component.MascotState
import ph.mart.healthapp.core.designsystem.component.PrimaryButton
import ph.mart.healthapp.core.designsystem.icon.AppIcons
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.progress.R
import ph.mart.healthapp.feature.progress.ui.pressure.components.BloodPressureRow
import ph.mart.healthapp.feature.progress.ui.progress.Subject
import ph.mart.healthapp.feature.progress.ui.progress.components.ChartCard
import ph.mart.healthapp.feature.progress.ui.progress.components.FactChip
import ph.mart.healthapp.feature.progress.ui.progress.components.FactChipRow
import ph.mart.healthapp.feature.progress.ui.progress.components.HeroValue
import ph.mart.healthapp.feature.progress.ui.progress.components.LegendEntry
import ph.mart.healthapp.feature.progress.ui.progress.components.StatRow
import ph.mart.healthapp.feature.progress.ui.progress.components.StatRowsCard
import ph.mart.healthapp.feature.progress.ui.progress.components.SubjectSwitcher
import ph.mart.healthapp.feature.progress.ui.shared.components.RangeBar
import ph.mart.healthapp.feature.progress.ui.shared.components.RangeBarChart

/** Breathing room above and below the window's own readings, so no bar sits flat on an edge. */
private const val AXIS_PAD_MMHG = 10

/**
 * Every reading, charted and listed — a route of its own, and the page that makes `SelfScrolling`
 * redundant. Its list is per-*reading* rather than per-day, so a 3M window can hold a couple of
 * hundred rows; as a swap-in it had to be exempted from `SubjectDetail`'s scrolling column because
 * a `LazyColumn` cannot nest in one. A route owns its own column, so the exemption goes with it.
 *
 * It reads through [BloodPressureViewModel] and writes through [LogBloodPressureViewModel] — both
 * under this route's owner, which is what lets the list delete the row it is showing.
 */
@Composable
internal fun BloodPressureScreen(
    onSwitchSubject: (Subject) -> Unit,
    onOpenRecap: () -> Unit,
    onExitFlow: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BloodPressureViewModel = koinViewModel(),
    logViewModel: LogBloodPressureViewModel = koinViewModel(),
) {
    val uiState by viewModel.collectAsState()
    BloodPressureContent(
        readings = uiState.readings,
        cycleTrackingOn = uiState.cycleTrackingOn,
        onEvent = logViewModel::handleEvent,
        onSwitchSubject = onSwitchSubject,
        onOpenRecap = onOpenRecap,
        onExitFlow = onExitFlow,
        modifier = modifier,
    )
}

@Composable
private fun BloodPressureContent(
    readings: List<BloodPressureReading>,
    cycleTrackingOn: Boolean,
    onEvent: (BloodPressureEvent) -> Unit,
    onSwitchSubject: (Subject) -> Unit,
    onOpenRecap: () -> Unit,
    onExitFlow: () -> Unit,
    modifier: Modifier = Modifier,
    state: BloodPressureState = rememberBloodPressureState(),
) {
    Surface(color = MaterialTheme.colorScheme.surface, modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Zero insets: this bar sits in the scaffold's content, which its `innerPadding` has
            // already cleared of the status bar. See `SleepScreen`.
            AppTopBar(
                title = stringResource(Subject.BloodPressure.label),
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
            if (readings.isEmpty()) {
                Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                    Box(modifier = Modifier.weight(1f)) {
                        FullScreenState(
                            icon = { MascotAvatar(state = MascotState.Idle, size = 64.dp) },
                            heading = stringResource(R.string.progress_empty_pressure_heading),
                            body = stringResource(R.string.progress_empty_pressure_body),
                            // The other empty page with a button, beside Cycle's: the sheet it
                            // opens is already on this screen, so it adds no entry point.
                            actions = {
                                PrimaryButton(
                                    label = stringResource(R.string.progress_hint_pressure),
                                    onClick = { state.sheetOpen = true },
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            },
                        )
                    }
                    SubjectSwitcher(
                        subject = Subject.BloodPressure,
                        onSelect = onSwitchSubject,
                        modifier = Modifier.padding(bottom = 16.dp),
                        cycleTracking = cycleTrackingOn,
                    )
                }
            } else {
                BloodPressureList(
                    readings = readings,
                    cycleTrackingOn = cycleTrackingOn,
                    state = state,
                    onSwitchSubject = onSwitchSubject,
                )
            }
        }
    }

    if (state.sheetOpen) {
        LogBloodPressureSheet(onDismiss = { state.sheetOpen = false })
    }

    val pendingId = state.pendingDeleteReadingId
    if (pendingId != null) {
        // Asked rather than undone: the diary's swipe-and-undo needs a snackbar host Progress
        // doesn't have, and a reading is a number the user typed, not a row they swiped.
        DiscardConfirmDialog(
            title = stringResource(R.string.progress_bp_delete_title),
            body = stringResource(R.string.progress_bp_delete_body),
            confirmLabel = stringResource(R.string.progress_delete),
            dismissLabel = stringResource(R.string.progress_cancel),
            onConfirm = {
                onEvent(BloodPressureEvent.OnDelete(pendingId))
                state.pendingDeleteReadingId = null
            },
            onDismiss = { state.pendingDeleteReadingId = null },
        )
    }
}

/**
 * Windowed by date rather than sliced off the end, like heart, sleep and mood: the readings are
 * sparse, so the chart needs the window's bounds to know where the gaps are.
 *
 * No `DockedFabContentPadding` at the foot: this is a route, so there is no docked FAB to clear,
 * and the scaffold's own `innerPadding` already clears the navigation bar.
 */
@Composable
private fun BloodPressureList(
    readings: List<BloodPressureReading>,
    cycleTrackingOn: Boolean,
    state: BloodPressureState,
    onSwitchSubject: (Subject) -> Unit,
) {
    val today = todayEpochDay()
    val range = state.range
    val from = today - range.days
    val inWindow = readings.inRange(range, today)
    val averages = inWindow.averages()

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 16.dp),
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
    ) {
        item {
            HeroValue(
                value = averages.systolic?.let { "$it/${averages.diastolic}" } ?: "—",
                caption = stringResource(R.string.progress_bp_hero),
            )
        }
        item {
            FactChipRow(chips = listOf(FactChip(stringResource(R.string.progress_bp_readings_chip, averages.readings))))
        }
        item {
            ChartCard(
                title = stringResource(R.string.progress_bp_readings),
                range = range,
                onRangeChange = { state.range = it },
                legend = listOf(
                    LegendEntry(stringResource(R.string.progress_bp_legend), MaterialTheme.colorScheme.secondary),
                ),
            ) {
                // Each day as one bar spanning its mean diastolic up to its mean systolic.
                RangeBarChart(
                    bars = inWindow.byDay().map { RangeBar(it.dateEpochDay, low = it.diastolic, high = it.systolic) },
                    fromEpochDay = from,
                    toEpochDay = today,
                    axisPad = AXIS_PAD_MMHG,
                )
            }
        }
        item {
            StatRowsCard(
                rows = listOfNotNull(
                    StatRow(stringResource(R.string.progress_bp_systolic), statLabel(averages.systolic)),
                    StatRow(stringResource(R.string.progress_bp_diastolic), statLabel(averages.diastolic)),
                    // Dropped rather than "—" when no reading in the window carried one: the pulse
                    // keeps its own denominator, so an absent one is absent, not zero.
                    averages.pulseBpm?.let { StatRow(stringResource(R.string.progress_bp_pulse_stat), statLabel(it)) },
                    StatRow(stringResource(R.string.progress_bp_readings_stat), "${averages.readings}"),
                ),
            )
        }
        item {
            PrimaryButton(
                label = stringResource(R.string.progress_bp_add),
                onClick = { state.sheetOpen = true },
                modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
            )
        }
        // Newest first: the reading someone just took is the one they want to check or correct.
        items(inWindow.asReversed(), key = { it.id }) { reading ->
            BloodPressureRow(
                reading = reading,
                onDelete = { state.pendingDeleteReadingId = reading.id },
            )
        }
        item {
            SubjectSwitcher(
                subject = Subject.BloodPressure,
                onSelect = onSwitchSubject,
                cycleTracking = cycleTrackingOn,
            )
        }
    }
}

private fun statLabel(value: Int?): String = value?.toString() ?: "—"

private fun readingsPreview(): List<BloodPressureReading> =
    listOf(
        Triple(132, 86, 74), Triple(128, 82, 71), Triple(121, 79, 0), Triple(126, 84, 68),
    ).mapIndexed { index, (systolic, diastolic, pulse) ->
        BloodPressureReading(
            id = index.toLong() + 1,
            takenAtMillis = System.currentTimeMillis() - (3L - index) * 86_400_000L,
            systolic = systolic,
            diastolic = diastolic,
            pulseBpm = pulse,
        )
    }

@PreviewLightDark
@Composable
private fun BloodPressureScreenPreview() {
    AppTheme {
        BloodPressureContent(
            readings = readingsPreview(),
            cycleTrackingOn = true,
            onEvent = {},
            onSwitchSubject = {},
            onOpenRecap = {},
            onExitFlow = {},
        )
    }
}

/** Nothing logged yet — one of the two empty pages with a button, because its sheet is here. */
@PreviewLightDark
@Composable
private fun BloodPressureScreenEmptyPreview() {
    AppTheme {
        BloodPressureContent(
            readings = emptyList(),
            cycleTrackingOn = true,
            onEvent = {},
            onSwitchSubject = {},
            onOpenRecap = {},
            onExitFlow = {},
        )
    }
}
