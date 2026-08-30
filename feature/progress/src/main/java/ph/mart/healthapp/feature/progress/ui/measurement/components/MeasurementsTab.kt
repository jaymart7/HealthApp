package ph.mart.healthapp.feature.progress.ui.measurement.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.data.progress.MeasurementEntry
import ph.mart.healthapp.core.data.progress.MeasurementPart
import ph.mart.healthapp.core.designsystem.component.PrimaryButton
import ph.mart.healthapp.core.designsystem.component.todayEpochDay
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.progress.ui.progress.ProgressScreenState
import ph.mart.healthapp.feature.progress.ui.progress.ProgressUiState

@Composable
internal fun MeasurementsTabContent(uiState: ProgressUiState, state: ProgressScreenState) {
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
            label = "+ Add measurement",
            onClick = { state.openMeasurementSheet(null) },
            modifier = Modifier.padding(top = 12.dp),
        )
    }
}

@PreviewLightDark
@Composable
private fun MeasurementsTabPreview() {
    val today = todayEpochDay()
    AppTheme {
        MeasurementsTabContent(
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

/** Nothing tracked yet: the tab is just its add button. */
@PreviewLightDark
@Composable
private fun MeasurementsTabEmptyPreview() {
    AppTheme { MeasurementsTabContent(uiState = ProgressUiState(), state = ProgressScreenState()) }
}
