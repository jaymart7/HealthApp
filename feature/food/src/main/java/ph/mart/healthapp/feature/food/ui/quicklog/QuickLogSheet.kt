package ph.mart.healthapp.feature.food.ui.quicklog

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
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
import ph.mart.healthapp.core.data.food.QuickLogTurn
import ph.mart.healthapp.core.data.food.RecognitionConfidence
import ph.mart.healthapp.core.data.food.RecognizedFood
import ph.mart.healthapp.core.data.profile.UnitSystem
import ph.mart.healthapp.core.designsystem.component.AppBottomSheet
import ph.mart.healthapp.core.designsystem.component.PrimaryButton
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.food.R
import ph.mart.healthapp.feature.food.ui.quicklog.components.QuickLogConversation
import ph.mart.healthapp.feature.food.ui.quicklog.components.QuickLogInputBar
import ph.mart.healthapp.feature.food.ui.shared.components.RecentSentences
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
    val cameraPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) camera.launch(captureUri) else state.message = R.string.food_quick_camera_denied
    }
    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let(attach)
    }

    viewModel.collectSideEffect { effect ->
        when (effect) {
            is QuickLogSideEffect.Asked -> state.applyQuestion(effect.question)
            is QuickLogSideEffect.Parsed -> {
                state.applyParsed(effect.foods, effect.exercises, effect.mealType, effect.waterGlasses, effect.weightKg)
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

    QuickLogContent(
        state = state,
        recentSentences = uiState.recentSentences,
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
            if (granted) camera.launch(captureUri) else cameraPermission.launch(Manifest.permission.CAMERA)
        },
        onPickPhoto = { gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
        onScanBarcode = {
            cancel()
            onScanBarcode()
        },
    )
}

@Composable
private fun QuickLogContent(
    state: QuickLogState,
    recentSentences: List<String>,
    onDismiss: () -> Unit,
    unit: UnitSystem = UnitSystem.Metric,
    onSend: () -> Unit,
    onCancel: () -> Unit,
    onLog: () -> Unit,
    onTakePhoto: () -> Unit,
    onPickPhoto: () -> Unit,
    onScanBarcode: () -> Unit,
) {
    val placeholder = stringResource(
        when {
            state.question != null -> R.string.food_quick_answer_placeholder
            state.hasResult -> R.string.food_quick_change_placeholder
            state.photo != null -> R.string.food_quick_photo_placeholder
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
                    onTakePhoto = onTakePhoto,
                    onPickPhoto = onPickPhoto,
                    onScanBarcode = onScanBarcode,
                    photo = state.photo?.asImageBitmap(),
                    onRemovePhoto = { state.photo = null },
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
        // Only before anything is typed or sent — `RecentSentences`' own rule — and a tap fills the
        // field rather than sending: a remembered sentence is worth correcting before a request.
        if (state.turns.isEmpty() && state.text.isBlank() && state.photo == null && recentSentences.isNotEmpty()) {
            RecentSentences(
                sentences = recentSentences,
                onSelect = { state.text = it },
                rowColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            )
        }
        QuickLogConversation(
            lastSaid = state.lastSaid,
            question = state.question,
            foods = state.foods,
            exercises = state.exercises,
            mealType = state.mealType,
            expandedIndex = state.expandedIndex,
            message = state.message?.let { stringResource(it) },
            onToggleFood = state::toggleExpanded,
            onFoodChange = state::updateFood,
            onRemoveFood = state::removeFood,
            onRemoveExercise = state::removeExercise,
            onMealTypeSelect = state::selectMealType,
            waterGlasses = state.waterGlasses,
            weightKg = state.weightKg,
            unit = unit,
            onRemoveWater = state::removeWater,
            onRemoveWeight = state::removeWeight,
        )
    }
}

@PreviewLightDark
@Composable
private fun QuickLogSheetPreview() {
    AppTheme {
        QuickLogContent(
            state = QuickLogState(),
            recentSentences = emptyList(),
            onDismiss = {},
            onSend = {},
            onCancel = {},
            onLog = {},
            onTakePhoto = {},
            onPickPhoto = {},
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
            recentSentences = emptyList(),
            onDismiss = {},
            onSend = {},
            onCancel = {},
            onLog = {},
            onTakePhoto = {},
            onPickPhoto = {},
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
                    mealType = null,
                )
            },
            recentSentences = emptyList(),
            onDismiss = {},
            onSend = {},
            onCancel = {},
            onLog = {},
            onTakePhoto = {},
            onPickPhoto = {},
            onScanBarcode = {},
        )
    }
}

/** A blank start with meals behind it: the strip is the whole difference, and it is gone the moment
 * a key is pressed. */
@PreviewLightDark
@Composable
private fun QuickLogSheetRecentPreview() {
    AppTheme {
        QuickLogContent(
            state = QuickLogState(),
            recentSentences = listOf("chicken adobo and rice, two cups", "two eggs and toast"),
            onDismiss = {},
            onSend = {},
            onCancel = {},
            onLog = {},
            onTakePhoto = {},
            onPickPhoto = {},
            onScanBarcode = {},
        )
    }
}

/**
 * The file the system camera writes into — `cacheDir/capture/`, the one directory `file_paths.xml`
 * shares for it, and one fixed name: a second photo replaces the first, and nothing outlives the
 * log that reads it.
 */
private fun captureUri(context: Context): Uri {
    val file = File(context.cacheDir, "capture").apply { mkdirs() }.resolve("quicklog.jpg")
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}
