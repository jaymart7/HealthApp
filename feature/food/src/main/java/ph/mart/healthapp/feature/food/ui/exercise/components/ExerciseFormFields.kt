package ph.mart.healthapp.feature.food.ui.exercise.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.data.exercise.ExerciseType
import ph.mart.healthapp.core.designsystem.component.AppTextField
import ph.mart.healthapp.core.designsystem.component.NumericStepperField
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.food.ui.exercise.KCAL_STEP
import ph.mart.healthapp.feature.food.ui.exercise.LogExerciseForm
import ph.mart.healthapp.feature.food.ui.exercise.MINUTES_STEP
import ph.mart.healthapp.feature.food.ui.exercise.withEstimate

/**
 * The note/duration/burn trio every activity carries, whichever surface is logging it — the sheet
 * and the strength workout screen both render it, so it lives here rather than being copied into
 * the second one.
 *
 * [showTypeChips] is false on the strength screen: the type is Strength by definition there, and a
 * chip row that could switch it would leave a set list attached to a swim.
 */
@Composable
internal fun ExerciseFormFields(
    form: LogExerciseForm,
    weightKg: Double,
    onFormChange: (LogExerciseForm) -> Unit,
    modifier: Modifier = Modifier,
    showTypeChips: Boolean = true,
) {
    // Re-estimating on every change is what keeps the burn honest until the user takes the field
    // over; `withEstimate` is a no-op after that, so no caller has to remember the rule.
    fun update(next: LogExerciseForm) = onFormChange(next.withEstimate(weightKg))

    Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = modifier) {
        if (showTypeChips) {
            ExerciseTypeChipRow(
                selected = form.type,
                onSelect = { update(form.copy(type = it)) },
            )
        }
        AppTextField(
            label = "Note (optional)",
            value = form.name,
            onValueChange = { onFormChange(form.copy(name = it)) },
        )
        NumericStepperField(
            label = "Duration",
            value = "${form.minutes}",
            unitSuffix = "min",
            onValueChange = { update(form.copy(minutes = it.toIntOrNull() ?: 0)) },
            onIncrement = { update(form.copy(minutes = form.minutes + MINUTES_STEP)) },
            onDecrement = {
                update(form.copy(minutes = (form.minutes - MINUTES_STEP).coerceAtLeast(MINUTES_STEP)))
            },
        )
        NumericStepperField(
            // Estimation stops the moment the field is edited by hand, so the label has to
            // stop saying "estimated" at the same moment — otherwise it describes a
            // calculation that is no longer running.
            label = if (form.burnedEdited) {
                "Burned · your figure"
            } else {
                "Burned · estimated from ${stringResource(form.type.label).lowercase()} at your latest weight"
            },
            value = "${form.burnedKcal}",
            unitSuffix = "kcal",
            onValueChange = { onFormChange(form.copy(burnedKcal = it.toIntOrNull() ?: 0, burnedEdited = true)) },
            onIncrement = { onFormChange(form.copy(burnedKcal = form.burnedKcal + KCAL_STEP, burnedEdited = true)) },
            onDecrement = {
                onFormChange(
                    form.copy(
                        burnedKcal = (form.burnedKcal - KCAL_STEP).coerceAtLeast(0),
                        burnedEdited = true,
                    ),
                )
            },
        )
    }
}

@PreviewLightDark
@Composable
private fun ExerciseFormFieldsPreview() {
    AppTheme {
        Surface {
            ExerciseFormFields(
                form = LogExerciseForm(type = ExerciseType.Run, minutes = 30, burnedKcal = 363),
                weightKg = 74.0,
                onFormChange = {},
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

/** The strength screen's variant: no type chips, because the type is already decided. */
@PreviewLightDark
@Composable
private fun ExerciseFormFieldsNoChipsPreview() {
    AppTheme {
        Surface {
            ExerciseFormFields(
                form = LogExerciseForm(type = ExerciseType.Strength, minutes = 45, burnedKcal = 260),
                weightKg = 74.0,
                onFormChange = {},
                showTypeChips = false,
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}
