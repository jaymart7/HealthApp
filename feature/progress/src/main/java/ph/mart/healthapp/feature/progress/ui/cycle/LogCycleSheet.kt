package ph.mart.healthapp.feature.progress.ui.cycle

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import org.koin.androidx.compose.koinViewModel
import org.orbitmvi.orbit.compose.collectSideEffect
import ph.mart.healthapp.core.data.cycle.CycleDay
import ph.mart.healthapp.core.data.cycle.CycleSymptom
import ph.mart.healthapp.core.data.cycle.FlowLevel
import ph.mart.healthapp.core.data.cycle.TAPPABLE_FLOW
import ph.mart.healthapp.core.designsystem.component.AppBottomSheet
import ph.mart.healthapp.core.designsystem.component.PrimaryButton
import ph.mart.healthapp.core.designsystem.component.SecondaryButton
import ph.mart.healthapp.core.designsystem.component.SegmentedToggle
import ph.mart.healthapp.core.designsystem.component.SheetDatePicker
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.progress.R

private val NOT_LOGGED = R.string.progress_cycle_not_logged

/**
 * Log or correct one cycle day. Unlike the blood-pressure sheet it *does* ask for a date: a period
 * is remembered in the evening as often as it is logged in the morning, and back-filling three days
 * is the workflow — the same reason the weigh-in sheet has a calendar.
 *
 * The day is seeded from whatever is already logged there, so opening a day twice shows what it
 * says rather than a blank form, and saving a blank one clears it (a zero row, never a delete).
 */
@Composable
fun LogCycleSheet(
    days: List<CycleDay>,
    onDismiss: () -> Unit,
    viewModel: CycleViewModel = koinViewModel(),
) {
    val state = rememberLogCycleState()
    viewModel.collectSideEffect { effect ->
        when (effect) {
            CycleSideEffect.Saved -> onDismiss()
        }
    }
    LogCycleContent(days = days, state = state, onDismiss = onDismiss, onEvent = viewModel::handleEvent)
}

@Composable
private fun LogCycleContent(
    days: List<CycleDay>,
    state: LogCycleState,
    onDismiss: () -> Unit,
    onEvent: (CycleEvent) -> Unit,
) {
    val form = state.form
    // Built from the form so an imported day keeps its own answer: "Logged" is in the list only
    // when it is what this day already says, since nothing may set it by hand.
    val levels = buildList {
        add(0)
        if (form.flow == FlowLevel.Unstated.value) add(FlowLevel.Unstated.value)
        addAll(TAPPABLE_FLOW.map { it.value })
    }
    val labels = levels.map { value ->
        if (value == 0) stringResource(NOT_LOGGED) else stringResource(FlowLevel.entries.first { it.value == value }.label)
    }

    AppBottomSheet(onDismiss = onDismiss) {
        Text(
            text = stringResource(R.string.progress_cycle_sheet_title),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 12.dp),
        )
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SheetDatePicker(
                showingCalendar = state.showingCalendar,
                onShowCalendar = { state.showingCalendar = true },
                onBackToFields = { state.showingCalendar = false },
                selectedDate = form.dateEpochDay,
                // Period days only: a marked calendar showing symptom-only days would say the
                // period ran longer than it did, which is the figure every cycle length rests on.
                markedDates = days.filter { it.flow > 0 }.mapTo(mutableSetOf()) { it.dateEpochDay },
                onSelectDate = { date ->
                    state.form = seedCycleForm(days, date)
                    state.showingCalendar = false
                },
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(top = 12.dp)) {
                    Label(stringResource(R.string.progress_cycle_flow))
                    SegmentedToggle(
                        options = labels,
                        selectedIndex = levels.indexOf(form.flow).coerceAtLeast(0),
                        onSelect = { index -> state.form = form.copy(flow = levels[index]) },
                    )
                    Label(stringResource(R.string.progress_cycle_symptoms))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        SymptomChips.forEach { symptom ->
                            SymptomChip(
                                symptom = symptom,
                                selected = symptom in form.symptoms,
                                onClick = { state.form = form.toggle(symptom) },
                            )
                        }
                    }
                    if (form.flow == 0 && form.symptoms.isEmpty()) {
                        Text(
                            text = stringResource(R.string.progress_cycle_nothing_picked),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                        SecondaryButton(label = stringResource(R.string.progress_cancel), onClick = onDismiss, modifier = Modifier.weight(1f))
                        PrimaryButton(
                            label = stringResource(R.string.progress_save),
                            onClick = { onEvent(CycleEvent.OnSave(form)) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Label(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/** The house pill — a `Surface` and a rounded shape, like `FactChip`. Selected fills in
 * `secondaryContainer`, the same fill the nav pill and the mascot picker's cells use. */
@Composable
private fun SymptomChip(symptom: CycleSymptom, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(
                if (selected) {
                    MaterialTheme.colorScheme.secondaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHighest
                },
            )
            .clickable(role = Role.Checkbox, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Text(
            text = stringResource(symptom.label),
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) {
                MaterialTheme.colorScheme.onSecondaryContainer
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
    }
}

/** A sheet renders invisible in isolation, so the preview supplies its own scrim. */
@PreviewLightDark
@Composable
private fun LogCycleSheetPreview() {
    AppTheme {
        Box(modifier = Modifier.background(Color.Black.copy(alpha = 0.32f)).padding(top = 48.dp)) {
            LogCycleContent(
                days = emptyList(),
                state = LogCycleState(
                    CycleLogForm(
                        dateEpochDay = 20_000,
                        flow = FlowLevel.Medium.value,
                        symptoms = setOf(CycleSymptom.Cramps, CycleSymptom.Fatigue),
                    ),
                ),
                onDismiss = {},
                onEvent = {},
            )
        }
    }
}

/** A day imported from Health Connect: the intensity nobody reported keeps its own pill. */
@PreviewLightDark
@Composable
private fun LogCycleSheetImportedPreview() {
    AppTheme {
        Box(modifier = Modifier.background(Color.Black.copy(alpha = 0.32f)).padding(top = 48.dp)) {
            LogCycleContent(
                days = emptyList(),
                state = LogCycleState(CycleLogForm(dateEpochDay = 20_000, flow = FlowLevel.Unstated.value)),
                onDismiss = {},
                onEvent = {},
            )
        }
    }
}
