package ph.mart.healthapp.core.camera

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * The scanning twin of [CameraCaptureController]: same [LifecycleCameraController] + [PreviewView]
 * path, but bound to `IMAGE_ANALYSIS` instead of capture, with ML Kit reading each frame.
 */
interface BarcodeScanController {
    @Composable
    fun Preview(modifier: Modifier)
}

/** [onBarcode] fires **once** per controller instance — the first successful decode wins, and the
 * analyzer stops delivering after it. The caller navigates away on that callback, so there is no
 * "scan the next one" case to serve. */
@Composable
fun rememberBarcodeScanController(onBarcode: (String) -> Unit): BarcodeScanController {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnBarcode by rememberUpdatedState(onBarcode)

    val controller = remember {
        LifecycleCameraController(context).apply {
            cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            setEnabledUseCases(CameraController.IMAGE_ANALYSIS)
        }
    }

    DisposableEffect(controller, lifecycleOwner) {
        val scanner = retailBarcodeScanner()
        val delivered = AtomicBoolean(false)
        controller.setImageAnalysisAnalyzer(ContextCompat.getMainExecutor(context)) { proxy ->
            analyze(scanner, proxy, delivered) { currentOnBarcode(it) }
        }
        controller.bindToLifecycle(lifecycleOwner)
        onDispose {
            controller.clearImageAnalysisAnalyzer()
            controller.unbind()
            scanner.close()
        }
    }

    return remember(controller) {
        object : BarcodeScanController {
            @Composable
            override fun Preview(modifier: Modifier) {
                AndroidView(
                    factory = { ctx -> PreviewView(ctx).apply { this.controller = controller } },
                    modifier = modifier,
                )
            }
        }
    }
}

/**
 * Reads a barcode out of an already-taken picture — the gallery door on the scanning viewfinder.
 * Null when there is no readable retail barcode in it, which is an ordinary outcome (a photo of a
 * plate, a blurry pack) rather than a failure. [InputImage.fromFilePath] applies the image's own
 * EXIF rotation, so this needs none of [decodeRotatedBitmap]'s work.
 */
suspend fun scanBarcode(context: Context, uri: Uri): String? {
    val scanner = retailBarcodeScanner()
    return try {
        suspendCancellableCoroutine { continuation ->
            scanner.process(InputImage.fromFilePath(context, uri))
                .addOnSuccessListener { barcodes ->
                    continuation.resume(barcodes.firstNotNullOfOrNull { it.rawValue })
                }
                .addOnFailureListener { continuation.resume(null) }
        }
    } catch (e: IOException) {
        // The picker can hand back a Uri whose stream is gone by the time it is opened.
        null
    } finally {
        scanner.close()
    }
}

/** The four retail product formats, shared by the live analyzer and the one-shot above — a QR code
 * on a coffee bag is not a food barcode, and narrowing the format set is also what keeps the
 * decoder fast. */
private fun retailBarcodeScanner(): BarcodeScanner = BarcodeScanning.getClient(
    BarcodeScannerOptions.Builder()
        .setBarcodeFormats(
            Barcode.FORMAT_EAN_13,
            Barcode.FORMAT_EAN_8,
            Barcode.FORMAT_UPC_A,
            Barcode.FORMAT_UPC_E,
        )
        .build(),
)

/** Every frame must be closed exactly once or the analyzer starves, hence the
 * `addOnCompleteListener` rather than closing inside the success branch. */
@OptIn(ExperimentalGetImage::class)
private fun analyze(
    scanner: BarcodeScanner,
    proxy: ImageProxy,
    delivered: AtomicBoolean,
    onBarcode: (String) -> Unit,
) {
    val image = proxy.image
    if (image == null) {
        proxy.close()
        return
    }
    scanner.process(InputImage.fromMediaImage(image, proxy.imageInfo.rotationDegrees))
        .addOnSuccessListener { barcodes ->
            val code = barcodes.firstNotNullOfOrNull { it.rawValue }
            if (code != null && delivered.compareAndSet(false, true)) onBarcode(code)
        }
        .addOnCompleteListener { proxy.close() }
}
