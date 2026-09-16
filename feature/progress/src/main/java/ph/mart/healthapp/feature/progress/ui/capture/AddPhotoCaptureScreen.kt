package ph.mart.healthapp.feature.progress.ui.capture

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ph.mart.healthapp.core.camera.decodeRotatedBitmap
import ph.mart.healthapp.core.camera.openAppSettings
import ph.mart.healthapp.core.camera.permissionPermanentlyDenied
import ph.mart.healthapp.core.camera.rememberCameraCaptureController
import ph.mart.healthapp.core.designsystem.component.CameraPermissionScreen
import ph.mart.healthapp.core.designsystem.component.SecondaryButton
import ph.mart.healthapp.core.designsystem.icon.AppIcons
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.progress.R

/** The staging files this screen writes. One prefix, because the sweep below matches on it. */
private const val STAGED_PREFIX = "progress_capture_"

/**
 * The viewfinder half of adding a body progress shot — a route of its own, ending the moment there
 * is a picture: [onPreview] carries its path and the preview route takes over.
 *
 * It opens on the camera rather than on a chooser, which is what the bottom sheet this used to be
 * could not do — a viewfinder wants the whole window, and a sheet is the one container that cannot
 * give it one. The gallery is a button on the viewfinder instead of a peer of it.
 *
 * No ViewModel, deliberately: there is nothing here to observe and nothing to persist — the save
 * belongs to the preview, which is where the form is. Don't "finish the quartet" by adding an
 * Orbit host with an empty container.
 *
 * No back handler either, for the same reason: back off this screen leaves the flow, which is
 * exactly what popping the entry already does.
 */
@Composable
fun AddPhotoCaptureScreen(onPreview: (String) -> Unit, onExitFlow: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val controller = rememberCameraCaptureController()
    val state = rememberAddPhotoCaptureState()

    val permissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            state.hasCameraPermission = granted
            state.permissionRefused = !granted
        }
    LaunchedEffect(Unit) {
        if (!state.hasCameraPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) scope.launch {
            // Subsampled and EXIF-rotated by the same decoder the capture path uses — a full-size
            // 48MP gallery JPEG is an OutOfMemoryError, not a slow frame.
            val bitmap = decodeRotatedBitmap(context, uri) ?: return@launch
            stageCapture(context, bitmap)?.let(onPreview)
        }
    }
    val onChooseGallery = {
        galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    Surface(color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxSize()) {
        if (state.permissionRefused) {
            // AppScaffold hands this route the whole window, insets included — which the viewfinder
            // wants and an ordinary explanation screen does not.
            Box(modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
                CameraPermissionScreen(
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
        } else {
            Viewfinder(
                onClose = onExitFlow,
                // Null is a shot that didn't happen — see [CameraCaptureController.capture].
                // The camera stays up, which is the only useful thing to do about it, and a
                // staging file that wouldn't write gets the same answer.
                onCapture = {
                    scope.launch {
                        val bitmap = controller.capture() ?: return@launch
                        stageCapture(context, bitmap)?.let(onPreview)
                    }
                },
                onChooseGallery = onChooseGallery,
                cameraPreview = { controller.Preview(modifier = Modifier.fillMaxSize()) },
            )
        }
    }
}

/**
 * The shot, on disk, so it can cross a route boundary — a [Bitmap] cannot ride in a `NavKey`.
 * `cacheDir` is the right home for a handover: the preview's save writes its own copy into
 * `filesDir`, exactly as `CameraCaptureController` already treats its own capture file.
 *
 * ponytail: the sweep is what bounds this at one file. Without it a JPEG per shot accumulates
 * until Android reclaims under storage pressure; per-file deletion on save, retake and cancel is
 * four call sites to keep in step for the same result.
 */
private suspend fun stageCapture(context: Context, bitmap: Bitmap): String? = withContext(Dispatchers.IO) {
    context.cacheDir.listFiles { file -> file.name.startsWith(STAGED_PREFIX) }?.forEach { it.delete() }
    val file = File(context.cacheDir, "$STAGED_PREFIX${System.currentTimeMillis()}.jpg")
    runCatching {
        FileOutputStream(file).use { out -> bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out) }
        file.absolutePath
    }.getOrNull()
}

/**
 * Full-bleed camera chrome. Always black/white regardless of app theme, for
 * [ph.mart.healthapp.feature.food.ui.photo.components.CaptureScreen]'s reason: overlay controls over a
 * live feed of arbitrary brightness have to stay legible, which no theme token can promise.
 */
@Composable
private fun Viewfinder(
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

@PreviewLightDark
@Composable
private fun ViewfinderPreview() {
    AppTheme { Viewfinder(onClose = {}, onCapture = {}, onChooseGallery = {}) }
}
