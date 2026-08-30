package ph.mart.healthapp.feature.progress.ui.weight

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import org.koin.androidx.compose.koinViewModel
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect
import ph.mart.healthapp.core.data.profile.UnitSystem
import ph.mart.healthapp.core.data.profile.displayUnitToKg
import ph.mart.healthapp.core.data.profile.kgToDisplayUnit
import ph.mart.healthapp.core.data.profile.weightUnitLabel
import ph.mart.healthapp.core.designsystem.component.AppBottomSheet
import ph.mart.healthapp.core.designsystem.component.AppTextField
import ph.mart.healthapp.core.designsystem.component.NumericStepperField
import ph.mart.healthapp.core.designsystem.component.PrimaryButton
import ph.mart.healthapp.core.designsystem.component.SecondaryButton
import ph.mart.healthapp.core.designsystem.component.SheetDatePicker
import ph.mart.healthapp.core.designsystem.component.todayEpochDay
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.progress.ui.progress.components.Note

@Composable
fun LogWeightSheet(onDismiss: () -> Unit, viewModel: LogWeightViewModel = koinViewModel()) {
    val uiState by viewModel.collectAsState()
    val state = rememberLogWeightState()
    viewModel.collectSideEffect { effect ->
        when (effect) {
            is LogWeightSideEffect.Loaded -> if (state.form.dateEpochDay == todayEpochDay()) {
                state.form = state.form.copy(weightKg = effect.weightKg)
            }
            LogWeightSideEffect.Saved -> onDismiss()
        }
    }
    LogWeightContent(uiState = uiState, state = state, onDismiss = onDismiss, onEvent = viewModel::handleEvent)
}

@Composable
private fun LogWeightContent(
    uiState: LogWeightUiState,
    state: LogWeightState,
    onDismiss: () -> Unit,
    onEvent: (LogWeightEvent) -> Unit,
) {
    val unit = uiState.preferredUnit
    val step = 0.5.let { if (unit == UnitSystem.Imperial) 1.0.displayUnitToKg(unit) else it }
    val existingForDate = uiState.entries.find { it.dateEpochDay == state.form.dateEpochDay }

    AppBottomSheet(onDismiss = onDismiss) {
        Text(
            text = "Log weight",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 12.dp),
        )
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SheetDatePicker(
                showingCalendar = state.showingCalendar,
                onShowCalendar = { state.showingCalendar = true },
                onBackToFields = { state.showingCalendar = false },
                selectedDate = state.form.dateEpochDay,
                markedDates = uiState.entries.map { it.dateEpochDay }.toSet(),
                onSelectDate = { date ->
                    val existing = uiState.entries.find { it.dateEpochDay == date }
                    state.form = state.form.copy(dateEpochDay = date, weightKg = existing?.weightKg ?: state.form.weightKg)
                    state.showingCalendar = false
                },
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 12.dp)) {
                    if (existingForDate != null) {
                        Text(
                            text = "Replacing your entry for this date.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    NumericStepperField(
                        label = "Weight",
                        value = formatWeight(state.form.weightKg.kgToDisplayUnit(unit)),
                        unitSuffix = unit.weightUnitLabel(),
                        onIncrement = { state.form = state.form.copy(weightKg = state.form.weightKg + step) },
                        onDecrement = { state.form = state.form.copy(weightKg = (state.form.weightKg - step).coerceAtLeast(20.0)) },
                    )
                    AppTextField(
                        label = "Note (optional)",
                        value = state.form.note,
                        onValueChange = { state.form = state.form.copy(note = it) },
                    )
                }
            }

            if (!state.showingCalendar) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    SecondaryButton(label = "Cancel", onClick = onDismiss, modifier = Modifier.weight(1f))
                    PrimaryButton(label = "Save", onClick = { onEvent(LogWeightEvent.OnSave(state.form)) }, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

private fun formatWeight(value: Double): String =
    if (value == value.toInt().toDouble()) value.toInt().toString() else "%.1f".format(value)

@PreviewLightDark
@Composable
private fun LogWeightSheetPreview() {
    AppTheme {
        LogWeightContent(
            uiState = LogWeightUiState(),
            state = LogWeightState(form = LogWeightForm(weightKg = 76.5)),
            onDismiss = {},
            onEvent = {},
        )
    }
}
