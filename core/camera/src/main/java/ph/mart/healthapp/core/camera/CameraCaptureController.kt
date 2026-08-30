package ph.mart.healthapp.core.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
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
import androidx.exifinterface.media.ExifInterface
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Wraps [LifecycleCameraController] + [PreviewView] — the boring, well-trodden CameraX-in-Compose
 * path (capture-to-cache-file + [ExifInterface] rotation correction), not manual `ImageProxy`
 * plane decoding.
 */
interface CameraCaptureController {
    @Composable
    fun Preview(modifier: Modifier)
    suspend fun capture(): Bitmap
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

            override suspend fun capture(): Bitmap = takePicture(context, controller)
        }
    }
}

private suspend fun takePicture(context: Context, controller: LifecycleCameraController): Bitmap {
    val file = File(context.cacheDir, "capture_${System.currentTimeMillis()}.jpg")
    val outputOptions = ImageCapture.OutputFileOptions.Builder(file).build()
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
    return decodeRotatedBitmap(file)
}

/**
 * What the capture is decoded down to, on its long edge. Nothing downstream wants more: the
 * recognition request re-encodes it, the analyzing screen draws it behind a scrim, and the
 * confirmation screen shows it at 64dp. Decoding the sensor's full frame instead would allocate
 * roughly 190MB for a 48MP camera — and then a second copy of it to rotate — which is an
 * OutOfMemoryError on the small devices minSdk 24 still admits, not a slow frame.
 */
private const val MAX_CAPTURE_EDGE = 1280

private fun decodeRotatedBitmap(file: File): Bitmap {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(file.path, bounds)
    val options = BitmapFactory.Options().apply {
        inSampleSize = sampleSizeFor(bounds.outWidth, bounds.outHeight)
    }
    val bitmap = BitmapFactory.decodeFile(file.path, options)
        ?: error("Could not decode capture at ${file.path}")

    val rotationDegrees = when (
        ExifInterface(file.path).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
    ) {
        ExifInterface.ORIENTATION_ROTATE_90 -> 90
        ExifInterface.ORIENTATION_ROTATE_180 -> 180
        ExifInterface.ORIENTATION_ROTATE_270 -> 270
        else -> 0
    }
    if (rotationDegrees == 0) return bitmap

    val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
    val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    // createBitmap can hand back the same instance when there is nothing to do; only recycle a
    // source that was genuinely replaced.
    if (rotated !== bitmap) bitmap.recycle()
    return rotated
}

/** Largest power-of-two subsample that still leaves the long edge at or above [MAX_CAPTURE_EDGE] —
 * the sizing rule `inSampleSize` is documented to round to anyway. */
private fun sampleSizeFor(width: Int, height: Int): Int {
    var sample = 1
    var longEdge = maxOf(width, height)
    while (longEdge / 2 >= MAX_CAPTURE_EDGE) {
        longEdge /= 2
        sample *= 2
    }
    return sample
}
