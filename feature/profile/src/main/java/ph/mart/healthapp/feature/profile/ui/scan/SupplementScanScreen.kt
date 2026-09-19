package ph.mart.healthapp.feature.profile.ui.scan

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.orbitmvi.orbit.compose.collectSideEffect
import ph.mart.healthapp.core.camera.decodeRotatedBitmap
import ph.mart.healthapp.core.camera.openAppSettings
import ph.mart.healthapp.core.camera.permissionPermanentlyDenied
import ph.mart.healthapp.core.camera.rememberCameraCaptureController
import ph.mart.healthapp.core.data.supplement.SupplementScanResult
import ph.mart.healthapp.core.designsystem.component.CameraPermissionScreen
import ph.mart.healthapp.core.designsystem.component.CaptureScreen
import ph.mart.healthapp.core.designsystem.component.FullScreenState
import ph.mart.healthapp.core.designsystem.component.LabelGuideSize
import ph.mart.healthapp.core.designsystem.component.MascotAvatar
import ph.mart.healthapp.core.designsystem.component.MascotState
import ph.mart.healthapp.core.designsystem.component.PrimaryButton
import ph.mart.healthapp.core.designsystem.component.SecondaryButton
import ph.mart.healthapp.feature.profile.R
import ph.mart.healthapp.feature.profile.ui.supplement.components.SupplementEditSheet

/**
 * The supplement-label flow: photograph the Supplement Facts panel, let a model transcribe it,
 * check it, keep it.
 *
 * **It answers the supplement list's typing job.** A multivitamin declares twenty lines and the
 * list could only ever hold three of them, typed — so a supplement the user takes every morning
 * contributed nothing to the day's nutrient panel, which is exactly the screen those figures
 * belong on. The bottle has all of it printed on the back.
 *
 * Built to `LabelScanScreen`'s shape, dead ends included, with one difference: **there is no
 * enter-by-hand state.** The screen that adds a supplement by hand is the list this was opened
 * from, one back press away, and a second copy of the form here would be a second save path to
 * the same table. So a dead end offers a retake and a way back, and the list does the rest.
 *
 * Confirmation is [SupplementEditSheet] over a plain surface — the same sheet the list uses, the
 * same save, seeded rather than blank. Its back closes the sheet, which is `ModalBottomSheet`'s
 * own handler and the reason the branch below never fires in practice.
 */
@Composable
fun SupplementScanScreen(
    onExit: () -> Unit,
    viewModel: SupplementScanViewModel = koinViewModel(),
) {
    val state = rememberSupplementScanScreen()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val cameraController = rememberCameraCaptureController()

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA,
            ) == PackageManager.PERMISSION_GRANTED,
        )
    }
    val permissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            hasCameraPermission = granted
            state.flow = if (granted) SupplementFlow.Capture else SupplementFlow.PermissionDenied
        }
    LaunchedEffect(Unit) {
        if (!hasCameraPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    // A photo of a bottle already taken joins the flow exactly where the shutter does.
    val galleryLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) startRead(viewModel, scope, state) { decodeRotatedBitmap(context, uri) }
        }

    viewModel.collectSideEffect { effect ->
        when (effect) {
            is SupplementScanSideEffect.ReadFinished -> when (val result = effect.result) {
                is SupplementScanResult.Found -> state.applyReading(result.reading)
                SupplementScanResult.NoLabelFound -> state.flow = SupplementFlow.Unreadable
                SupplementScanResult.Failed -> state.flow = SupplementFlow.Retry
            }

            SupplementScanSideEffect.Saved -> onExit()
        }
    }

    // Set when a retry finds the network still down, so the screen says so instead of appearing to
    // ignore the tap — the shape every other scan flow uses.
    var retriedWhileOffline by remember { mutableStateOf(false) }

    val backHandlerState = rememberNavigationEventState(currentInfo = NavigationEventInfo.None)
    NavigationBackHandler(
        state = backHandlerState,
        onBackCompleted = {
            when (state.flow) {
                SupplementFlow.Capture -> onExit()
                SupplementFlow.Reading -> {
                    viewModel.handleEvent(SupplementScanEvent.OnCancelRead)
                    state.flow = SupplementFlow.Capture
                }

                // The sheet's own handler takes this first; the branch keeps the dispatch total.
                SupplementFlow.Confirmation -> state.recapture()

                SupplementFlow.Unreadable, SupplementFlow.Retry, SupplementFlow.Offline,
                SupplementFlow.PermissionDenied,
                -> onExit()
            }
        },
    )

    Surface(color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxSize()) {
        // Capture is a full-bleed camera surface that insets its own chrome; every other state is
        // ordinary content, and safeDrawing unions the IME so the sheet's fields clear the keyboard.
        val insets = if (state.flow == SupplementFlow.Capture) Modifier else Modifier.safeDrawingPadding()
        Box(modifier = Modifier.fillMaxSize().then(insets)) {
            when (state.flow) {
                SupplementFlow.Capture -> if (hasCameraPermission) {
                    CaptureScreen(
                        onClose = onExit,
                        onCapture = { startRead(viewModel, scope, state) { cameraController.capture() } },
                        onPickPhoto = {
                            galleryLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                            )
                        },
                        // Not a third door: leaving is how this flow says "I'll type it", and the
                        // list behind it is the form.
                        onEnterManually = onExit,
                        hint = stringResource(R.string.profile_supplements_scan_frame),
                        // A Supplement Facts panel is a column, like a Nutrition Facts one.
                        guideSize = LabelGuideSize,
                        cameraPreview = { cameraController.Preview(modifier = Modifier.fillMaxSize()) },
                    )
                }

                SupplementFlow.Reading -> FullScreenState(
                    icon = { CircularProgressIndicator(color = MaterialTheme.colorScheme.primary) },
                    heading = stringResource(R.string.profile_supplements_scan_reading),
                    body = stringResource(R.string.profile_supplements_scan_reading_body),
                    actions = {
                        SecondaryButton(
                            label = stringResource(R.string.profile_cancel),
                            onClick = {
                                viewModel.handleEvent(SupplementScanEvent.OnCancelRead)
                                state.flow = SupplementFlow.Capture
                            },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    },
                )

                // Nothing behind the sheet but the surface: the camera is torn down by now and the
                // panel the user is checking is in their hand, not on screen.
                SupplementFlow.Confirmation -> state.reading?.let { seeded ->
                    SupplementEditSheet(
                        supplement = seeded,
                        onDismiss = state::recapture,
                        onSave = { viewModel.handleEvent(SupplementScanEvent.OnSave(it)) },
                    )
                }

                // Two dead ends, two different things to say. "There is no panel in that photo" is
                // the user's aim; "that didn't work" is ours.
                SupplementFlow.Unreadable -> DeadEnd(
                    heading = stringResource(R.string.profile_supplements_scan_unreadable),
                    body = stringResource(R.string.profile_supplements_scan_unreadable_body),
                    onRetake = state::recapture,
                    onBack = onExit,
                )

                SupplementFlow.Retry -> DeadEnd(
                    heading = stringResource(R.string.profile_supplements_scan_failed),
                    body = stringResource(R.string.profile_supplements_scan_failed_body),
                    onRetake = state::recapture,
                    onBack = onExit,
                )

                SupplementFlow.Offline -> DeadEnd(
                    heading = stringResource(R.string.profile_no_connection),
                    body = if (retriedWhileOffline) {
                        stringResource(R.string.profile_supplements_scan_offline_retry)
                    } else {
                        stringResource(R.string.profile_supplements_scan_offline)
                    },
                    retakeLabel = stringResource(R.string.profile_try_again),
                    onRetake = {
                        if (viewModel.isOnline()) {
                            retriedWhileOffline = false
                            if (hasCameraPermission) {
                                state.recapture()
                            } else {
                                permissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        } else {
                            retriedWhileOffline = true
                        }
                    },
                    onBack = onExit,
                )

                // Once the prompt is spent, launching it again does nothing at all and the screen
                // becomes a dead end — Settings is the only door left.
                SupplementFlow.PermissionDenied -> CameraPermissionScreen(
                    settingsOnly = context.permissionPermanentlyDenied(Manifest.permission.CAMERA),
                    grantBody = stringResource(R.string.profile_supplements_scan_permission_grant),
                    settingsBody = stringResource(R.string.profile_supplements_scan_permission_settings),
                    onGrant = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                    onOpenSettings = { context.openAppSettings() },
                    onBack = onExit,
                )
            }
        }
    }
}

/**
 * The three ways this flow stops short, drawn the same way.
 *
 * **Retaking leads here**, the opposite of the label flow's order and for the reason that flow
 * gives in reverse: there, typing the entry in was the job the scan was saving and the form was on
 * the same screen. Here the form is the list one press back, so the thing worth offering first is
 * another shot — the bottle is still in the user's hand.
 */
@Composable
private fun DeadEnd(
    heading: String,
    body: String,
    onRetake: () -> Unit,
    onBack: () -> Unit,
    retakeLabel: String = stringResource(R.string.profile_supplements_scan_retake),
) {
    FullScreenState(
        icon = { MascotAvatar(state = MascotState.Sleepy, size = 64.dp) },
        heading = heading,
        body = body,
        actions = {
            PrimaryButton(
                label = retakeLabel,
                onClick = onRetake,
                modifier = Modifier.fillMaxWidth(),
            )
            SecondaryButton(
                label = stringResource(R.string.profile_supplements_scan_add_by_hand),
                onClick = onBack,
                modifier = Modifier.fillMaxWidth(),
            )
        },
    )
}

/**
 * The one path into [SupplementFlow.Reading], whether the photo comes off the sensor or out of the
 * gallery. The offline check runs before the photo is loaded, because reading is the online part
 * and there is no point decoding first. A null photo is one that never arrived; either way the
 * camera stays up.
 */
private fun startRead(
    viewModel: SupplementScanViewModel,
    scope: CoroutineScope,
    state: SupplementScanScreenState,
    loadPhoto: suspend () -> Bitmap?,
) {
    if (!viewModel.isOnline()) {
        state.flow = SupplementFlow.Offline
        return
    }
    scope.launch {
        val photo = loadPhoto() ?: return@launch
        state.flow = SupplementFlow.Reading
        viewModel.handleEvent(SupplementScanEvent.OnPhotoCaptured(photo))
    }
}
