package ph.mart.healthapp.feature.progress.ui.measurement.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.data.profile.kgToDisplayUnit
import ph.mart.healthapp.core.data.profile.weightUnitLabel
import ph.mart.healthapp.core.data.progress.MeasurementEntry
import ph.mart.healthapp.core.data.progress.MeasurementPart
import ph.mart.healthapp.core.data.progress.WAIST_TO_HEIGHT_HEALTHY_MAX
import ph.mart.healthapp.core.data.progress.WeightEntry
import ph.mart.healthapp.core.data.progress.fatMassKgOf
import ph.mart.healthapp.core.data.progress.leanMassKgOf
import ph.mart.healthapp.core.data.progress.toDisplay
import ph.mart.healthapp.core.data.progress.unitLabel
import ph.mart.healthapp.core.data.progress.waistToHeightOf
import ph.mart.healthapp.core.data.todayEpochDay
import ph.mart.healthapp.core.designsystem.component.PrimaryButton
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.progress.R
import ph.mart.healthapp.feature.progress.ui.progress.ProgressScreenState
import ph.mart.healthapp.feature.progress.ui.progress.ProgressUiState
import ph.mart.healthapp.feature.progress.ui.progress.components.StatRow
import ph.mart.healthapp.feature.progress.ui.progress.components.StatRowsCard

/**
 * A list, not a chart — six readings each with their own sparse history, which is a table of rows
 * rather than a series with an axis. No range toggle for the same reason: there is nothing to
 * slice, and every part's whole history fits in its row's sparkline.
 *
 * The one subject page with a write on it besides Blood pressure, because the sheet it opens is the
 * screen's own — tapping a row pre-fills it with that part.
 */
@Composable
internal fun MeasurementsDetailBody(uiState: ProgressUiState, state: ProgressScreenState) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        WaistToHeightCard(uiState = uiState)
        BodyCompositionCard(uiState = uiState)
        MeasurementPart.entries.filter { it in uiState.measurements }.forEach { part ->
            MeasurementRow(
                name = stringResource(part.label),
                history = uiState.measurements[part]
                    .orEmpty()
                    .sortedBy { it.dateEpochDay }
                    .map { part.toDisplay(it.value, uiState.preferredUnit) },
                unitLabel = part.unitLabel(uiState.preferredUnit),
                onTap = { state.openMeasurementSheet(part) },
            )
        }
        PrimaryButton(
            label = stringResource(R.string.progress_measurement_add),
            onClick = { state.openMeasurementSheet(null) },
            modifier = Modifier.padding(top = 12.dp),
        )
    }
}

/**
 * The first of the page's two derived cards, above the rows they are derived from, and gone
 * entirely until both its inputs exist — there is no dash state, because a ratio with a side
 * missing is not a reading that hasn't been taken, it is a ratio that doesn't exist.
 *
 * Two rows rather than one: the number means nothing without the published boundary beside it, and
 * [StatRow] carries a label and a value, not a caption. Both are `Neutral` — the boundary is
 * reported, never graded, which is the same line the absent fertile window draws on the Cycle page.
 */
@Composable
private fun WaistToHeightCard(uiState: ProgressUiState) {
    val waistCm = uiState.measurements[MeasurementPart.Waist]
        .orEmpty()
        .maxByOrNull { it.dateEpochDay }
        ?.value
        ?: return
    val heightCm = uiState.heightCm ?: return
    val ratio = waistToHeightOf(waistCm, heightCm) ?: return

    StatRowsCard(
        rows = listOf(
            StatRow(
                label = stringResource(R.string.progress_measurement_waist_height),
                value = formatRatio(ratio),
            ),
            StatRow(
                label = stringResource(R.string.progress_measurement_waist_height_healthy),
                value = stringResource(
                    R.string.progress_measurement_waist_height_max,
                    formatRatio(WAIST_TO_HEIGHT_HEALTHY_MAX),
                ),
            ),
        ),
        modifier = Modifier.padding(bottom = 8.dp),
    )
}

/**
 * The second, and the only thing on this page that reads a weigh-in: the newest body fat reading
 * against the newest weight, split into what is fat and what is not. Same rule as the card above —
 * both rows or neither, no dash state, and no band or target beside them, because there is no one
 * published healthy body fat range to name the way [BmiCategory][ph.mart.healthapp.core.data.progress.BmiCategory]
 * names the WHO bands.
 *
 * These two *do* convert: a mass is a mass, where the ratio above is the same number in either
 * unit system.
 */
@Composable
private fun BodyCompositionCard(uiState: ProgressUiState) {
    val percent = uiState.measurements[MeasurementPart.BodyFat]
        .orEmpty()
        .maxByOrNull { it.dateEpochDay }
        ?.value
        ?: return
    val weightKg = uiState.weightEntries.maxByOrNull { it.dateEpochDay }?.weightKg ?: return
    val fatKg = fatMassKgOf(weightKg, percent) ?: return
    val leanKg = leanMassKgOf(weightKg, percent) ?: return
    val unit = uiState.preferredUnit

    StatRowsCard(
        rows = listOf(
            StatRow(
                label = stringResource(R.string.progress_measurement_fat_mass),
                value = stringResource(
                    R.string.progress_measurement_value,
                    formatMeasurement(fatKg.kgToDisplayUnit(unit)),
                    unit.weightUnitLabel(),
                ),
            ),
            StatRow(
                label = stringResource(R.string.progress_measurement_lean_mass),
                value = stringResource(
                    R.string.progress_measurement_value,
                    formatMeasurement(leanKg.kgToDisplayUnit(unit)),
                    unit.weightUnitLabel(),
                ),
            ),
        ),
        modifier = Modifier.padding(bottom = 8.dp),
    )
}

/** Two decimals always — the whole figure lives between 0.4 and 0.6, so a dropped one would put
 * every reading on the boundary it is being compared against. */
private fun formatRatio(value: Double): String = "%.2f".format(value)

@PreviewLightDark
@Composable
private fun MeasurementsDetailPreview() {
    val today = todayEpochDay()
    AppTheme {
        MeasurementsDetailBody(
            uiState = ProgressUiState(
                weightEntries = listOf(WeightEntry(today, 82.0)),
                measurements = mapOf(
                    MeasurementPart.Waist to listOf(
                        MeasurementEntry(MeasurementPart.Waist, today - 30, 88.0),
                        MeasurementEntry(MeasurementPart.Waist, today, 85.5),
                    ),
                    MeasurementPart.BodyFat to listOf(
                        MeasurementEntry(MeasurementPart.BodyFat, today - 30, 19.5),
                        MeasurementEntry(MeasurementPart.BodyFat, today, 18.0),
                    ),
                ),
                heightCm = 178.0,
            ),
            state = ProgressScreenState(),
        )
    }
}
