package ph.mart.healthapp.feature.food.ui.label

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
import ph.mart.healthapp.core.data.food.LabelBasis
import ph.mart.healthapp.core.data.food.LabelScanResult
import ph.mart.healthapp.core.designsystem.component.CameraPermissionScreen
import ph.mart.healthapp.core.designsystem.component.CaptureScreen
import ph.mart.healthapp.core.designsystem.component.DiscardConfirmDialog
import ph.mart.healthapp.core.designsystem.component.FullScreenState
import ph.mart.healthapp.core.designsystem.component.LabelGuideSize
import ph.mart.healthapp.core.designsystem.component.MascotAvatar
import ph.mart.healthapp.core.designsystem.component.MascotState
import ph.mart.healthapp.core.designsystem.component.PrimaryButton
import ph.mart.healthapp.core.designsystem.component.SecondaryButton
import ph.mart.healthapp.feature.food.R
import ph.mart.healthapp.feature.food.ui.barcode.BarcodeScanScreen
import ph.mart.healthapp.feature.food.ui.label.components.LabelPanelReadout
import ph.mart.healthapp.feature.food.ui.shared.components.ScanConfirmationScreen
import ph.mart.healthapp.feature.food.ui.shared.isSaveableFood
import ph.mart.healthapp.feature.food.ui.shared.toFoodEntry
import ph.mart.healthapp.feature.food.ui.shared.toSuggestion

/**
 * The nutrition-label flow: photograph the panel, let a model transcribe it, check it, log it.
 *
 * **It answers the barcode flow's dead end.** Open Food Facts and FoodData Central between them
 * miss a locally-packaged product often enough that "Add it manually" was the ordinary outcome of a
 * scan — a blank form asking the user to retype figures printed six inches from the camera, and one
 * that could never carry vitamin D, calcium, iron or potassium at all, since those four are seeded
 * and never typed. The packet has all of it printed on the back.
 *
 * Built to [BarcodeScanScreen]'s shape rather than the photo flow's, for the same two reasons: one
 * subject rather than a plate of rows, and dead ends drawn as [FullScreenState] here rather than as
 * screens of their own. One always-mounted [NavigationBackHandler] dispatches on [LabelFlow].
 */
@Composable
fun LabelScanScreen(
    dateEpochDay: Long,
    onExit: () -> Unit,
    viewModel: LabelScanViewModel = koinViewModel(),
) {
    val state = rememberLabelScanScreen()
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
            state.flow = if (granted) LabelFlow.Capture else LabelFlow.PermissionDenied
        }
    LaunchedEffect(Unit) {
        if (!hasCameraPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    // A photo of a packet already taken joins the flow exactly where the shutter does — nothing
    // downstream of [startRead] can tell the two apart.
    val galleryLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) startRead(viewModel, scope, state) { decodeRotatedBitmap(context, uri) }
        }

    viewModel.collectSideEffect { effect ->
        when (effect) {
            is LabelScanSideEffect.ReadFinished -> when (val result = effect.result) {
                is LabelScanResult.Found -> state.applyReading(result.reading)
                LabelScanResult.NoLabelFound -> state.flow = LabelFlow.Unreadable
                LabelScanResult.Failed -> state.flow = LabelFlow.Retry
            }

            LabelScanSideEffect.EntryLogged -> onExit()
        }
    }

    // Set when a retry finds the network still down, so the screen says so instead of appearing to
    // ignore the tap — the shape the photo and barcode flows both use.
    var retriedWhileOffline by remember { mutableStateOf(false) }

    val backHandlerState = rememberNavigationEventState(currentInfo = NavigationEventInfo.None)
    NavigationBackHandler(
        state = backHandlerState,
        onBackCompleted = {
            when (state.flow) {
                LabelFlow.Capture -> onExit()
                LabelFlow.Reading -> {
                    viewModel.handleEvent(LabelScanEvent.OnCancelRead)
                    state.flow = LabelFlow.Capture
                }

                LabelFlow.Confirmation -> if (state.isDirty) {
                    state.pendingDiscard = { state.recapture() }
                } else {
                    state.recapture()
                }

                LabelFlow.Unreadable, LabelFlow.Retry, LabelFlow.Offline, LabelFlow.PermissionDenied -> onExit()
            }
        },
    )

    Surface(color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxSize()) {
        // Capture is a full-bleed camera surface that insets its own chrome; every other state is
        // ordinary content, and safeDrawing unions the IME so the editable rows clear the keyboard.
        val insets = if (state.flow == LabelFlow.Capture) Modifier else Modifier.safeDrawingPadding()
        Box(modifier = Modifier.fillMaxSize().then(insets)) {
            when (state.flow) {
                LabelFlow.Capture -> if (hasCameraPermission) {
                    CaptureScreen(
                        onClose = onExit,
                        onCapture = { startRead(viewModel, scope, state) { cameraController.capture() } },
                        onPickPhoto = {
                            galleryLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                            )
                        },
                        onEnterManually = state::startManualEntry,
                        hint = stringResource(R.string.food_label_frame),
                        // A panel is a column, not a plate.
                        guideSize = LabelGuideSize,
                        cameraPreview = { cameraController.Preview(modifier = Modifier.fillMaxSize()) },
                    )
                }

                // The barcode flow's LookingUp, with the panel's verb: the photo is not shown back
                // while it is read, because unlike a plate it is not the thing being confirmed.
                LabelFlow.Reading -> FullScreenState(
                    icon = { CircularProgressIndicator(color = MaterialTheme.colorScheme.primary) },
                    heading = stringResource(R.string.food_label_reading),
                    body = stringResource(R.string.food_label_reading_body),
                    actions = {
                        SecondaryButton(
                            label = stringResource(R.string.food_cancel),
                            onClick = {
                                viewModel.handleEvent(LabelScanEvent.OnCancelRead)
                                state.flow = LabelFlow.Capture
                            },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    },
                )

                LabelFlow.Confirmation -> ScanConfirmationScreen(
                    form = state.form,
                    // A read panel is a seeded form even when it could not read the name, so the
                    // presets and the label caveat are right; only a form nobody seeded is manual.
                    manualEntry = state.readBasis == null,
                    subtitle = stringResource(R.string.food_scan_manual_subtitle)
                        .takeIf { state.readBasis == null },
                    // The figures were copied off a packet by a model, and the chip is what says a
                    // model was involved at all — the caveat under the portion says what it read.
                    aiLabel = stringResource(R.string.food_label_ai_chip).takeIf { state.readBasis != null },
                    caveat = labelCaveat(state.readBasis),
                    caveatBaseAmount = state.seedAmount,
                    onFormChange = { state.form = it },
                    onMealTypeSelect = state::selectMealType,
                    // The four panel nutrients nobody can type, shown so they can be checked.
                    belowMacros = {
                        if (state.readBasis != null) LabelPanelReadout(nutrients = state.form.nutrients)
                    },
                    saveMyFood = state.saveMyFood,
                    onSaveMyFoodChange = { state.saveMyFood = it },
                    // The diary's day, not today — a packet read while reviewing Tuesday belongs to
                    // Tuesday, the rule every door off the diary follows.
                    onLogEntry = {
                        viewModel.handleEvent(
                            LabelScanEvent.OnLogEntry(
                                entry = state.form.toFoodEntry(dateEpochDay),
                                keepAsFood = state.form.toSuggestion()
                                    .takeIf { state.saveMyFood && state.form.isSaveableFood() },
                            ),
                        )
                    },
                    // Asks the same question back asks, rather than throwing the edits away.
                    onDiscard = { if (state.isDirty) state.pendingDiscard = { onExit() } else onExit() },
                )

                // Two dead ends, two different things to say. "There is no panel in that photo" is
                // the user's aim; "that didn't work" is ours. Both offer the same way forward,
                // because typing it in is the thing the label was saving them from.
                LabelFlow.Unreadable -> DeadEnd(
                    heading = stringResource(R.string.food_label_unreadable),
                    body = stringResource(R.string.food_label_unreadable_body),
                    onEnterManually = state::startManualEntry,
                    onRetake = state::recapture,
                )

                LabelFlow.Retry -> DeadEnd(
                    heading = stringResource(R.string.food_label_failed),
                    body = stringResource(R.string.food_label_failed_body),
                    onEnterManually = state::startManualEntry,
                    onRetake = state::recapture,
                )

                LabelFlow.Offline -> DeadEnd(
                    heading = stringResource(R.string.food_no_connection),
                    body = if (retriedWhileOffline) {
                        stringResource(R.string.food_label_offline_retry)
                    } else {
                        stringResource(R.string.food_label_offline)
                    },
                    onEnterManually = state::startManualEntry,
                    retakeLabel = stringResource(R.string.food_try_again),
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
                )

                // Once the prompt is spent, launching it again does nothing at all and the screen
                // becomes a dead end — Settings is the only door left.
                LabelFlow.PermissionDenied -> CameraPermissionScreen(
                    settingsOnly = context.permissionPermanentlyDenied(Manifest.permission.CAMERA),
                    grantBody = stringResource(R.string.food_label_permission_grant),
                    settingsBody = stringResource(R.string.food_label_permission_settings),
                    onGrant = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                    onOpenSettings = { context.openAppSettings() },
                    onBack = onExit,
                )
            }

            state.pendingDiscard?.let { discard ->
                DiscardConfirmDialog(
                    title = stringResource(R.string.food_label_discard_title),
                    body = stringResource(R.string.food_unsaved_edits),
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

/**
 * The three ways this flow stops short, drawn the same way.
 *
 * **Typing it in leads, and retaking is second.** The opposite of the photo flow's retry screen,
 * deliberately: a plate that failed to analyse is worth another shot from a better angle, but a
 * panel that would not read has usually already had the good angle, and the packet is still in the
 * user's hand. The label was saving them a typing job, not replacing one.
 */
@Composable
private fun DeadEnd(
    heading: String,
    body: String,
    onEnterManually: () -> Unit,
    onRetake: () -> Unit,
    retakeLabel: String = stringResource(R.string.food_label_retake),
) {
    FullScreenState(
        icon = { MascotAvatar(state = MascotState.Sleepy, size = 64.dp) },
        heading = heading,
        body = body,
        actions = {
            PrimaryButton(
                label = stringResource(R.string.food_label_enter_by_hand),
                onClick = onEnterManually,
                modifier = Modifier.fillMaxWidth(),
            )
            SecondaryButton(
                label = retakeLabel,
                onClick = onRetake,
                modifier = Modifier.fillMaxWidth(),
            )
        },
    )
}

/**
 * What the figures on the confirmation are figures *for*, in the panel's own terms.
 *
 * Null on a form nobody seeded, which hands the portion control back its own manual instruction.
 * Otherwise it names the basis the reading carried, because "Database values are per 100 g" is
 * wrong twice over here: a packet is not a database, and half of them declare a serving instead.
 */
@Composable
private fun labelCaveat(basis: LabelBasis?): String? = when (basis) {
    null -> null
    LabelBasis.Per100g -> stringResource(R.string.food_label_caveat_per_100g)
    LabelBasis.PerServing -> stringResource(R.string.food_label_caveat_per_serving)
}

/**
 * The one path into [LabelFlow.Reading], whether the photo comes off the sensor or out of the
 * gallery — `PhotoCaptureScreen.startAnalysis`'s shape and its reasoning. The offline check runs
 * before the photo is loaded, because reading is the online part and there is no point decoding
 * first. A null photo is one that never arrived (the picker handed back something undecodable, the
 * shutter found the sensor held by another app); either way the camera stays up.
 */
private fun startRead(
    viewModel: LabelScanViewModel,
    scope: CoroutineScope,
    state: LabelScanScreenState,
    loadPhoto: suspend () -> Bitmap?,
) {
    if (!viewModel.isOnline()) {
        state.flow = LabelFlow.Offline
        return
    }
    scope.launch {
        val photo = loadPhoto() ?: return@launch
        state.flow = LabelFlow.Reading
        viewModel.handleEvent(LabelScanEvent.OnPhotoCaptured(photo))
    }
}
