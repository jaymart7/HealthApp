package ph.mart.healthapp.feature.food.ui.quicklog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import org.koin.androidx.compose.koinViewModel
import org.orbitmvi.orbit.compose.collectSideEffect
import ph.mart.healthapp.core.data.exercise.ExerciseEntry
import ph.mart.healthapp.core.data.exercise.ExerciseType
import ph.mart.healthapp.core.data.food.QuickLogTurn
import ph.mart.healthapp.core.data.food.RecognitionConfidence
import ph.mart.healthapp.core.data.food.RecognizedFood
import ph.mart.healthapp.core.designsystem.component.AppBottomSheet
import ph.mart.healthapp.core.designsystem.component.PrimaryButton
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.food.R
import ph.mart.healthapp.feature.food.ui.quicklog.components.QuickLogConversation
import ph.mart.healthapp.feature.food.ui.quicklog.components.QuickLogInputBar
import ph.mart.healthapp.feature.food.ui.shared.toAddEntryForm
import ph.mart.healthapp.feature.food.ui.shared.toFoodEntry

/**
 * The FAB's sheet: one field that reads what the user ate or did, asks back when the sentence left
 * out the thing the estimate turns on, and logs both kinds from one confirmation. The camera and
 * the barcode sit under the field as the two ways in that are not sentences.
 *
 * A sheet rather than a route for the reason every FAB sheet is one — back closes it onto the tab
 * it was opened over — and hosted by `AppScaffold`, so everything leaving it is a callback:
 * [onCapturePhoto] and [onScanBarcode] push their routes (day 0, the FAB is today-only), and
 * [onSaved] is the same earned-kcal snackbar the log-exercise sheet reports to.
 *
 * Back steps through the conversation before it leaves: a call in flight is cancelled with its
 * words handed back, a question or a review starts over from the first sentence, and only a blank
 * start lets the sheet's own back close it. The handler is mounted inside the sheet's window, the
 * way `LogExerciseSheet`'s describe panel mounts its own, or `ModalBottomSheet` takes the gesture
 * first.
 */
@Composable
fun QuickLogSheet(
    onDismiss: () -> Unit,
    onCapturePhoto: () -> Unit,
    onScanBarcode: () -> Unit,
    onSaved: (creditedKcal: Int) -> Unit,
    viewModel: QuickLogViewModel = koinViewModel(),
) {
    val state = rememberQuickLogState()

    viewModel.collectSideEffect { effect ->
        when (effect) {
            is QuickLogSideEffect.Asked -> state.applyQuestion(effect.question)
            is QuickLogSideEffect.Parsed -> state.applyParsed(effect.foods, effect.exercises)
            QuickLogSideEffect.NothingFound -> state.restoreLast(R.string.food_quick_nothing)
            QuickLogSideEffect.Failed -> state.restoreLast(R.string.food_quick_failed)
            // Reported before the dismiss — `LogExerciseSheet`'s order, for its reason.
            is QuickLogSideEffect.Logged -> {
                onSaved(effect.creditedKcal)
                onDismiss()
            }
        }
    }

    // The ViewModel outlives this sheet, so an answer still in flight would otherwise land on the
    // next one the FAB opens.
    val cancel = { viewModel.handleEvent(QuickLogEvent.OnCancel) }

    QuickLogContent(
        state = state,
        onDismiss = {
            cancel()
            onDismiss()
        },
        onSend = {
            // Checked at the tap, before the words leave the field — `startParse`'s rule: "you're
            // offline" and "that didn't work" are different things to say.
            if (viewModel.isOnline()) {
                viewModel.handleEvent(QuickLogEvent.OnSend(state.send()))
            } else {
                state.message = R.string.food_quick_offline
            }
        },
        onCancel = {
            cancel()
            state.restoreLast(null)
        },
        onLog = {
            viewModel.handleEvent(
                QuickLogEvent.OnLog(
                    foods = state.foods.map { it.toAddEntryForm(state.mealType).toFoodEntry() },
                    exercises = state.exercises,
                ),
            )
        },
        onCapturePhoto = {
            cancel()
            onCapturePhoto()
        },
        onScanBarcode = {
            cancel()
            onScanBarcode()
        },
    )
}

@Composable
private fun QuickLogContent(
    state: QuickLogState,
    onDismiss: () -> Unit,
    onSend: () -> Unit,
    onCancel: () -> Unit,
    onLog: () -> Unit,
    onCapturePhoto: () -> Unit,
    onScanBarcode: () -> Unit,
) {
    val placeholder = stringResource(
        when {
            state.question != null -> R.string.food_quick_answer_placeholder
            state.hasResult -> R.string.food_quick_change_placeholder
            else -> R.string.food_quick_placeholder
        },
    )

    AppBottomSheet(
        title = stringResource(R.string.food_quick_prompt),
        onDismiss = onDismiss,
        // The field is the bar, so it sits on the keyboard however long the review above it runs.
        bottomBar = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
            ) {
                if (state.phase == QuickLogPhase.Review) {
                    PrimaryButton(
                        label = stringResource(R.string.food_quick_log),
                        onClick = onLog,
                        enabled = state.hasResult,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                QuickLogInputBar(
                    text = state.text,
                    placeholder = placeholder,
                    thinking = state.phase == QuickLogPhase.Thinking,
                    canSend = state.canSend,
                    showShortcuts = state.turns.isEmpty(),
                    onTextChange = { state.text = it },
                    onSend = onSend,
                    onCancel = onCancel,
                    onCapturePhoto = onCapturePhoto,
                    onScanBarcode = onScanBarcode,
                )
            }
        },
    ) {
        // Down when a result lands, so the rows and the Log button are what is on screen; tapping
        // the field for a correction brings it straight back. Read in here, not in [QuickLogSheet]:
        // the sheet is its own window, and the keyboard belongs to the window that has the field.
        val keyboard = LocalSoftwareKeyboardController.current
        LaunchedEffect(state.phase) { if (state.phase == QuickLogPhase.Review) keyboard?.hide() }
        if (state.canStepBack) {
            val navigationState = rememberNavigationEventState(currentInfo = NavigationEventInfo.None)
            NavigationBackHandler(
                state = navigationState,
                onBackCompleted = {
                    if (state.phase == QuickLogPhase.Thinking) onCancel() else state.startOver()
                },
            )
        }
        QuickLogConversation(
            lastSaid = state.lastSaid,
            question = state.question,
            foods = state.foods,
            exercises = state.exercises,
            mealType = state.mealType,
            message = state.message?.let { stringResource(it) },
            onRemoveFood = state::removeFood,
            onRemoveExercise = state::removeExercise,
            onMealTypeSelect = { state.mealType = it },
        )
    }
}

@PreviewLightDark
@Composable
private fun QuickLogSheetPreview() {
    AppTheme {
        QuickLogContent(
            state = QuickLogState(),
            onDismiss = {},
            onSend = {},
            onCancel = {},
            onLog = {},
            onCapturePhoto = {},
            onScanBarcode = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun QuickLogSheetQuestionPreview() {
    AppTheme {
        QuickLogContent(
            state = QuickLogState().apply {
                turns = listOf(
                    QuickLogTurn(fromUser = true, text = "went for a run"),
                    QuickLogTurn(fromUser = false, text = "How long did it last?"),
                )
            },
            onDismiss = {},
            onSend = {},
            onCancel = {},
            onLog = {},
            onCapturePhoto = {},
            onScanBarcode = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun QuickLogSheetReviewPreview() {
    AppTheme {
        QuickLogContent(
            state = QuickLogState().apply {
                turns = listOf(QuickLogTurn(fromUser = true, text = "two eggs and toast, then a 30 min run"))
                applyParsed(
                    foods = listOf(
                        RecognizedFood("Scrambled eggs", 2.0, "egg", 180, 12, 2, 14, confidence = RecognitionConfidence.High),
                        RecognizedFood("Wholemeal toast", 1.0, "slice", 80, 4, 14, 1, confidence = RecognitionConfidence.High),
                    ),
                    exercises = listOf(ExerciseEntry(type = ExerciseType.Run, minutes = 30, burnedKcal = 343)),
                )
            },
            onDismiss = {},
            onSend = {},
            onCancel = {},
            onLog = {},
            onCapturePhoto = {},
            onScanBarcode = {},
        )
    }
}
