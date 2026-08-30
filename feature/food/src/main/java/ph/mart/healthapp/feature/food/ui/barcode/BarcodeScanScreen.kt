package ph.mart.healthapp.feature.food.ui.barcode

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import org.koin.androidx.compose.koinViewModel
import org.orbitmvi.orbit.compose.collectSideEffect
import ph.mart.healthapp.core.camera.rememberBarcodeScanController
import ph.mart.healthapp.core.data.food.BarcodeLookupResult
import ph.mart.healthapp.core.designsystem.component.FullScreenState
import ph.mart.healthapp.core.designsystem.component.MascotAvatar
import ph.mart.healthapp.core.designsystem.component.MascotState
import ph.mart.healthapp.core.designsystem.component.PrimaryButton
import ph.mart.healthapp.core.designsystem.component.SecondaryButton
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.food.ui.barcode.components.ScanScreen
import ph.mart.healthapp.feature.food.ui.diary.toFoodEntry
import ph.mart.healthapp.feature.food.ui.photo.PhotoCaptureScreen
import ph.mart.healthapp.feature.food.ui.shared.components.ScanConfirmationScreen
import ph.mart.healthapp.feature.food.ui.shared.openAppSettings
import ph.mart.healthapp.feature.food.ui.shared.permissionPermanentlyDenied
import ph.mart.healthapp.feature.food.ui.shared.toFoodEntry

private const val FOUND_SUBTITLE = "Values are per 100 g — adjust the portion to match what you ate."
private const val MANUAL_SUBTITLE = "We didn't have that product, so this one's on you."

/**
 * The barcode flow, built to the same shape as [PhotoCaptureScreen]: one always-mounted
 * [NavigationBackHandler] that dispatches on the current [ScanFlow] instead of applying one
 * behavior to every state.
 */
@Composable
fun BarcodeScanScreen(
    dateEpochDay: Long,
    onExit: () -> Unit,
    viewModel: BarcodeScanViewModel = koinViewModel(),
) {
    val state = rememberBarcodeScanScreen()
    val context = LocalContext.current

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
            state.flow = if (granted) ScanFlow.Scanning else ScanFlow.PermissionDenied
        }
    LaunchedEffect(Unit) {
        // The lookup is the online part, not the scan — but a scan the app can't resolve is a dead
        // end, so the offline state is shown up front rather than after the user aims the camera.
        if (!viewModel.isOnline()) {
            state.flow = ScanFlow.Offline
        } else if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    viewModel.collectSideEffect { effect ->
        when (effect) {
            is BarcodeScanSideEffect.LookupFinished -> when (val result = effect.result) {
                is BarcodeLookupResult.Found -> state.applyProduct(result.product)
                BarcodeLookupResult.NotFound -> state.flow = ScanFlow.NotFound
                BarcodeLookupResult.Failed -> state.flow =
                    if (viewModel.isOnline()) ScanFlow.NotFound else ScanFlow.Offline
            }

            BarcodeScanSideEffect.EntryLogged -> onExit()
        }
    }

    var retriedWhileOffline by remember { mutableStateOf(false) }

    val backHandlerState = rememberNavigationEventState(currentInfo = NavigationEventInfo.None)
    NavigationBackHandler(
        state = backHandlerState,
        onBackCompleted = {
            when (state.flow) {
                ScanFlow.Scanning -> onExit()
                ScanFlow.LookingUp -> {
                    viewModel.handleEvent(BarcodeScanEvent.OnCancelLookup)
                    state.rescan()
                }

                ScanFlow.Confirmation -> if (state.isDirty) {
                    state.pendingDiscard = { state.rescan() }
                } else {
                    state.rescan()
                }

                ScanFlow.NotFound, ScanFlow.Offline, ScanFlow.PermissionDenied -> onExit()
            }
        },
    )

    Surface(color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxSize()) {
        // Scanning is a full-bleed camera surface that insets its own chrome; every other state is
        // ordinary content, and safeDrawing unions the IME so the editable rows clear the keyboard.
        val insets = if (state.flow == ScanFlow.Scanning) Modifier else Modifier.safeDrawingPadding()
        Box(modifier = Modifier.fillMaxSize().then(insets)) {
            when (state.flow) {
                ScanFlow.Scanning -> if (hasCameraPermission) {
                    val scanController = rememberBarcodeScanController { barcode ->
                        state.flow = ScanFlow.LookingUp
                        viewModel.handleEvent(BarcodeScanEvent.OnBarcodeScanned(barcode))
                    }
                    ScanScreen(
                        onClose = onExit,
                        cameraPreview = { scanController.Preview(modifier = Modifier.fillMaxSize()) },
                    )
                }

                ScanFlow.LookingUp -> FullScreenState(
                    icon = { CircularProgressIndicator(color = MaterialTheme.colorScheme.primary) },
                    heading = "Looking that up…",
                    body = "Checking the product database for this barcode.",
                    actions = {
                        SecondaryButton(
                            label = "Cancel",
                            onClick = {
                                viewModel.handleEvent(BarcodeScanEvent.OnCancelLookup)
                                state.rescan()
                            },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    },
                )

                ScanFlow.Confirmation -> ScanConfirmationScreen(
                    form = state.form,
                    subtitle = if (state.originalForm.name.isBlank()) MANUAL_SUBTITLE else FOUND_SUBTITLE,
                    onFormChange = { state.form = it },
                    onMealTypeSelect = state::selectMealType,
                    // The diary's day, not today — a scan while reviewing Tuesday belongs to Tuesday.
                    onLogEntry = { viewModel.handleEvent(BarcodeScanEvent.OnLogEntry(state.form.toFoodEntry(dateEpochDay))) },
                    // Asks the same question back asks; it used to throw the edits away silently.
                    onDiscard = { if (state.isDirty) state.pendingDiscard = { onExit() } else onExit() },
                )

                ScanFlow.NotFound -> FullScreenState(
                    icon = { MascotAvatar(state = MascotState.Sleepy, size = 64.dp) },
                    heading = "We don't have that product",
                    body = "It isn't in the product database yet — you can still add it by hand, or scan a different item.",
                    actions = {
                        PrimaryButton(
                            label = "Add it manually",
                            onClick = state::startManualEntry,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        SecondaryButton(
                            label = "Scan again",
                            onClick = state::rescan,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    },
                )

                ScanFlow.Offline -> FullScreenState(
                    icon = { MascotAvatar(state = MascotState.Sleepy, size = 64.dp) },
                    heading = "No connection",
                    body = if (retriedWhileOffline) {
                        "Still nothing. A barcode lookup needs a connection — you can add the item by hand in the meantime."
                    } else {
                        "Looking up a barcode needs a connection. You can still log manually — everything else works offline."
                    },
                    actions = {
                        PrimaryButton(
                            label = "Log manually",
                            onClick = state::startManualEntry,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        SecondaryButton(
                            label = "Try again",
                            onClick = {
                                if (!viewModel.isOnline()) {
                                    retriedWhileOffline = true
                                } else {
                                    retriedWhileOffline = false
                                    if (hasCameraPermission) {
                                        state.rescan()
                                    } else {
                                        permissionLauncher.launch(Manifest.permission.CAMERA)
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    },
                )

                ScanFlow.PermissionDenied -> {
                    // Same dead end the photo flow had: a spent prompt never shows again, so
                    // "Grant access" was a button that could not work.
                    val settingsOnly = context.permissionPermanentlyDenied(Manifest.permission.CAMERA)
                    FullScreenState(
                        icon = { MascotAvatar(state = MascotState.Sleepy, size = 64.dp) },
                        heading = "Camera access needed",
                        body = if (settingsOnly) {
                            "Camera access is off for FitPulse. Turn it on in Settings to scan a barcode, or go back and log manually."
                        } else {
                            "Grant camera access to scan a barcode, or go back and log manually."
                        },
                        actions = {
                            PrimaryButton(
                                label = if (settingsOnly) "Open settings" else "Grant access",
                                onClick = {
                                    if (settingsOnly) {
                                        context.openAppSettings()
                                    } else {
                                        permissionLauncher.launch(Manifest.permission.CAMERA)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                            )
                            SecondaryButton(label = "Back", onClick = onExit, modifier = Modifier.fillMaxWidth())
                        },
                    )
                }
            }

            state.pendingDiscard?.let { discard ->
                DiscardScanDialog(
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

@Composable
private fun DiscardScanDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Discard this item?") },
        text = { Text("You've made edits that haven't been logged yet.") },
        confirmButton = { TextButton(onClick = onConfirm) { Text("Discard") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Keep editing") } },
    )
}

@PreviewLightDark
@Composable
private fun DiscardScanDialogPreview() {
    AppTheme {
        Box(
            modifier = Modifier
                .background(Color.Black.copy(alpha = 0.32f))
                .padding(24.dp),
        ) {
            DiscardScanDialog(onConfirm = {}, onDismiss = {})
        }
    }
}
