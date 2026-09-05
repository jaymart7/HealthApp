package ph.mart.healthapp.feature.progress.ui.measurement

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import org.koin.androidx.compose.koinViewModel
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect
import ph.mart.healthapp.core.data.profile.UnitSystem
import ph.mart.healthapp.core.data.profile.cmToDisplayUnit
import ph.mart.healthapp.core.data.profile.displayUnitToCm
import ph.mart.healthapp.core.data.profile.lengthUnitLabel
import ph.mart.healthapp.core.data.progress.MeasurementPart
import ph.mart.healthapp.core.designsystem.component.AppBottomSheet
import ph.mart.healthapp.core.designsystem.component.NumericStepperField
import ph.mart.healthapp.core.designsystem.component.PrimaryButton
import ph.mart.healthapp.core.designsystem.component.SecondaryButton
import ph.mart.healthapp.core.designsystem.component.SheetDatePicker
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.progress.R

@Composable
fun AddMeasurementSheet(
    trackedParts: Set<MeasurementPart>,
    preselectedPart: MeasurementPart?,
    unit: UnitSystem = UnitSystem.Metric,
    onDismiss: () -> Unit,
    viewModel: AddMeasurementViewModel = koinViewModel(),
) {
    val uiState by viewModel.collectAsState()
    val state = rememberAddMeasurementState(preselectedPart, trackedParts)
    viewModel.collectSideEffect { effect ->
        when (effect) {
            AddMeasurementSideEffect.Saved -> onDismiss()
        }
    }
    AddMeasurementContent(
        uiState = uiState,
        state = state,
        trackedParts = trackedParts,
        unit = unit,
        onDismiss = onDismiss,
        onEvent = viewModel::handleEvent,
    )
}

@Composable
private fun AddMeasurementContent(
    uiState: AddMeasurementUiState,
    state: AddMeasurementState,
    trackedParts: Set<MeasurementPart>,
    unit: UnitSystem,
    onDismiss: () -> Unit,
    onEvent: (AddMeasurementEvent) -> Unit,
) {
    val untrackedParts = MeasurementPart.entries.filter { it !in trackedParts }
    val part = state.form.part
    val existingForDate = part?.let { p -> uiState.entriesByPart[p]?.find { it.dateEpochDay == state.form.dateEpochDay } }
    val step = 0.5.displayUnitToCm(unit)

    AppBottomSheet(onDismiss = onDismiss) {
        Text(
            text = if (part != null && part !in untrackedParts) {
                stringResource(R.string.progress_measurement_log, part.name)
            } else {
                stringResource(R.string.progress_measurement_add_title)
            },
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 12.dp),
        )
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (part == null || part in untrackedParts) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    untrackedParts.forEach { candidate ->
                        val selected = candidate == part
                        Surface(
                            onClick = { state.form = state.form.copy(part = candidate) },
                            color = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
                            contentColor = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                            shape = RoundedCornerShape(999.dp),
                        ) {
                            Text(text = candidate.name, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp))
                        }
                    }
                }
            }

            SheetDatePicker(
                showingCalendar = state.showingCalendar,
                onShowCalendar = { state.showingCalendar = true },
                onBackToFields = { state.showingCalendar = false },
                selectedDate = state.form.dateEpochDay,
                markedDates = part?.let { p -> uiState.entriesByPart[p]?.map { it.dateEpochDay }?.toSet() } ?: emptySet(),
                onSelectDate = { date ->
                    val existing = part?.let { p -> uiState.entriesByPart[p]?.find { it.dateEpochDay == date } }
                    state.form = state.form.copy(dateEpochDay = date, valueCm = existing?.valueCm ?: state.form.valueCm)
                    state.showingCalendar = false
                },
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 12.dp)) {
                    if (existingForDate != null) {
                        Text(
                            text = stringResource(R.string.progress_measurement_replacing),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    NumericStepperField(
                        label = stringResource(R.string.progress_measurement_value_label),
                        value = formatValue(state.form.valueCm.cmToDisplayUnit(unit)),
                        unitSuffix = unit.lengthUnitLabel(),
                        onIncrement = { state.form = state.form.copy(valueCm = state.form.valueCm + step) },
                        onDecrement = { state.form = state.form.copy(valueCm = (state.form.valueCm - step).coerceAtLeast(10.0)) },
                    )
                }
            }

            if (!state.showingCalendar) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    SecondaryButton(label = stringResource(R.string.progress_cancel), onClick = onDismiss, modifier = Modifier.weight(1f))
                    PrimaryButton(
                        label = stringResource(R.string.progress_save),
                        onClick = { onEvent(AddMeasurementEvent.OnSave(state.form)) },
                        enabled = state.form.part != null,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

private fun formatValue(value: Double): String =
    if (value == value.toInt().toDouble()) value.toInt().toString() else "%.1f".format(value)

@PreviewLightDark
@Composable
private fun AddMeasurementSheetPreview() {
    AppTheme {
        AddMeasurementContent(
            uiState = AddMeasurementUiState(),
            state = AddMeasurementState(form = AddMeasurementForm(part = MeasurementPart.Waist)),
            trackedParts = setOf(MeasurementPart.Chest),
            unit = UnitSystem.Metric,
            onDismiss = {},
            onEvent = {},
        )
    }
}
