package ph.mart.healthapp.feature.food.ui.exercise

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import org.koin.androidx.compose.koinViewModel
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect
import ph.mart.healthapp.core.data.exercise.ExerciseEntry
import ph.mart.healthapp.core.data.exercise.ExerciseType
import ph.mart.healthapp.core.designsystem.component.AppBottomSheet
import ph.mart.healthapp.core.designsystem.component.PrimaryButton
import ph.mart.healthapp.core.designsystem.component.SecondaryButton
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.food.R
import ph.mart.healthapp.feature.food.ui.exercise.components.ExerciseFormFields

/** [dateEpochDay] is the day the entry lands on — the diary passes its selected day; the FAB's
 * quick-action sheet leaves it 0, which the repository stamps as today.
 *
 * [editing] is the logged activity being corrected, if any — the same sheet, seeded and saving
 * over that row instead of adding one.
 *
 * [onOpenStrength] leaves for the strength workout screen, carrying the day. It is offered only
 * once Strength is picked, and it is a door rather than an automatic redirect on purpose: the
 * plain duration-and-kcal path is what an imported watch session is, and it stays reachable. */
@Composable
fun LogExerciseSheet(
    onDismiss: () -> Unit,
    onOpenStrength: (Long) -> Unit,
    dateEpochDay: Long = 0,
    editing: ExerciseEntry? = null,
    viewModel: LogExerciseViewModel = koinViewModel(),
) {
    val uiState by viewModel.collectAsState()
    val state = rememberLogExerciseState(editing?.toLogExerciseForm() ?: LogExerciseForm())
    viewModel.collectSideEffect { effect ->
        when (effect) {
            LogExerciseSideEffect.Saved -> onDismiss()
        }
    }
    LogExerciseContent(
        uiState = uiState,
        state = state,
        dateEpochDay = dateEpochDay,
        editingId = editing?.id,
        onDismiss = onDismiss,
        onOpenStrength = { onOpenStrength(dateEpochDay) },
        onEvent = viewModel::handleEvent,
    )
}

@Composable
private fun LogExerciseContent(
    uiState: LogExerciseUiState,
    state: LogExerciseState,
    dateEpochDay: Long,
    editingId: Long?,
    onDismiss: () -> Unit,
    onOpenStrength: () -> Unit,
    onEvent: (LogExerciseEvent) -> Unit,
) {
    // Seeded from the form's own fields, so the estimate is right on the first frame too — the
    // form is the single source, and `withEstimate` is a no-op once the user takes the field over.
    val form = state.form.withEstimate(uiState.weightKg)

    AppBottomSheet(onDismiss = onDismiss) {
        Text(
            text = stringResource(if (editingId == null) R.string.food_exercise_log else R.string.food_exercise_edit),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 12.dp),
        )
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ExerciseFormFields(
                form = form,
                weightKg = uiState.weightKg,
                onFormChange = { state.form = it },
            )
            // Sets need a list and an editor, which don't fit above a keyboard — the argument the
            // recipe builder already made. So the sheet hands off rather than growing a sub-view.
            if (form.type == ExerciseType.Strength) {
                SecondaryButton(
                    label = stringResource(R.string.food_exercise_log_sets),
                    onClick = onOpenStrength,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                SecondaryButton(label = stringResource(R.string.food_cancel), onClick = onDismiss, modifier = Modifier.weight(1f))
                PrimaryButton(
                    label = stringResource(R.string.food_save),
                    onClick = { onEvent(LogExerciseEvent.OnSave(form, dateEpochDay, editingId)) },
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
            dateEpochDay = 0,
            editingId = null,
            onDismiss = {},
            onOpenStrength = {},
            onEvent = {},
        )
    }
}

/** Strength picked: the one type that offers a door out to a screen that can hold a set list. */
@PreviewLightDark
@Composable
private fun LogExerciseSheetStrengthPreview() {
    AppTheme {
        LogExerciseContent(
            uiState = LogExerciseUiState(weightKg = 74.0),
            state = LogExerciseState(form = LogExerciseForm(type = ExerciseType.Strength, minutes = 45)),
            dateEpochDay = 0,
            editingId = null,
            onDismiss = {},
            onOpenStrength = {},
            onEvent = {},
        )
    }
}

/** Correcting a logged activity: the title says so, and the burn is the figure that was logged
 * rather than a fresh estimate — [LogExerciseForm.burnedEdited] is what holds it there. */
@PreviewLightDark
@Composable
private fun LogExerciseSheetEditingPreview() {
    AppTheme {
        LogExerciseContent(
            uiState = LogExerciseUiState(weightKg = 74.0),
            state = LogExerciseState(
                form = ExerciseEntry(id = 1, type = ExerciseType.Run, name = "Riverside loop", minutes = 30, burnedKcal = 363)
                    .toLogExerciseForm(),
            ),
            dateEpochDay = 0,
            editingId = 1,
            onDismiss = {},
            onOpenStrength = {},
            onEvent = {},
        )
    }
}
