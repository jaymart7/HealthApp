package ph.mart.healthapp.core.designsystem.component

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Picture
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.drawscope.draw
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ph.mart.healthapp.core.designsystem.R
import ph.mart.healthapp.core.designsystem.theme.AppTheme

/**
 * The capture-then-share pair behind every image FitPulse hands to the chooser — the weekly recap,
 * the progress-photo strip and the food diary's day card.
 *
 * It lived in `:feature:progress/ui/shared/` while that was the only feature sharing a picture; the
 * diary's share is the second, and `:feature:*` modules never import each other.
 *
 * A captured layer is transparent wherever nothing painted, so every caller puts an opaque
 * background inside the captured subtree — which is what [ShareImageSheet] exists to stop each
 * caller having to remember.
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

/**
 * Preview-then-share: the sheet shows exactly the PNG that leaves the app, which is why the
 * branding can exist here without ever appearing on the screen the card came from.
 *
 * [content] is the card, rendered verbatim — every figure and every colour rule stays its own.
 * This adds only the opaque ground a captured layer needs and the footer that says which app drew
 * it. Capture records what was *drawn*, so [content] must be one card rather than a scrolling
 * column: capturing a scroll hands the chooser a screenshot clipped at the fold.
 *
 * No `NavigationEventHandler`: [AppBottomSheet] delegates to `ModalBottomSheet`, which already
 * takes back, and this is a leaf with no sub-level of its own.
 */
@Composable
fun ShareImageSheet(
    fileName: String,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val picture = remember { Picture() }

    AppBottomSheet(onDismiss = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .captureToPicture(picture)
                .background(MaterialTheme.colorScheme.surface)
                .padding(vertical = 8.dp),
        ) {
            content()
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 8.dp),
            ) {
                MascotAvatar(state = MascotState.Happy, size = 24.dp)
                Text(
                    text = stringResource(R.string.ds_share_brand),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        PrimaryButton(
            label = stringResource(R.string.ds_share),
            onClick = {
                scope.launch {
                    // Zero until the sheet has drawn a frame — a tap that fast would otherwise
                    // hand the chooser an empty file.
                    if (picture.width > 0) {
                        sharePng(context, picture, fileName)
                        onDismiss()
                    }
                }
            },
            modifier = Modifier.padding(top = 16.dp),
        )
    }
}

@PreviewLightDark
@Composable
private fun ShareImageSheetPreview() {
    AppTheme {
        ShareImageSheet(fileName = "fitpulse-preview.png", onDismiss = {}) {
            Text(
                text = stringResource(R.string.ds_share_brand),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
