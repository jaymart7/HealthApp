package ph.mart.healthapp.core.camera

import android.content.Context
import android.graphics.Bitmap
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Wraps [LifecycleCameraController] + [PreviewView] — the boring, well-trodden CameraX-in-Compose
 * path (capture-to-cache-file + [decodeRotatedBitmap] rotation correction), not manual `ImageProxy`
 * plane decoding.
 */
interface CameraCaptureController {
    @Composable
    fun Preview(modifier: Modifier)

    /**
     * Null when the shot didn't happen — the sensor was held by another app, the capture failed,
     * there was no room to write it, or what came back wouldn't decode. All four are ordinary on a
     * real phone, and none of them is worth taking the process down for: both call sites already
     * have a null path, because a gallery pick can hand back something undecodable too.
     */
    suspend fun capture(): Bitmap?
}

@Composable
fun rememberCameraCaptureController(): CameraCaptureController {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val controller = remember {
        LifecycleCameraController(context).apply {
            cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
        }
    }
    remember(controller, lifecycleOwner) { controller.bindToLifecycle(lifecycleOwner) }

    return remember(controller) {
        object : CameraCaptureController {
            @Composable
            override fun Preview(modifier: Modifier) {
                AndroidView(
                    factory = { ctx -> PreviewView(ctx).apply { this.controller = controller } },
                    modifier = modifier,
                )
            }

            override suspend fun capture(): Bitmap? = takePicture(context, controller)
        }
    }
}

/**
 * The capture file is a staging post, not storage: [decodeRotatedBitmap] downsamples to 1280px and
 * whoever keeps the plate writes its own copy into `filesDir`. Deleting it in a `finally` is what
 * stops a full-resolution JPEG per shot accumulating in the cache — three meals a day is on the
 * order of a gigabyte a year sitting there until Android reclaims it under storage pressure.
 *
 * Failures return null rather than throwing. [ImageCaptureException] is what CameraX reports when
 * another app holds the sensor, when the capture itself fails and when there is no room to write;
 * `takePicture` also throws [IllegalStateException] outright if the shutter is tapped before the
 * controller has finished binding. None of the three is exceptional enough to take the process down
 * — and both call sites launch this from a `rememberCoroutineScope`, where anything thrown reaches
 * the default handler and does exactly that.
 *
 * [CancellationException] is rethrown rather than swallowed: leaving the screen mid-capture must
 * still cancel the caller, and a blanket catch around a `suspendCancellableCoroutine` is precisely
 * where that gets lost — the same trap `CoachRepositoryImpl.send` documents.
 */
private suspend fun takePicture(context: Context, controller: LifecycleCameraController): Bitmap? {
    val file = File(context.cacheDir, "capture_${System.currentTimeMillis()}.jpg")
    val outputOptions = ImageCapture.OutputFileOptions.Builder(file).build()
    return try {
        suspendCancellableCoroutine { continuation ->
            controller.takePicture(
                outputOptions,
                ContextCompat.getMainExecutor(context),
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                        continuation.resume(Unit)
                    }

                    override fun onError(exception: ImageCaptureException) {
                        continuation.resumeWithException(exception)
                    }
                },
            )
        }
        decodeRotatedBitmap(file)
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        null
    } finally {
        file.delete()
    }
}
