package ph.mart.healthapp.core.designsystem.component

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Every photo file in the app is decoded through here — the progress grid's tiles, the comparison
 * slider, the timelapse player, and the meal photos on a diary row and in their gallery. It
 * downsamples because it has to: a full-frame JPEG is several megabytes decoded, and a grid holding
 * a year of them (or a timelapse cycling one after another) has no business keeping any of them at
 * capture resolution.
 *
 * [maxWidthPx] is what the caller will actually draw into. `inSampleSize` only halves, so the
 * result is the smallest power-of-two reduction still at least that wide — never narrower than
 * asked for, so nothing is drawn upscaled.
 *
 * It lives in the design system rather than beside the progress grid it was written for because two
 * features now draw stored photos, and a second decoder is a second downsampling rule to keep in
 * step.
 */
@Composable
fun rememberBitmapFromFile(path: String, maxWidthPx: Int = FULL_FRAME_PX): ImageBitmap? {
    val state = produceState<ImageBitmap?>(initialValue = null, key1 = path, key2 = maxWidthPx) {
        value = withContext(Dispatchers.IO) { decodeSampled(path, maxWidthPx)?.asImageBitmap() }
    }
    return state.value
}

/** A photo drawn at the full width of a phone screen. */
const val FULL_FRAME_PX = 1080

/** A photo drawn as one cell of the three-column grid. */
const val GRID_TILE_PX = 360

/** A photo drawn as a diary row's 40dp tile, or the strip's 72dp one. */
const val THUMB_PX = 240

private fun decodeSampled(path: String, maxWidthPx: Int): Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(path, bounds)
    if (bounds.outWidth <= 0) return null
    var sample = 1
    while (bounds.outWidth / (sample * 2) >= maxWidthPx) sample *= 2
    return BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sample })
}
