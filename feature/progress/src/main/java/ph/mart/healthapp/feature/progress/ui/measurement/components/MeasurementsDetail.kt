package ph.mart.healthapp.feature.progress.ui.measurement.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.data.progress.MeasurementEntry
import ph.mart.healthapp.core.data.progress.MeasurementPart
import ph.mart.healthapp.core.data.progress.WAIST_TO_HEIGHT_HEALTHY_MAX
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
 * A list, not a chart — five body parts each with their own sparse history, which is a table of
 * rows rather than a series with an axis. No range toggle for the same reason: there is nothing to
 * slice, and every part's whole history fits in its row's sparkline.
 *
 * The one subject page with a write on it besides Blood pressure, because the sheet it opens is the
 * screen's own — tapping a row pre-fills it with that part.
 */
@Composable
internal fun MeasurementsDetailBody(uiState: ProgressUiState, state: ProgressScreenState) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        WaistToHeightCard(uiState = uiState)
        MeasurementPart.entries.filter { it in uiState.measurements }.forEach { part ->
            MeasurementRow(
                name = part.name,
                historyCm = uiState.measurements[part].orEmpty().sortedBy { it.dateEpochDay }.map { it.valueCm },
                unit = uiState.preferredUnit,
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
 * The page's one derived figure, above the rows it is derived from, and gone entirely until both
 * its inputs exist — there is no dash state, because a ratio with a side missing is not a reading
 * that hasn't been taken, it is a ratio that doesn't exist.
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
        ?.valueCm
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
                measurements = mapOf(
                    MeasurementPart.Waist to listOf(
                        MeasurementEntry(MeasurementPart.Waist, today - 30, 88.0),
                        MeasurementEntry(MeasurementPart.Waist, today, 85.5),
                    ),
                ),
                heightCm = 178.0,
            ),
            state = ProgressScreenState(),
        )
    }
}
