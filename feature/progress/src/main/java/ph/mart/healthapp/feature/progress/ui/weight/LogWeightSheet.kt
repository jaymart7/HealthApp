package ph.mart.healthapp.feature.progress.ui.weight

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import org.koin.androidx.compose.koinViewModel
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect
import ph.mart.healthapp.core.data.nowMinuteOfDay
import ph.mart.healthapp.core.data.profile.UnitSystem
import ph.mart.healthapp.core.data.profile.displayUnitToKg
import ph.mart.healthapp.core.data.profile.kgToDisplayUnit
import ph.mart.healthapp.core.data.profile.weightUnitLabel
import ph.mart.healthapp.core.data.progress.NOTE_GOOGLE_HEALTH
import ph.mart.healthapp.core.data.progress.WeightEntry
import ph.mart.healthapp.core.data.progress.isImported
import ph.mart.healthapp.core.data.todayEpochDay
import ph.mart.healthapp.core.designsystem.component.AppBottomSheet
import ph.mart.healthapp.core.designsystem.component.AppTextField
import ph.mart.healthapp.core.designsystem.component.DiscardConfirmDialog
import ph.mart.healthapp.core.designsystem.component.NumericStepperField
import ph.mart.healthapp.core.designsystem.component.PrimaryButton
import ph.mart.healthapp.core.designsystem.component.SheetDatePicker
import ph.mart.healthapp.core.designsystem.component.TextButton
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.progress.R
import ph.mart.healthapp.feature.progress.ui.shared.components.Note

/**
 * Logging a weigh-in, and — when [entry] is one of the Weight page's records — editing or deleting
 * it. The repository upserts by date, so saving an [entry] replaces it rather than adding a second
 * row for the day; that is the whole of the edit.
 */
@Composable
fun LogWeightSheet(
    onDismiss: () -> Unit,
    entry: WeightEntry? = null,
    viewModel: LogWeightViewModel = koinViewModel(),
) {
    val uiState by viewModel.collectAsState()
    val state = rememberLogWeightState(
        entry?.let {
            LogWeightForm(
                dateEpochDay = it.dateEpochDay,
                weightKg = it.weightKg,
                note = it.note,
                // A row from before the field existed has no time to re-seed from, so the sheet
                // opens at now rather than inventing a midnight.
                minuteOfDay = it.minuteOfDay ?: nowMinuteOfDay(),
            )
        } ?: LogWeightForm(),
    )
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

    AppBottomSheet(title = stringResource(R.string.progress_weight_log), onDismiss = onDismiss) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SheetDatePicker(
                showingCalendar = state.showingCalendar,
                onShowCalendar = { state.showingCalendar = true },
                onBackToFields = { state.showingCalendar = false },
                selectedDate = state.form.dateEpochDay,
                markedDates = uiState.entries.map { it.dateEpochDay }.toSet(),
                onSelectDate = { date ->
                    val existing = uiState.entries.find { it.dateEpochDay == date }
                    state.form = state.form.copy(
                        dateEpochDay = date,
                        weightKg = existing?.weightKg ?: state.form.weightKg,
                        // Follows the weight: landing on a day that already has a weigh-in shows
                        // that weigh-in, hour and all, because saving is going to replace it.
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
                            text = stringResource(R.string.progress_weight_replacing),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    NumericStepperField(
                        label = stringResource(R.string.progress_weight_label),
                        value = formatWeight(state.form.weightKg.kgToDisplayUnit(unit)),
                        unitSuffix = unit.weightUnitLabel(),
                        onIncrement = { state.form = state.form.copy(weightKg = state.form.weightKg + step) },
                        onDecrement = { state.form = state.form.copy(weightKg = (state.form.weightKg - step).coerceAtLeast(20.0)) },
                        // Typed figures are not clamped: a half-typed "7" on the way to "75" would
                        // snap to the floor and make the field impossible to retype. The save
                        // clamps instead.
                        onValueChange = { state.form = state.form.copy(weightKg = (it.toDoubleOrNull() ?: 0.0).displayUnitToKg(unit)) },
                        decimal = true,
                    )
                    AppTextField(
                        label = stringResource(R.string.progress_weight_note),
                        value = state.form.note,
                        onValueChange = { state.form = state.form.copy(note = it) },
                    )
                }
            }

            if (!state.showingCalendar) {
                PrimaryButton(
                    label = stringResource(R.string.progress_save),
                    onClick = { onEvent(LogWeightEvent.OnSave(state.form)) },
                    modifier = Modifier.fillMaxWidth(),
                )
                // Follows the date picker rather than the row the sheet opened on: whatever day is
                // selected is the day this deletes.
                existingForDate?.let { existing ->
                    if (existing.isImported()) {
                        // No delete on a provider's copy — the next sync would simply bring it back.
                        Text(
                            text = stringResource(R.string.progress_weight_imported, existing.note),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                        )
                    } else {
                        TextButton(
                            label = stringResource(R.string.progress_delete),
                            onClick = { state.confirmingDelete = true },
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                        )
                    }
                }
            }
        }
    }

    if (state.confirmingDelete) {
        DiscardConfirmDialog(
            title = stringResource(R.string.progress_weight_delete_title),
            body = stringResource(R.string.progress_weight_delete_body),
            confirmLabel = stringResource(R.string.progress_delete),
            dismissLabel = stringResource(R.string.progress_cancel),
            onConfirm = {
                state.confirmingDelete = false
                onEvent(LogWeightEvent.OnDelete(state.form.dateEpochDay))
            },
            onDismiss = { state.confirmingDelete = false },
        )
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

/** Opened on one of the Weight page's records: the day already has an entry, so the sheet says it
 * is replacing it and offers the delete. */
@PreviewLightDark
@Composable
private fun LogWeightSheetEditPreview() {
    val entry = WeightEntry(dateEpochDay = 20_700L, weightKg = 76.5, note = "After the gym")
    AppTheme {
        LogWeightContent(
            uiState = LogWeightUiState(entries = listOf(entry)),
            state = LogWeightState(form = LogWeightForm(entry.dateEpochDay, entry.weightKg, entry.note)),
            onDismiss = {},
            onEvent = {},
        )
    }
}

/** The same day, imported: a caption where the delete would be. */
@PreviewLightDark
@Composable
private fun LogWeightSheetImportedPreview() {
    val entry = WeightEntry(dateEpochDay = 20_700L, weightKg = 76.5, note = NOTE_GOOGLE_HEALTH)
    AppTheme {
        LogWeightContent(
            uiState = LogWeightUiState(entries = listOf(entry)),
            state = LogWeightState(form = LogWeightForm(entry.dateEpochDay, entry.weightKg, entry.note)),
            onDismiss = {},
            onEvent = {},
        )
    }
}
