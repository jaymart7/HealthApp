package ph.mart.healthapp.feature.food.ui

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
import ph.mart.healthapp.core.data.exercise.ExerciseType
import ph.mart.healthapp.core.designsystem.component.AppBottomSheet
import ph.mart.healthapp.core.designsystem.component.AppTextField
import ph.mart.healthapp.core.designsystem.component.NumericStepperField
import ph.mart.healthapp.core.designsystem.component.PrimaryButton
import ph.mart.healthapp.core.designsystem.component.SecondaryButton
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.food.ui.components.ExerciseTypeChipRow

@Composable
fun LogExerciseSheet(onDismiss: () -> Unit, viewModel: LogExerciseViewModel = koinViewModel()) {
    val uiState by viewModel.collectAsState()
    val state = rememberLogExerciseState()
    viewModel.collectSideEffect { effect ->
        when (effect) {
            LogExerciseSideEffect.Saved -> onDismiss()
        }
    }
    LogExerciseContent(uiState = uiState, state = state, onDismiss = onDismiss, onEvent = viewModel::handleEvent)
}

@Composable
private fun LogExerciseContent(
    uiState: LogExerciseUiState,
    state: LogExerciseState,
    onDismiss: () -> Unit,
    onEvent: (LogExerciseEvent) -> Unit,
) {
    // Seeded from the form's own fields, so the estimate is right on the first frame too — the
    // form is the single source, and `withEstimate` is a no-op once the user takes the field over.
    val form = state.form.withEstimate(uiState.weightKg)

    AppBottomSheet(onDismiss = onDismiss) {
        Text(
            text = "Log exercise",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 12.dp),
        )
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ExerciseTypeChipRow(
                selected = form.type,
                onSelect = { state.form = form.copy(type = it).withEstimate(uiState.weightKg) },
            )
            AppTextField(
                label = "Note (optional)",
                value = form.name,
                onValueChange = { state.form = form.copy(name = it) },
            )
            NumericStepperField(
                label = "Duration",
                value = "${form.minutes}",
                unitSuffix = "min",
                onIncrement = {
                    state.form = form.copy(minutes = form.minutes + MINUTES_STEP).withEstimate(uiState.weightKg)
                },
                onDecrement = {
                    state.form = form.copy(minutes = (form.minutes - MINUTES_STEP).coerceAtLeast(MINUTES_STEP))
                        .withEstimate(uiState.weightKg)
                },
            )
            NumericStepperField(
                label = "Burned · estimated from ${form.type.label.lowercase()} at your latest weight",
                value = "${form.burnedKcal}",
                unitSuffix = "kcal",
                onIncrement = { state.form = form.copy(burnedKcal = form.burnedKcal + KCAL_STEP, burnedEdited = true) },
                onDecrement = {
                    state.form = form.copy(
                        burnedKcal = (form.burnedKcal - KCAL_STEP).coerceAtLeast(0),
                        burnedEdited = true,
                    )
                },
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                SecondaryButton(label = "Cancel", onClick = onDismiss, modifier = Modifier.weight(1f))
                PrimaryButton(
                    label = "Save",
                    onClick = { onEvent(LogExerciseEvent.OnSave(form)) },
                    enabled = form.isValid(),
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun LogExerciseSheetPreview() {
    AppTheme {
        LogExerciseContent(
            uiState = LogExerciseUiState(weightKg = 74.0),
            state = LogExerciseState(form = LogExerciseForm(type = ExerciseType.Run, minutes = 30)),
            onDismiss = {},
            onEvent = {},
        )
    }
}
