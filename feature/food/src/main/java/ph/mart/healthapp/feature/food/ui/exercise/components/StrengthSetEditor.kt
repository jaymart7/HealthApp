package ph.mart.healthapp.feature.food.ui.exercise.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.data.exercise.StrengthSet
import ph.mart.healthapp.core.data.exercise.formatLoad
import ph.mart.healthapp.core.data.profile.UnitSystem
import ph.mart.healthapp.core.data.profile.displayUnitToKg
import ph.mart.healthapp.core.data.profile.kgToDisplayUnit
import ph.mart.healthapp.core.data.profile.weightUnitLabel
import ph.mart.healthapp.core.designsystem.component.AppTextField
import ph.mart.healthapp.core.designsystem.component.NumericStepperField
import ph.mart.healthapp.core.designsystem.component.PrimaryButton
import ph.mart.healthapp.core.designsystem.theme.AppTheme

/** The plate a lifter actually adds — 2.5 kg a side is one increment, and 5 lb is its imperial
 * twin. Stepping by 1 would make a 20 kg jump twenty taps, which is what the typable field and
 * this step exist together to avoid. */
private fun loadStep(unit: UnitSystem): Double = if (unit == UnitSystem.Imperial) 5.0 else 2.5

/**
 * Names one lift and adds one set of it. The name is free text with chips for what was lifted
 * recently — a fixed exercise list would be wrong for everybody within a week, and the chips cost
 * no schema because they are derived from the workouts already logged.
 *
 * The load is entered and displayed in the user's own unit and stored in kilograms, the rule every
 * weight in this app follows. A load of zero is bodyweight and is a valid set, so [canAdd] turns
 * on reps and a name, never on the weight.
 */
@Composable
internal fun StrengthSetEditor(
    draft: StrengthSet,
    unit: UnitSystem,
    recentLifts: List<String>,
    onDraftChange: (StrengthSet) -> Unit,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val step = loadStep(unit)
    val displayLoad = draft.weightKg.kgToDisplayUnit(unit)

    Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = modifier) {
        AppTextField(
            label = "Exercise",
            value = draft.exerciseName,
            placeholder = "Bench press",
            onValueChange = { onDraftChange(draft.copy(exerciseName = it)) },
        )
        if (recentLifts.isNotEmpty()) {
            LiftNameChipRow(
                names = recentLifts,
                selected = draft.exerciseName,
                onSelect = { onDraftChange(draft.copy(exerciseName = it)) },
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            NumericStepperField(
                label = "Reps",
                value = "${draft.reps}",
                unitSuffix = "",
                onValueChange = { onDraftChange(draft.copy(reps = it.toIntOrNull() ?: 0)) },
                onIncrement = { onDraftChange(draft.copy(reps = draft.reps + 1)) },
                onDecrement = { onDraftChange(draft.copy(reps = (draft.reps - 1).coerceAtLeast(0))) },
                modifier = Modifier.weight(1f),
            )
            NumericStepperField(
                // Zero is bodyweight, not an unfilled field, so the label says so rather than
                // leaving a lifter wondering whether the set will count.
                label = if (draft.weightKg <= 0.0) "Load · bodyweight" else "Load",
                value = formatLoad(displayLoad),
                unitSuffix = unit.weightUnitLabel(),
                onValueChange = {
                    onDraftChange(draft.copy(weightKg = (it.toDoubleOrNull() ?: 0.0).displayUnitToKg(unit)))
                },
                onIncrement = { onDraftChange(draft.copy(weightKg = (displayLoad + step).displayUnitToKg(unit))) },
                onDecrement = {
                    onDraftChange(
                        draft.copy(weightKg = (displayLoad - step).coerceAtLeast(0.0).displayUnitToKg(unit)),
                    )
                },
                modifier = Modifier.weight(1f),
            )
        }
        // The caller keeps the draft after a commit, so pressing this again logs the same set —
        // which is the commonest gesture in a strength log and needs no button of its own.
        PrimaryButton(
            label = "Add set",
            onClick = onAdd,
            enabled = draft.canAdd(),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** A named lift with at least one rep. The load is deliberately not checked — zero is bodyweight. */
internal fun StrengthSet.canAdd(): Boolean = exerciseName.isNotBlank() && reps > 0

/** [ExerciseTypeChipRow]'s pill, over names rather than a fixed enum — they size to their labels
 * and scroll, because a lift name is as long as it is. */
@Composable
private fun LiftNameChipRow(names: List<String>, selected: String, onSelect: (String) -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
    ) {
        names.forEach { name ->
            val isSelected = name.equals(selected, ignoreCase = true)
            Surface(
                onClick = { onSelect(name) },
                shape = RoundedCornerShape(999.dp),
                color = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface,
                contentColor = if (isSelected) {
                    MaterialTheme.colorScheme.onSecondaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                border = BorderStroke(
                    width = if (isSelected) 1.5.dp else 1.dp,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                ),
                modifier = Modifier.height(36.dp),
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 16.dp)) {
                    Text(text = name, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun StrengthSetEditorPreview() {
    AppTheme {
        Surface {
            StrengthSetEditor(
                draft = StrengthSet("Bench press", reps = 8, weightKg = 60.0),
                unit = UnitSystem.Metric,
                recentLifts = listOf("Bench press", "Squat", "Deadlift"),
                onDraftChange = {},
                onAdd = {},
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

/** A blank draft: "Add set" is off until a lift is named, and the load reads as bodyweight. */
@PreviewLightDark
@Composable
private fun StrengthSetEditorEmptyPreview() {
    AppTheme {
        Surface {
            StrengthSetEditor(
                draft = StrengthSet("", reps = 0, weightKg = 0.0),
                unit = UnitSystem.Metric,
                recentLifts = emptyList(),
                onDraftChange = {},
                onAdd = {},
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}
