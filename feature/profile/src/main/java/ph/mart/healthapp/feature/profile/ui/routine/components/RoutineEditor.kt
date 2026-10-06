package ph.mart.healthapp.feature.profile.ui.routine.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.data.exercise.MAX_ROUTINE_LIFTS
import ph.mart.healthapp.core.data.exercise.MAX_ROUTINE_REPS
import ph.mart.healthapp.core.data.exercise.MAX_ROUTINE_SETS
import ph.mart.healthapp.core.data.exercise.Routine
import ph.mart.healthapp.core.data.exercise.RoutineLift
import ph.mart.healthapp.core.designsystem.component.AppTextField
import ph.mart.healthapp.core.designsystem.component.StepperButton
import ph.mart.healthapp.core.designsystem.component.TextButton
import ph.mart.healthapp.core.designsystem.icon.AppIcons
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.core.designsystem.theme.tabularNums
import ph.mart.healthapp.core.designsystem.R as DesignSystemR
import ph.mart.healthapp.feature.profile.R

/**
 * A routine as fields: its name, then every lift with its sets and reps, removable, and "Add lift"
 * under them. What [NewRoutineSheet]'s preview and [EditRoutineSheet] both draw, so a designed
 * routine and an edited one are held to the same bounds by the same controls.
 */
@Composable
internal fun RoutineFields(draft: Routine, onDraftChange: (Routine) -> Unit) {
    fun setLift(index: Int, lift: RoutineLift) =
        onDraftChange(draft.copy(lifts = draft.lifts.mapIndexed { i, it -> if (i == index) lift else it }))

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        AppTextField(
            label = stringResource(R.string.profile_routine_design_name),
            value = draft.name,
            onValueChange = { onDraftChange(draft.copy(name = it)) },
        )
        draft.lifts.forEachIndexed { index, lift ->
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            LiftEditor(
                lift = lift,
                onChange = { setLift(index, it) },
                onRemove = { onDraftChange(draft.copy(lifts = draft.lifts.filterIndexed { i, _ -> i != index })) },
            )
        }
        TextButton(
            label = stringResource(R.string.profile_routine_design_add),
            onClick = { onDraftChange(draft.copy(lifts = draft.lifts + RoutineLift("", sets = 3, reps = 10))) },
            enabled = draft.lifts.size < MAX_ROUTINE_LIFTS,
            icon = AppIcons.Add,
        )
    }
}

/**
 * One lift of the draft: its name, then sets and reps as two stepper halves sharing the row. Each
 * half lets its figure take the slack, so a large font scale squeezes the number rather than
 * pushing a button off the sheet. Clamped to the coach's own bounds, so an edited routine is held
 * to what a designed one is.
 */
@Composable
private fun LiftEditor(lift: RoutineLift, onChange: (RoutineLift) -> Unit, onRemove: () -> Unit) {
    // A just-added row has no name yet, and "Remove " tells a screen reader nothing.
    val liftLabel = lift.exerciseName.ifBlank { stringResource(R.string.profile_routine_design_lift) }
    val removeLabel = stringResource(R.string.profile_routine_design_remove, liftLabel)
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AppTextField(
                value = lift.exerciseName,
                onValueChange = { onChange(lift.copy(exerciseName = it)) },
                placeholder = stringResource(R.string.profile_routine_design_lift),
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onRemove) {
                Icon(
                    imageVector = AppIcons.Delete,
                    contentDescription = removeLabel,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            CountStepper(
                value = lift.sets,
                unit = pluralStringResource(R.plurals.profile_routine_sets_unit, lift.sets),
                label = stringResource(R.string.profile_routine_design_sets_of, liftLabel),
                range = 1..MAX_ROUTINE_SETS,
                onValueChange = { onChange(lift.copy(sets = it)) },
                modifier = Modifier.weight(1f),
            )
            CountStepper(
                value = lift.reps,
                unit = pluralStringResource(R.plurals.profile_routine_reps_unit, lift.reps),
                label = stringResource(R.string.profile_routine_design_reps_of, liftLabel),
                range = 1..MAX_ROUTINE_REPS,
                onValueChange = { onChange(lift.copy(reps = it)) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/** − figure + — `StepperRow`'s buttons with the figure between them, and its unit under it. */
@Composable
private fun CountStepper(
    value: Int,
    unit: String,
    label: String,
    range: IntRange,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
        StepperButton(
            symbol = "−",
            label = stringResource(DesignSystemR.string.ds_decrease, label),
            onClick = { onValueChange(value - 1) },
            enabled = value > range.first,
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
            Text(
                text = value.toString(),
                style = MaterialTheme.typography.titleMedium.tabularNums,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = unit,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        StepperButton(
            symbol = "+",
            label = stringResource(DesignSystemR.string.ds_increase, label),
            onClick = { onValueChange(value + 1) },
            enabled = value < range.last,
        )
    }
}

/** A name, at least one lift, and every lift named — a blank row the user added and never filled
 * in would otherwise save as a nameless lift. */
internal fun Routine.isSaveable(): Boolean =
    name.isNotBlank() && lifts.isNotEmpty() && lifts.all { it.exerciseName.isNotBlank() }

internal fun Routine.trimmed(): Routine =
    copy(name = name.trim(), lifts = lifts.map { it.copy(exerciseName = it.exerciseName.trim()) })

@PreviewLightDark
@Composable
private fun RoutineFieldsPreview() {
    AppTheme {
        Surface(color = MaterialTheme.colorScheme.surfaceContainerLow) {
            Column(modifier = Modifier.padding(16.dp)) {
                RoutineFields(
                    draft = Routine(
                        id = 0,
                        name = "Push day",
                        lifts = listOf(
                            RoutineLift("Bench press", sets = 3, reps = 8),
                            RoutineLift("", sets = 3, reps = 10),
                        ),
                    ),
                    onDraftChange = {},
                )
            }
        }
    }
}
