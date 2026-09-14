package ph.mart.healthapp.feature.progress.ui.addphoto

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect
import ph.mart.healthapp.core.camera.decodeRotatedBitmap
import ph.mart.healthapp.core.camera.openAppSettings
import ph.mart.healthapp.core.camera.permissionPermanentlyDenied
import ph.mart.healthapp.core.camera.rememberCameraCaptureController
import ph.mart.healthapp.core.data.profile.UnitSystem
import ph.mart.healthapp.core.data.profile.displayUnitToKg
import ph.mart.healthapp.core.data.profile.kgToDisplayUnit
import ph.mart.healthapp.core.data.profile.weightUnitLabel
import ph.mart.healthapp.core.designsystem.component.AppTopBar
import ph.mart.healthapp.core.designsystem.component.CameraPermissionScreen
import ph.mart.healthapp.core.designsystem.component.NumericStepperField
import ph.mart.healthapp.core.designsystem.component.PrimaryButton
import ph.mart.healthapp.core.designsystem.component.SecondaryButton
import ph.mart.healthapp.core.designsystem.component.SheetDatePicker
import ph.mart.healthapp.core.designsystem.icon.AppIcons
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.progress.R

/** The 3:4 the whole set is displayed at, from the grid tile to the timelapse frame. */
private const val PHOTO_ASPECT = 3f / 4f

/**
 * A body progress shot, from the viewfinder to the saved row — one route, three steps, the shape
 * [ph.mart.healthapp.feature.food.ui.photo.PhotoCaptureScreen] already established: one always-mounted
 * [NavigationBackHandler] dispatching on the current step rather than one behavior for all of them.
 *
 * It opens on the camera rather than on a chooser, which is what the bottom sheet this used to be
 * could not do — a viewfinder wants the whole window, and a sheet is the one container that cannot
 * give it one. The gallery is a button on the viewfinder instead of a peer of it.
 */
@Composable
fun AddPhotoScreen(onExitFlow: () -> Unit, viewModel: AddPhotoViewModel = koinViewModel()) {
    val uiState by viewModel.collectAsState()
    val state = rememberAddPhotoState()
    viewModel.collectSideEffect { effect ->
        when (effect) {
            AddPhotoSideEffect.Saved -> onExitFlow()
        }
    }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val controller = rememberCameraCaptureController()

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
            state.step = if (granted) AddPhotoStep.Capture else AddPhotoStep.PermissionDenied
        }
    LaunchedEffect(Unit) {
        if (!hasCameraPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) scope.launch {
            // Subsampled and EXIF-rotated by the same decoder the capture path uses — a full-size
            // 48MP gallery JPEG is an OutOfMemoryError, not a slow frame.
            state.photo = decodeRotatedBitmap(context, uri) ?: return@launch
            state.step = AddPhotoStep.Preview
        }
    }
    val onChooseGallery = {
        galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    val navigationState = rememberNavigationEventState(currentInfo = NavigationEventInfo.None)
    NavigationBackHandler(
        state = navigationState,
        onBackCompleted = {
            when (state.step) {
                // Back off the preview is a retake, one step, not a way out of the flow — and it
                // lands where the camera actually is, which with the permission refused is the
                // screen explaining that rather than a black rectangle.
                AddPhotoStep.Preview -> state.step =
                    if (hasCameraPermission) AddPhotoStep.Capture else AddPhotoStep.PermissionDenied
                AddPhotoStep.Capture, AddPhotoStep.PermissionDenied -> onExitFlow()
            }
        },
    )

    Surface(color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxSize()) {
        // AppScaffold hands this route the whole window, insets included. The viewfinder wants
        // exactly that and insets its own chrome; the other two steps are ordinary content.
        // safeDrawing unions the IME, which is where the weight stepper's keyboard avoidance
        // comes from.
        val insets = if (state.step == AddPhotoStep.Capture) Modifier else Modifier.safeDrawingPadding()
        Box(modifier = Modifier.fillMaxSize().then(insets)) {
            when (state.step) {
                AddPhotoStep.Capture -> AddPhotoCaptureScreen(
                    onClose = onExitFlow,
                    // Null is a shot that didn't happen — see [CameraCaptureController.capture].
                    // The camera stays up, which is the only useful thing to do about it.
                    onCapture = {
                        scope.launch {
                            controller.capture()?.let { bitmap ->
                                state.photo = bitmap
                                state.step = AddPhotoStep.Preview
                            }
                        }
                    },
                    onChooseGallery = onChooseGallery,
                    cameraPreview = { controller.Preview(modifier = Modifier.fillMaxSize()) },
                )

                AddPhotoStep.Preview -> state.photo?.let { photo ->
                    AddPhotoContent(
                        uiState = uiState,
                        state = state,
                        photo = photo,
                        onRetake = { state.step = AddPhotoStep.Capture },
                        onCancel = onExitFlow,
                        onEvent = viewModel::handleEvent,
                    )
                }

                AddPhotoStep.PermissionDenied -> CameraPermissionScreen(
                    // Once the prompt is spent, launching it again does nothing at all and the
                    // screen becomes a dead end — Settings is the only door left.
                    settingsOnly = context.permissionPermanentlyDenied(Manifest.permission.CAMERA),
                    grantBody = stringResource(R.string.progress_camera_permission_grant),
                    settingsBody = stringResource(R.string.progress_camera_permission_settings),
                    onGrant = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                    onOpenSettings = { context.openAppSettings() },
                    onBack = onExitFlow,
                    // A shot already in the gallery needs no camera, so a refusal here costs the
                    // live viewfinder and nothing else.
                    extraAction = {
                        SecondaryButton(
                            label = stringResource(R.string.progress_photo_gallery),
                            onClick = onChooseGallery,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    },
                )
            }
        }
    }
}

/**
 * Full-bleed camera chrome. Always black/white regardless of app theme, for
 * [ph.mart.healthapp.feature.food.ui.photo.components.CaptureScreen]'s reason: overlay controls over a
 * live feed of arbitrary brightness have to stay legible, which no theme token can promise.
 */
@Composable
private fun AddPhotoCaptureScreen(
    onClose: () -> Unit,
    onCapture: () -> Unit,
    onChooseGallery: () -> Unit,
    modifier: Modifier = Modifier,
    cameraPreview: @Composable () -> Unit = {},
) {
    Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
        cameraPreview()

        // Everything tappable sits inside the safe area; the preview stays full-bleed.
        Box(modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
                    .size(48.dp)
                    .background(Color.Black.copy(alpha = 0.45f), CircleShape),
            ) {
                Icon(
                    imageVector = AppIcons.Close,
                    contentDescription = stringResource(R.string.progress_close),
                    tint = Color.White,
                )
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp, vertical = 32.dp),
                contentAlignment = Alignment.Center,
            ) {
                IconButton(
                    onClick = onChooseGallery,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .size(48.dp)
                        .background(Color.Black.copy(alpha = 0.45f), CircleShape),
                ) {
                    Icon(
                        imageVector = AppIcons.Gallery,
                        contentDescription = stringResource(R.string.progress_photo_gallery),
                        tint = Color.White,
                    )
                }

                // An empty Surface is invisible to a screen reader, and this one is the whole
                // point of the screen. Resolved above the lambda, which cannot read a resource.
                val takePhoto = stringResource(R.string.progress_photo_take)
                Surface(
                    onClick = onCapture,
                    shape = CircleShape,
                    color = Color.White,
                    modifier = Modifier
                        .size(72.dp)
                        .semantics {
                            contentDescription = takePhoto
                            role = Role.Button
                        },
                ) {}
            }
        }
    }
}

@Composable
private fun AddPhotoContent(
    uiState: AddPhotoUiState,
    state: AddPhotoState,
    photo: Bitmap,
    onRetake: () -> Unit,
    onCancel: () -> Unit,
    onEvent: (AddPhotoEvent) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Zero insets: the root has already cleared the system bars for this step, and the
        // default would apply them a second time — PhotosScreen's reason, same shape.
        AppTopBar(
            title = stringResource(R.string.progress_photo_add),
            onBack = onRetake,
            windowInsets = WindowInsets(0),
        )
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
        ) {
            Image(
                bitmap = photo.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().aspectRatio(PHOTO_ASPECT).clip(RoundedCornerShape(12.dp)),
            )
            SheetDatePicker(
                showingCalendar = state.showingCalendar,
                onShowCalendar = { state.showingCalendar = true },
                onBackToFields = { state.showingCalendar = false },
                selectedDate = state.form.dateEpochDay,
                markedDates = uiState.photos.map { it.dateEpochDay }.toSet(),
                onSelectDate = { date ->
                    state.form = state.form.copy(dateEpochDay = date)
                    state.showingCalendar = false
                },
            ) {
                WeightField(
                    form = state.form,
                    unit = uiState.preferredUnit,
                    onFormChange = { state.form = it },
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
            if (!state.showingCalendar) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    SecondaryButton(label = stringResource(R.string.progress_cancel), onClick = onCancel, modifier = Modifier.weight(1f))
                    PrimaryButton(
                        label = stringResource(R.string.progress_save),
                        onClick = { onEvent(AddPhotoEvent.OnSave(photo, state.form)) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun WeightField(form: AddPhotoForm, unit: UnitSystem, onFormChange: (AddPhotoForm) -> Unit, modifier: Modifier = Modifier) {
    val step = 0.5
    NumericStepperField(
        label = stringResource(R.string.progress_photo_weight),
        value = form.weightKg?.let { formatWeight(it.kgToDisplayUnit(unit)) } ?: stringResource(R.string.progress_none),
        unitSuffix = unit.weightUnitLabel(),
        onIncrement = { onFormChange(form.copy(weightKg = ((form.weightKg ?: 0.0) + step.displayUnitToKg(unit)))) },
        onDecrement = { onFormChange(form.copy(weightKg = (((form.weightKg ?: step) - step.displayUnitToKg(unit)).coerceAtLeast(20.0)))) },
        modifier = modifier,
    )
}

private fun formatWeight(value: Double): String =
    if (value == value.toInt().toDouble()) value.toInt().toString() else "%.1f".format(value)

@PreviewLightDark
@Composable
private fun AddPhotoCapturePreview() {
    AppTheme { AddPhotoCaptureScreen(onClose = {}, onCapture = {}, onChooseGallery = {}) }
}

@PreviewLightDark
@Composable
private fun AddPhotoContentPreview() {
    AppTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            AddPhotoContent(
                uiState = AddPhotoUiState(),
                state = AddPhotoState(step = AddPhotoStep.Preview),
                photo = previewPhoto(),
                onRetake = {},
                onCancel = {},
                onEvent = {},
            )
        }
    }
}

/** A stand-in for the shot, so the preview shows the frame the fields sit under. */
private fun previewPhoto(): Bitmap =
    Bitmap.createBitmap(3, 4, Bitmap.Config.ARGB_8888).apply { eraseColor(android.graphics.Color.DKGRAY) }
