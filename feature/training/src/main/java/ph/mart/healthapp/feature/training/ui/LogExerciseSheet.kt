package ph.mart.healthapp.feature.training.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import ph.mart.healthapp.feature.training.R
import ph.mart.healthapp.feature.training.ui.components.ExerciseFormFields

/** [dateEpochDay] is the day the entry lands on — the diary passes its selected day; the FAB's
 * quick-action sheet leaves it 0, which the repository stamps as today.
 *
 * [editingId] names the logged activity being corrected, `0` for a new one — the same sheet,
 * seeded and saving over that row instead of adding one. An **id**, not the row: `AppScaffold` is
 * the one host of this sheet now that `:feature:food` cannot import it, and its sheet state is
 * `rememberSaveable`, which an `ExerciseEntry` is not.
 *
 * [onOpenStrength] leaves for the strength workout screen, carrying the day. It is offered only
 * once Strength is picked, and it is a door rather than an automatic redirect on purpose: the
 * plain duration-and-kcal path is what an imported watch session is, and it stays reachable. */
@Composable
fun LogExerciseSheet(
    onDismiss: () -> Unit,
    onOpenStrength: (Long) -> Unit,
    dateEpochDay: Long = 0,
    editingId: Long = 0,
    viewModel: LogExerciseViewModel = koinViewModel(),
) {
    val uiState by viewModel.collectAsState()
    LaunchedEffect(editingId) {
        if (editingId > 0) viewModel.handleEvent(LogExerciseEvent.OnLoadEditing(editingId))
    }
    viewModel.collectSideEffect { effect ->
        when (effect) {
            LogExerciseSideEffect.Saved -> onDismiss()
        }
    }
    // The row has to be *this* row: the ViewModel outlives the sheet, so a previous edit's entry
    // is still on the state when the FAB opens a blank one. Matching the id is both the hold-back
    // (nothing composes until the read lands, or the saveable form would re-key under the user)
    // and the staleness guard, which is why there is no second `editingLoaded` flag.
    val editing = uiState.editing?.takeIf { it.id == editingId }
    if (editingId > 0 && editing == null) return
    val state = rememberLogExerciseState(editing?.toLogExerciseForm() ?: LogExerciseForm())
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
            text = stringResource(if (editingId == null) R.string.training_exercise_log else R.string.training_exercise_edit),
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
                    label = stringResource(R.string.training_exercise_log_sets),
                    onClick = onOpenStrength,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                SecondaryButton(label = stringResource(R.string.training_cancel), onClick = onDismiss, modifier = Modifier.weight(1f))
                PrimaryButton(
                    label = stringResource(R.string.training_save),
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
