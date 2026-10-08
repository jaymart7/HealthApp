package ph.mart.healthapp.feature.profile.ui.routine.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import ph.mart.healthapp.core.data.exercise.Routine
import ph.mart.healthapp.core.data.exercise.RoutineLift
import ph.mart.healthapp.core.designsystem.component.AIChip
import ph.mart.healthapp.core.designsystem.component.AIChipVariant
import ph.mart.healthapp.core.designsystem.component.AppBottomSheet
import ph.mart.healthapp.core.designsystem.component.AppTextField
import ph.mart.healthapp.core.designsystem.component.PrimaryButton
import ph.mart.healthapp.core.designsystem.component.SecondaryButton
import ph.mart.healthapp.core.designsystem.component.TextButton
import ph.mart.healthapp.core.designsystem.component.rememberSpeechAvailable
import ph.mart.healthapp.core.designsystem.component.speechIntent
import ph.mart.healthapp.core.designsystem.component.spokenPhrase
import ph.mart.healthapp.core.designsystem.icon.AppIcons
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.profile.R
import ph.mart.healthapp.feature.profile.ui.routine.NewRoutineState

/**
 * New routine: say what you want, see what Gemini designed, save it. Two steps in one sheet —
 * **describe** (typed or dictated), then a **preview** of the designed routine where everything is
 * still the user's: the name, each lift's name, its sets and reps, removing one or adding one.
 * "Change" goes back to the request instead, for when the whole design missed.
 *
 * "Build from a workout instead" is the manual path and the whole offline degrade — the strength
 * screen's set list in routine mode, whose pinned "Save routine" names it and comes back here.
 *
 * Back steps one level, the predictive-back rule: a design in flight is cancelled, a preview
 * returns to the request with its text kept, and only the describe step lets the sheet's own
 * back dismiss it. One handler, registered inside the sheet so it wins while it applies.
 */
@Composable
internal fun NewRoutineSheet(
    state: NewRoutineState,
    designing: Boolean,
    onDesign: () -> Unit,
    onCancelDesign: () -> Unit,
    onSave: (Routine) -> Unit,
    onBuildFromWorkout: () -> Unit,
    onDismiss: () -> Unit,
) {
    AppBottomSheet(title = stringResource(R.string.profile_routines_new), onDismiss = onDismiss) {
        val draft = state.draft
        if (designing || draft != null) {
            val navigationState = rememberNavigationEventState(currentInfo = NavigationEventInfo.None)
            NavigationBackHandler(
                state = navigationState,
                onBackCompleted = { if (designing) onCancelDesign() else state.draft = null },
            )
        }
        if (draft == null) {
            DescribeStep(
                state = state,
                designing = designing,
                onDesign = onDesign,
                onCancelDesign = onCancelDesign,
                onBuildFromWorkout = onBuildFromWorkout,
            )
        } else {
            PreviewStep(
                draft = draft,
                onDraftChange = { state.draft = it },
                onChange = { state.draft = null },
                onSave = { onSave(draft.trimmed()) },
            )
        }
    }
}

@Composable
private fun DescribeStep(
    state: NewRoutineState,
    designing: Boolean,
    onDesign: () -> Unit,
    onCancelDesign: () -> Unit,
    onBuildFromWorkout: () -> Unit,
) {
    val prompt = stringResource(R.string.profile_routine_design_prompt)
    val speechAvailable = rememberSpeechAvailable()
    val speech = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        // Replaces rather than appends — `DescribeExerciseField`'s rule: a request is said whole.
        spokenPhrase(result.data)?.let {
            state.request = it
            state.message = null
        }
    }
    val speakLabel = stringResource(R.string.profile_routine_design_speak)

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        AppTextField(
            label = prompt,
            value = state.request,
            onValueChange = {
                state.request = it
                state.message = null
            },
            placeholder = stringResource(R.string.profile_routine_design_placeholder),
            maxLines = 3,
            // Passed even with no recognizer, so the field keeps one width on every device —
            // `DescribeExerciseField`'s reason.
            trailing = {
                if (speechAvailable) {
                    IconButton(onClick = { speech.launch(speechIntent(prompt)) }) {
                        Icon(
                            imageVector = AppIcons.Mic,
                            contentDescription = speakLabel,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            },
        )
        state.message?.let {
            Text(
                text = stringResource(it),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (designing) {
            // A cancel rather than a bare spinner, `DescribeExerciseField`'s reason: a model can
            // hang, and dismissing the sheet would cost the user the sentence they typed.
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                SecondaryButton(label = stringResource(R.string.profile_routine_design_cancel), onClick = onCancelDesign)
            }
        } else {
            PrimaryButton(
                label = stringResource(R.string.profile_routine_design_submit),
                onClick = onDesign,
                enabled = state.request.isNotBlank(),
                icon = AppIcons.AiSparkle,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        TextButton(
            label = stringResource(R.string.profile_routine_design_manual),
            onClick = onBuildFromWorkout,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun PreviewStep(
    draft: Routine,
    onDraftChange: (Routine) -> Unit,
    onChange: () -> Unit,
    onSave: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        AIChip(label = stringResource(R.string.profile_routine_design_chip), variant = AIChipVariant.Default)
        RoutineFields(draft = draft, onDraftChange = onDraftChange)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            SecondaryButton(
                label = stringResource(R.string.profile_routine_design_change),
                onClick = onChange,
                modifier = Modifier.weight(1f),
            )
            PrimaryButton(
                label = stringResource(R.string.profile_routine_design_save),
                onClick = onSave,
                enabled = draft.isSaveable(),
                modifier = Modifier.weight(1f),
            )
        }
    }
}

private val previewDraft = Routine(
    id = 0,
    name = "Push day",
    lifts = listOf(
        RoutineLift("Dumbbell bench press", sets = 3, reps = 10),
        RoutineLift("Seated dumbbell press", sets = 3, reps = 10),
        RoutineLift("Lateral raise", sets = 3, reps = 12),
        RoutineLift("Overhead triceps extension", sets = 3, reps = 12),
    ),
)

@PreviewLightDark
@Composable
private fun NewRoutineSheetDescribePreview() {
    AppTheme {
        NewRoutineSheet(
            state = NewRoutineState(open = true, request = "Push day, 45 minutes, dumbbells only"),
            designing = false,
            onDesign = {},
            onCancelDesign = {},
            onSave = {},
            onBuildFromWorkout = {},
            onDismiss = {},
        )
    }
}

/** In flight: the cancel is the control, and the spinner is only the reason it is there. */
@PreviewLightDark
@Composable
private fun NewRoutineSheetDesigningPreview() {
    AppTheme {
        NewRoutineSheet(
            state = NewRoutineState(open = true, request = "Push day, 45 minutes, dumbbells only"),
            designing = true,
            onDesign = {},
            onCancelDesign = {},
            onSave = {},
            onBuildFromWorkout = {},
            onDismiss = {},
        )
    }
}

/** A request the model couldn't turn into lifts keeps the sentence, so it can be corrected. */
@PreviewLightDark
@Composable
private fun NewRoutineSheetNothingPreview() {
    AppTheme {
        NewRoutineSheet(
            state = NewRoutineState(
                open = true,
                request = "Something relaxing",
                message = R.string.profile_routine_design_none,
            ),
            designing = false,
            onDesign = {},
            onCancelDesign = {},
            onSave = {},
            onBuildFromWorkout = {},
            onDismiss = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun NewRoutineSheetPreviewStepPreview() {
    AppTheme {
        NewRoutineSheet(
            state = NewRoutineState(open = true, request = "Push day, 45 minutes, dumbbells only", draft = previewDraft),
            designing = false,
            onDesign = {},
            onCancelDesign = {},
            onSave = {},
            onBuildFromWorkout = {},
            onDismiss = {},
        )
    }
}
