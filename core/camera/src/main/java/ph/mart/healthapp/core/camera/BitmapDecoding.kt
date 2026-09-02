package ph.mart.healthapp.core.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.InputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * What a photo is decoded down to, on its long edge. Nothing downstream wants more: the
 * recognition request re-encodes it, the analyzing screen draws it behind a scrim, and the
 * confirmation screen shows it at 64dp. Decoding the source's full frame instead would allocate
 * roughly 190MB for a 48MP image — and then a second copy of it to rotate — which is an
 * OutOfMemoryError on the small devices minSdk 24 still admits, not a slow frame. A picture picked
 * from the gallery is the same hazard as one off the sensor, which is why both go through here.
 */
private const val MAX_CAPTURE_EDGE = 1280

/** Decodes a picked image at the same size and orientation a capture gets. Null when the stream
 * can't be opened or holds nothing decodable — a picker can hand back a Uri that resolves to
 * neither. */
suspend fun decodeRotatedBitmap(context: Context, uri: Uri): Bitmap? =
    decodeRotatedBitmap { context.contentResolver.openInputStream(uri) }

internal suspend fun decodeRotatedBitmap(file: File): Bitmap? =
    decodeRotatedBitmap { file.inputStream() }

/** Three passes — bounds, pixels, orientation — so the stream is opened once per pass rather than
 * buffered whole. IO-dispatched: both callers arrive on the main thread. */
private suspend fun decodeRotatedBitmap(openStream: () -> InputStream?): Bitmap? = withContext(Dispatchers.IO) {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    openStream()?.use { BitmapFactory.decodeStream(it, null, bounds) }
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@withContext null

    val options = BitmapFactory.Options().apply {
        inSampleSize = sampleSizeFor(bounds.outWidth, bounds.outHeight)
    }
    val bitmap = openStream()?.use { BitmapFactory.decodeStream(it, null, options) }
        ?: return@withContext null

    val rotationDegrees = openStream()?.use { stream ->
        when (ExifInterface(stream).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90
            ExifInterface.ORIENTATION_ROTATE_180 -> 180
            ExifInterface.ORIENTATION_ROTATE_270 -> 270
            else -> 0
        }
    } ?: 0
    if (rotationDegrees == 0) return@withContext bitmap

    val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
    val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    // createBitmap can hand back the same instance when there is nothing to do; only recycle a
    // source that was genuinely replaced.
    if (rotated !== bitmap) bitmap.recycle()
    rotated
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
