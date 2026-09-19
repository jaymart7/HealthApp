package ph.mart.healthapp.feature.progress.ui.measurement

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
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
import ph.mart.healthapp.core.data.progress.MeasurementPart
import ph.mart.healthapp.core.data.progress.defaultValue
import ph.mart.healthapp.core.data.progress.fromDisplay
import ph.mart.healthapp.core.data.progress.range
import ph.mart.healthapp.core.data.progress.toDisplay
import ph.mart.healthapp.core.data.progress.unitLabel
import ph.mart.healthapp.core.designsystem.component.AppBottomSheet
import ph.mart.healthapp.core.designsystem.component.NumericStepperField
import ph.mart.healthapp.core.designsystem.component.PrimaryButton
import ph.mart.healthapp.core.designsystem.component.SheetDatePicker
import ph.mart.healthapp.core.designsystem.component.formatOneDecimal
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
    // Nothing picked yet still draws the field, and a length is the shape five of the six parts
    // take — the stepper re-seeds itself the moment a chip is tapped.
    val kind = part ?: MeasurementPart.Chest
    val step = kind.fromDisplay(0.5, unit)

    AppBottomSheet(
        title = if (part != null && part !in untrackedParts) {
            stringResource(R.string.progress_measurement_log, stringResource(part.label))
        } else {
            stringResource(R.string.progress_measurement_add_title)
        },
        onDismiss = onDismiss,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (part == null || part in untrackedParts) {
                // Wraps, because six chips do not fit one phone-width line and the day nothing is
                // tracked yet is the day all six are offered.
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    untrackedParts.forEach { candidate ->
                        val selected = candidate == part
                        Surface(
                            // Switching part switches units and scale with it, so the figure has
                            // to follow: that day's reading for the new part, or its own opening
                            // figure. Carrying 80 over from a waist onto body fat would be absurd.
                            onClick = {
                                val existing = uiState.entriesByPart[candidate]?.find { it.dateEpochDay == state.form.dateEpochDay }
                                state.form = state.form.copy(part = candidate, value = existing?.value ?: candidate.defaultValue())
                            },
                            color = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
                            contentColor = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                            shape = RoundedCornerShape(999.dp),
                        ) {
                            Text(text = stringResource(candidate.label), modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp))
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
                    state.form = state.form.copy(
                        dateEpochDay = date,
                        value = existing?.value ?: state.form.value,
                        // Follows the figure: a day that already has this part's reading shows it
                        // as it was taken, because saving replaces it.
                        minuteOfDay = existing?.minuteOfDay ?: state.form.minuteOfDay,
                    )
                    state.showingCalendar = false
                },
                selectedMinuteOfDay = state.form.minuteOfDay,
                onSelectTime = { state.form = state.form.copy(minuteOfDay = it) },
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
                        value = formatOneDecimal(kind.toDisplay(state.form.value, unit)),
                        unitSuffix = kind.unitLabel(unit),
                        onIncrement = { state.form = state.form.copy(value = (state.form.value + step).coerceIn(kind.range())) },
                        onDecrement = { state.form = state.form.copy(value = (state.form.value - step).coerceIn(kind.range())) },
                        // Unclamped while typing, for the reason the weight sheet is: the save
                        // coerces into the part's range.
                        onValueChange = { state.form = state.form.copy(value = kind.fromDisplay(it.toDoubleOrNull() ?: 0.0, unit)) },
                        decimal = true,
                    )
                }
            }

            if (!state.showingCalendar) {
                PrimaryButton(
                    label = stringResource(R.string.progress_save),
                    onClick = { onEvent(AddMeasurementEvent.OnSave(state.form)) },
                    enabled = state.form.part != null,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

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
