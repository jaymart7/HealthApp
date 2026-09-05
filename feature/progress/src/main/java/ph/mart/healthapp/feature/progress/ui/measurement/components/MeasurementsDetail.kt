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
import ph.mart.healthapp.core.data.todayEpochDay
import ph.mart.healthapp.core.designsystem.component.PrimaryButton
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.progress.R
import ph.mart.healthapp.feature.progress.ui.progress.ProgressScreenState
import ph.mart.healthapp.feature.progress.ui.progress.ProgressUiState

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
            ),
            state = ProgressScreenState(),
        )
    }
}

