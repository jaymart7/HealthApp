package ph.mart.healthapp.feature.food.ui.photo

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.orbitmvi.orbit.compose.collectSideEffect
import ph.mart.healthapp.core.camera.CameraCaptureController
import ph.mart.healthapp.core.camera.rememberCameraCaptureController
import ph.mart.healthapp.core.data.food.RecognitionResult
import ph.mart.healthapp.core.designsystem.component.DiscardConfirmDialog
import ph.mart.healthapp.feature.food.ui.diary.toFoodEntry
import ph.mart.healthapp.feature.food.ui.photo.components.AnalyzingScreen
import ph.mart.healthapp.feature.food.ui.photo.components.CameraPermissionScreen
import ph.mart.healthapp.feature.food.ui.photo.components.CaptureScreen
import ph.mart.healthapp.feature.food.ui.photo.components.ConfirmationScreen
import ph.mart.healthapp.feature.food.ui.photo.components.ManualSearchScreen
import ph.mart.healthapp.feature.food.ui.photo.components.PhotoOfflineScreen
import ph.mart.healthapp.feature.food.ui.photo.components.RetryScreen
import ph.mart.healthapp.feature.food.ui.shared.components.ScanConfirmationScreen
import ph.mart.healthapp.feature.food.ui.shared.openAppSettings
import ph.mart.healthapp.feature.food.ui.shared.permissionPermanentlyDenied
import ph.mart.healthapp.feature.food.ui.shared.toFoodEntry

private const val SEARCH_SUBTITLE =
    "From the food database — adjust the portion to match what you ate."

/**
 * Hosts the whole 6(+1)-state flow from `PhotoLogging.dc.html`, same shape as
 * [ph.mart.healthapp.feature.onboarding.ui.onboarding.OnboardingScreen]'s single screen + internal step
 * state: one [NavigationBackHandler], always mounted, whose `onBackCompleted` dispatches on the
 * current [CaptureFlow] rather than applying one behavior to every state.
 */
@Composable
fun PhotoCaptureScreen(onExit: () -> Unit, viewModel: PhotoCaptureViewModel = koinViewModel()) {
    val state = rememberPhotoCaptureScreen()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val cameraController = rememberCameraCaptureController()

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED,
        )
    }
    val permissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            hasCameraPermission = granted
            state.flow = if (granted) CaptureFlow.Capture else CaptureFlow.PermissionDenied
        }
    LaunchedEffect(Unit) {
        if (!hasCameraPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    viewModel.collectSideEffect { effect ->
        when (effect) {
            is PhotoCaptureSideEffect.RecognitionFinished -> when (val result = effect.result) {
                is RecognitionResult.Success -> state.applyRecognized(result.food)
                RecognitionResult.NoFoodDetected -> state.flow = CaptureFlow.NoFood
                RecognitionResult.Failed -> state.flow = CaptureFlow.Retry
            }

            PhotoCaptureSideEffect.MealLogged -> onExit()
        }
    }

    // Set when a retry finds the network still down, so the screen says so instead of appearing
    // to ignore the tap.
    var retriedWhileOffline by remember { mutableStateOf(false) }

    val backHandlerState = rememberNavigationEventState(currentInfo = NavigationEventInfo.None)
    NavigationBackHandler(
        state = backHandlerState,
        onBackCompleted = {
            when (state.flow) {
                CaptureFlow.Capture -> onExit()
                CaptureFlow.Analyzing -> {
                    viewModel.handleEvent(PhotoCaptureEvent.OnCancelAnalysis)
                    state.flow = CaptureFlow.Capture
                }

                CaptureFlow.Confirmation -> if (state.isDirty) {
                    state.pendingDiscard = { state.flow = CaptureFlow.Capture }
                } else {
                    state.flow = CaptureFlow.Capture
                }

                // Back steps to the search it was picked from, not out of the flow.
                CaptureFlow.SearchConfirmation -> if (state.isDirty) {
                    state.pendingDiscard = { state.flow = CaptureFlow.NoFood }
                } else {
                    state.flow = CaptureFlow.NoFood
                }

                CaptureFlow.Retry, CaptureFlow.NoFood, CaptureFlow.Offline, CaptureFlow.PermissionDenied -> onExit()
            }
        },
    )

    Surface(color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxSize()) {
        // AppScaffold hands this route the whole window. Capture/Analyzing are full-bleed camera
        // surfaces that inset their own chrome; the other five states are ordinary content.
        // safeDrawing unions the IME, so Confirmation's editable rows and ManualSearch's field
        // get keyboard avoidance from this same line.
        val insets = if (state.flow == CaptureFlow.Capture || state.flow == CaptureFlow.Analyzing) {
            Modifier
        } else {
            Modifier.safeDrawingPadding()
        }
        Box(modifier = Modifier.fillMaxSize().then(insets)) {
            when (state.flow) {
                CaptureFlow.Capture -> if (hasCameraPermission) {
                    CaptureScreen(
                        onClose = onExit,
                        onCapture = {
                            onCaptureRequested(
                                viewModel,
                                scope,
                                cameraController,
                                state
                            )
                        },
                        cameraPreview = { cameraController.Preview(modifier = Modifier.fillMaxSize()) },
                    )
                }

                CaptureFlow.Analyzing -> state.photo?.let { photo ->
                    AnalyzingScreen(
                        photo = photo,
                        onCancel = {
                            viewModel.handleEvent(PhotoCaptureEvent.OnCancelAnalysis)
                            state.flow = CaptureFlow.Capture
                        },
                    )
                }

                CaptureFlow.Confirmation -> state.photo?.let { photo ->
                    ConfirmationScreen(
                        photo = photo,
                        form = state.form,
                        confidence = state.confidence,
                        onFormChange = { state.form = it },
                        onMealTypeSelect = state::selectMealType,
                        onSearchInstead = { state.flow = CaptureFlow.NoFood },
                        onLogMeal = { viewModel.handleEvent(PhotoCaptureEvent.OnLogMeal(state.form.toFoodEntry())) },
                        // Back already asks before throwing away edits; the button that means the
                        // same thing asked nothing at all.
                        onDiscard = { if (state.isDirty) state.pendingDiscard = { onExit() } else onExit() },
                    )
                }

                CaptureFlow.Retry -> RetryScreen(
                    photo = state.photo,
                    onRetry = { state.flow = CaptureFlow.Capture },
                    onLogManually = { state.flow = CaptureFlow.NoFood },
                )

                CaptureFlow.NoFood -> ManualSearchScreen(
                    onSelectProduct = state::applyProduct,
                    onEnterManually = state::startManualEntry,
                    onCancel = onExit,
                )

                // A searched or hand-entered item is not an AI detection, so it gets the barcode
                // flow's plain confirmation — no photo, no AI chip.
                CaptureFlow.SearchConfirmation -> ScanConfirmationScreen(
                    form = state.form,
                    subtitle = SEARCH_SUBTITLE,
                    onFormChange = { state.form = it },
                    onMealTypeSelect = state::selectMealType,
                    onLogEntry = { viewModel.handleEvent(PhotoCaptureEvent.OnLogMeal(state.form.toFoodEntry())) },
                    onDiscard = { if (state.isDirty) state.pendingDiscard = { onExit() } else onExit() },
                )

                CaptureFlow.Offline -> PhotoOfflineScreen(
                    retried = retriedWhileOffline,
                    onLogManually = { state.flow = CaptureFlow.NoFood },
                    onRetry = {
                        if (viewModel.isOnline()) {
                            retriedWhileOffline = false
                            state.flow = CaptureFlow.Capture
                        } else {
                            retriedWhileOffline = true
                        }
                    },
                )

                CaptureFlow.PermissionDenied -> CameraPermissionScreen(
                    // Once the prompt is spent, launching it again does nothing at all and the
                    // screen becomes a dead end — Settings is the only door left.
                    settingsOnly = context.permissionPermanentlyDenied(Manifest.permission.CAMERA),
                    onGrant = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                    onOpenSettings = { context.openAppSettings() },
                    onBack = onExit,
                )
            }

            state.pendingDiscard?.let { discard ->
                DiscardConfirmDialog(
                    title = "Discard this meal?",
                    body = "You've made edits that haven't been logged yet.",
                    onConfirm = {
                        state.pendingDiscard = null
                        discard()
                    },
                    onDismiss = { state.pendingDiscard = null },
                )
            }
        }
    }
}

private fun onCaptureRequested(
    viewModel: PhotoCaptureViewModel,
    scope: CoroutineScope,
    cameraController: CameraCaptureController,
    state: PhotoCaptureScreenState,
) {
    if (!viewModel.isOnline()) {
        state.flow = CaptureFlow.Offline
        return
    }
    scope.launch {
        val photo = cameraController.capture()
        state.photo = photo
        state.flow = CaptureFlow.Analyzing
        viewModel.handleEvent(PhotoCaptureEvent.OnCapture(photo))
    }
}
