package ph.mart.healthapp.feature.progress.ui.shared

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Picture
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.drawscope.draw
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * The capture-then-share pair behind every image FitPulse hands to the chooser — the weekly recap
 * and the progress-photo strip. It lives in `ui/shared/` rather than either flow's `components/`
 * because two flows draw it, and in `:feature:progress` rather than `:core:designsystem` because
 * no other feature shares a picture.
 *
 * A captured layer is transparent wherever nothing painted, so every caller puts an opaque
 * background inside the captured subtree.
 */
fun Modifier.captureToPicture(picture: Picture): Modifier = drawWithCache {
    // Redirect this subtree's draw into a Picture and then play it back into the real canvas:
    // the API-24-safe capture. GraphicsLayer.toImageBitmap() is the shorter call but only pays
    // off above the app's minSdk.
    val width = size.width.toInt()
    val height = size.height.toInt()
    onDrawWithContent {
        val pictureCanvas = Canvas(picture.beginRecording(width, height))
        draw(this, layoutDirection, pictureCanvas, size) { this@onDrawWithContent.drawContent() }
        picture.endRecording()
        drawIntoCanvas { it.nativeCanvas.drawPicture(picture) }
    }
}

/**
 * One file per [fileName], overwritten: the last image shared is the only one worth keeping, and a
 * fixed name means nothing accumulates in the cache. The grant is read-only and scoped to
 * `cacheDir/share` by `@xml/file_paths` — nothing in `filesDir` (the progress photos, the
 * database) is reachable through the provider.
 */
suspend fun sharePng(context: Context, picture: Picture, fileName: String) {
    val uri = withContext(Dispatchers.IO) {
        // Software bitmap on every API: Bitmap.createBitmap(picture) is shorter above API 28 but
        // yields a hardware bitmap, and compressing one of those is its own compatibility story.
        val bitmap = Bitmap.createBitmap(picture.width, picture.height, Bitmap.Config.ARGB_8888)
        android.graphics.Canvas(bitmap).drawPicture(picture)
        val dir = File(context.cacheDir, "share").apply { mkdirs() }
        val file = File(dir, fileName)
        FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(send, null))
}
