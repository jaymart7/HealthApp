package ph.mart.healthapp.feature.food.ui.quicklog

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import java.io.File
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect
import ph.mart.healthapp.core.camera.decodeRotatedBitmap
import ph.mart.healthapp.core.data.exercise.ExerciseEntry
import ph.mart.healthapp.core.data.exercise.ExerciseType
import ph.mart.healthapp.core.data.food.MealType
import ph.mart.healthapp.core.data.food.RecognitionConfidence
import ph.mart.healthapp.core.data.food.RecognizedFood
import ph.mart.healthapp.core.data.profile.UnitSystem
import ph.mart.healthapp.core.designsystem.component.AppBottomSheet
import ph.mart.healthapp.core.designsystem.component.PrimaryButton
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.core.designsystem.theme.Motion
import ph.mart.healthapp.feature.food.R
import ph.mart.healthapp.feature.food.ui.quicklog.components.LocalQuickLogShared
import ph.mart.healthapp.feature.food.ui.quicklog.components.QuickLogConversation
import ph.mart.healthapp.feature.food.ui.quicklog.components.QuickLogInputBar
import ph.mart.healthapp.feature.food.ui.quicklog.components.QuickLogMessageLine
import ph.mart.healthapp.feature.food.ui.quicklog.components.QuickLogMotion
import ph.mart.healthapp.feature.food.ui.quicklog.components.quickLogShared
import ph.mart.healthapp.feature.food.ui.quicklog.components.retainLast
import ph.mart.healthapp.feature.food.ui.shared.toFoodEntry

/**
 * The FAB's sheet: one field that reads what the user ate or did, asks back when the sentence left
 * out the thing the estimate turns on, and logs both kinds from one confirmation. A photo of the
 * plate can ride with the words, and the barcode sits under the field as the one way in that is not
 * about the meal at all.
 *
 * The photo is taken or picked here rather than on the camera route: the system camera
 * (`TakePicture` into a cache file the app's FileProvider shares, `CAMERA` asked for first because
 * the manifest declares it) or the photo picker, both decoded by `:core:camera`'s
 * `decodeRotatedBitmap` at the size a capture gets.
 *
 * A sheet rather than a route for the reason every FAB sheet is one — back closes it onto the tab
 * it was opened over — and hosted by `AppScaffold`, so everything leaving it is a callback:
 * [onScanBarcode] pushes its route (day 0, the FAB is today-only), and [onLogged] hands the host the credited burn for its snackbar and the `undo` that reverses the
 * whole log — the host owns the snackbar, and this sheet is gone by the time anyone taps it.
 * [onAskCoach] is the diary's coach door in the same shape: once a conversation has started, what
 * the user said goes to the coach's field unsent, under a chip naming the quick log — the coach can
 * draft the log itself from there, so there is no door back.
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
    onScanBarcode: () -> Unit,
    onLogged: (creditedKcal: Int, undo: () -> Unit) -> Unit,
    onAskCoach: (question: String, source: String) -> Unit,
    viewModel: QuickLogViewModel = koinViewModel(),
) {
    val uiState by viewModel.collectAsState()
    val state = rememberQuickLogState()

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val attach: (Uri) -> Unit = { uri ->
        scope.launch { decodeRotatedBitmap(context, uri)?.let { state.photo = it } }
    }
    val captureUri = remember { captureUri(context) }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { taken ->
        if (taken) attach(captureUri)
    }
    // Guarded: a work profile or a policy can leave no camera app to answer, and the picker is
    // still there — `HealthConnectionScreen`'s refusal-to-crash around its own intent.
    val takePhoto = { runCatching { camera.launch(captureUri) } }
    val cameraPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) takePhoto() else state.message = R.string.food_quick_camera_denied
    }
    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let(attach)
    }

    viewModel.collectSideEffect { effect ->
        when (effect) {
            is QuickLogSideEffect.Asked -> state.applyQuestion(effect.question)
            is QuickLogSideEffect.Parsed -> {
                state.applyParsed(
                    foods = effect.foods,
                    exercises = effect.exercises,
                    mealType = effect.mealType,
                    waterGlasses = effect.waterGlasses,
                    weightKg = effect.weightKg,
                    offline = effect.offline,
                )
                if (effect.offline) state.message = R.string.food_quick_offline_matched
            }
            is QuickLogSideEffect.NothingFound ->
                state.restoreLast(if (effect.offline) R.string.food_quick_offline else R.string.food_quick_nothing)
            QuickLogSideEffect.Failed -> state.restoreLast(R.string.food_quick_failed)
            // Reported before the dismiss — `LogExerciseSheet`'s order, for its reason.
            // The ViewModel outlives the sheet, so the undo still reaches it after the dismiss.
            is QuickLogSideEffect.Logged -> {
                onLogged(effect.creditedKcal) { viewModel.handleEvent(QuickLogEvent.OnUndo(effect.batch)) }
                onDismiss()
            }
        }
    }

    // The ViewModel outlives this sheet, so an answer still in flight would otherwise land on the
    // next one the FAB opens.
    val cancel = { viewModel.handleEvent(QuickLogEvent.OnCancel) }
    val coachSource = stringResource(R.string.food_quick_coach_source)

    QuickLogContent(
        state = state,
        unit = uiState.unit,
        onDismiss = {
            cancel()
            onDismiss()
        },
        // Offline is the ViewModel's call now: it matches on the phone rather than refusing.
        onSend = { viewModel.handleEvent(QuickLogEvent.OnSend(state.send(), state.photo)) },
        onCancel = {
            cancel()
            state.restoreLast(null)
        },
        onLog = {
            viewModel.handleEvent(
                QuickLogEvent.OnLog(
                    foods = state.foods.map { it.toFoodEntry() },
                    exercises = state.exercises,
                    sentence = state.userSentence,
                    waterGlasses = state.waterGlasses,
                    weightKg = state.weightKg,
                    photo = state.photo,
                ),
            )
        },
        onTakePhoto = {
            val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
            if (granted) takePhoto() else cameraPermission.launch(Manifest.permission.CAMERA)
        },
        onPickPhoto = { gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
        onScanBarcode = {
            cancel()
            onScanBarcode()
        },
        onContinueInCoach = state.coachQuestion?.let { question ->
            {
                cancel()
                onAskCoach(question, coachSource)
            }
        },
    )
}

/**
 * The sheet as the handoff draws it. Pinned header, then the conversation scrolling under it, then
 * the bar — message, Log, photo, field, the ways in — bottom-anchored, so anything appearing above
 * the field grows the bar upward and the field itself never moves. A blank start has no content at
 * all, so the most-seen state is a header, a composer and the keyboard.
 *
 * The whole sheet sits in one [SharedTransitionLayout], passed in through the sheet's `container`
 * so it lives in the sheet's own window: that is what lets a sent sentence grow out of the field
 * into its bubble and a cancelled one fly back ([quickLogShared]).
 */
@Composable
private fun QuickLogContent(
    state: QuickLogState,
    onDismiss: () -> Unit,
    unit: UnitSystem = UnitSystem.Metric,
    onSend: () -> Unit,
    onCancel: () -> Unit,
    onLog: () -> Unit,
    onTakePhoto: () -> Unit,
    onPickPhoto: () -> Unit,
    onScanBarcode: () -> Unit,
    onContinueInCoach: (() -> Unit)? = null,
) {
    // None while a call runs (A11): there is nothing to type then, and a prompt would say there was.
    val placeholder = when {
        state.thinking -> null
        state.question != null -> R.string.food_quick_answer_placeholder
        state.hasResult -> R.string.food_quick_change_placeholder
        state.photo != null -> R.string.food_quick_photo_placeholder
        else -> R.string.food_quick_placeholder
    }?.let { stringResource(it) }
    val message = state.message ?: R.string.food_quick_removed.takeIf { state.showRemoved }

    AppBottomSheet(
        title = stringResource(R.string.food_quick_prompt),
        onDismiss = onDismiss,
        scrollRules = true,
        container = { sheet ->
            SharedTransitionLayout {
                CompositionLocalProvider(LocalQuickLogShared provides this) { sheet() }
            }
        },
        // The field is the bar, so it sits on the keyboard however long the review above it runs.
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
            ) {
                val shownMessage = retainLast(message)
                AnimatedVisibility(
                    visible = message != null,
                    enter = expandVertically(tween(QuickLogMotion.Enter, easing = Motion.EmphasizedDecelerate)) +
                        fadeIn(tween(QuickLogMotion.Swap, QuickLogMotion.Enter - QuickLogMotion.Swap)),
                    exit = shrinkVertically(tween(QuickLogMotion.Exit, easing = Motion.EmphasizedAccelerate)) +
                        fadeOut(tween(QuickLogMotion.Fade)),
                ) {
                    shownMessage?.let { QuickLogMessageLine(message = it, modifier = Modifier.padding(bottom = 12.dp)) }
                }
                // In on a review, out the moment a correction is sent — its height and its gap
                // together, so the field under it stays put.
                AnimatedVisibility(
                    visible = state.phase == QuickLogPhase.Review,
                    enter = expandVertically(tween(QuickLogMotion.Enter, easing = Motion.EmphasizedDecelerate)) +
                        fadeIn(tween(QuickLogMotion.Swap, QuickLogMotion.Enter - QuickLogMotion.Swap)),
                    exit = shrinkVertically(tween(QuickLogMotion.Exit, easing = Motion.EmphasizedAccelerate)) +
                        fadeOut(tween(QuickLogMotion.Exit, easing = Motion.EmphasizedAccelerate)),
                ) {
                    PrimaryButton(
                        label = stringResource(R.string.food_quick_log),
                        onClick = onLog,
                        enabled = state.hasResult,
                        modifier = Modifier
                            .padding(bottom = 12.dp)
                            .fillMaxWidth(),
                    )
                }
                QuickLogInputBar(
                    text = state.text,
                    placeholder = placeholder,
                    thinking = state.thinking,
                    canSend = state.canSend,
                    showShortcuts = state.turns.isEmpty(),
                    turnCount = state.turns.size,
                    onTextChange = { state.text = it },
                    onSend = onSend,
                    onCancel = onCancel,
                    onTakePhoto = onTakePhoto,
                    onPickPhoto = onPickPhoto,
                    onScanBarcode = onScanBarcode,
                    sentText = state.turns.lastOrNull()?.takeIf { state.thinking && it.fromUser }?.text,
                    photo = state.photo?.asImageBitmap(),
                    onRemovePhoto = { state.photo = null },
                    onContinueInCoach = onContinueInCoach,
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
        val hasContent = state.turns.isNotEmpty() || state.parsedTurns != null
        Column(modifier = Modifier.padding(top = if (hasContent) 4.dp else 0.dp)) {
            QuickLogConversation(
                said = state.said,
                turns = state.turns,
                threadStart = state.parsedTurns ?: 0,
                thinking = state.thinking,
                foods = state.foods,
                exercises = state.exercises,
                mealType = state.mealType,
                expandedIndex = state.expandedIndex,
                onToggleFood = state::toggleExpanded,
                onFoodChange = state::updateFood,
                onRemoveFood = state::removeFood,
                onRemoveExercise = state::removeExercise,
                onMealTypeSelect = state::selectMealType,
                waterGlasses = state.waterGlasses,
                weightKg = state.weightKg,
                unit = unit,
                offline = state.offline,
                onRemoveWater = state::removeWater,
                onRemoveWeight = state::removeWeight,
            )
        }
    }
}

/** The sheet in one of its states, with the coach door drawn exactly when the sheet would draw it. */
@Composable
private fun QuickLogPreview(state: QuickLogState) {
    AppTheme {
        QuickLogContent(
            state = state,
            onDismiss = {},
            onSend = {},
            onCancel = {},
            onLog = {},
            onTakePhoto = {},
            onPickPhoto = {},
            onScanBarcode = {},
            onContinueInCoach = state.coachQuestion?.let { { } },
        )
    }
}

private val PREVIEW_FOODS = listOf(
    RecognizedFood("Scrambled eggs", 2.0, "egg", 180, 12, 1, 14, confidence = RecognitionConfidence.High),
    RecognizedFood(
        "Wholemeal toast", 1.0, "slice", 80, 4, 14, 1,
        confidence = RecognitionConfidence.Low, uncertainAbout = "a slice",
    ),
)

/** Two foods, a run, water and a weigh-in from one sentence — the handoff's review. */
private fun reviewedState(offline: Boolean = false) =
    QuickLogState(text = "two eggs and toast, then a 30 min run, 3 glasses of water, weighed 72.4").apply {
        send()
        applyParsed(
            foods = if (offline) PREVIEW_FOODS.map { it.copy(confidence = RecognitionConfidence.Low, uncertainAbout = null) } else PREVIEW_FOODS,
            exercises = listOf(ExerciseEntry(type = ExerciseType.Run, name = "along the river", minutes = 30, burnedKcal = 343)),
            mealType = MealType.Breakfast,
            waterGlasses = 3,
            weightKg = 72.4,
            offline = offline,
        )
        if (offline) message = R.string.food_quick_offline_matched
    }

/** 01 — the most-seen state: a header, a composer and the keyboard, and nothing else. */
@PreviewLightDark
@Composable
private fun QuickLogSheetPreview() {
    QuickLogPreview(QuickLogState())
}

/** 05 — a plate attached: send is ready with nothing typed. */
@PreviewLightDark
@Composable
private fun QuickLogSheetPhotoPreview() {
    QuickLogPreview(QuickLogState().apply { photo = Bitmap.createBitmap(PREVIEW_PHOTO_PX, PREVIEW_PHOTO_PX, Bitmap.Config.ARGB_8888) })
}

/** 06 — the first send reading: the words stay on screen as a bubble, the model's turn is dots. */
@PreviewLightDark
@Composable
private fun QuickLogSheetThinkingPreview() {
    QuickLogPreview(QuickLogState(text = "two eggs and toast, then a 30 min run").apply { send() })
}

/** 07 — a follow-up waiting for its answer, the conversation so far above it. */
@PreviewLightDark
@Composable
private fun QuickLogSheetQuestionPreview() {
    QuickLogPreview(
        QuickLogState(text = "rice and chicken adobo").apply {
            send()
            applyQuestion("About how much rice — one cup or two?")
        },
    )
}

/** 09 — the review: "You said" over the food card and the other card, Log over the field. */
@PreviewLightDark
@Composable
private fun QuickLogSheetReviewPreview() {
    QuickLogPreview(reviewedState())
}

/** 10 — the first food's portion open under it. */
@PreviewLightDark
@Composable
private fun QuickLogSheetPortionPreview() {
    QuickLogPreview(reviewedState().apply { expandedIndex = 0 })
}

/** 13 — matched offline: every row a guess, and the line in the bar saying so. */
@PreviewLightDark
@Composable
private fun QuickLogSheetOfflinePreview() {
    QuickLogPreview(reviewedState(offline = true))
}

/** 14 — a first send that came to nothing: the words back in the field, the coach a third chip. */
@PreviewLightDark
@Composable
private fun QuickLogSheetDeadEndPreview() {
    QuickLogPreview(
        QuickLogState(text = "feeling pretty good today").apply {
            send()
            restoreLast(R.string.food_quick_nothing)
        },
    )
}

/** 16 — every row removed: the said line stays, Log is disabled, and the bar says why. */
@PreviewLightDark
@Composable
private fun QuickLogSheetRemovedPreview() {
    QuickLogPreview(
        reviewedState().apply {
            repeat(foods.size) { removeFood(0) }
            removeExercise(0)
            removeWater()
            removeWeight()
        },
    )
}

private const val PREVIEW_PHOTO_PX = 160

/**
 * The file the system camera writes into — `cacheDir/capture/`, the one directory `file_paths.xml`
 * shares for it, and one fixed name: a second photo replaces the first, and nothing outlives the
 * log that reads it.
 */
private fun captureUri(context: Context): Uri {
    val file = File(context.cacheDir, "capture").apply { mkdirs() }.resolve("quicklog.jpg")
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}
