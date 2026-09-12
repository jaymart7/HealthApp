package ph.mart.healthapp.core.designsystem.component

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Picture
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.core.content.ContextCompat
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
 * Software bitmap on every API: `Bitmap.createBitmap(picture)` is shorter above API 28 but yields a
 * hardware bitmap, and compressing one of those is its own compatibility story.
 */
private fun Picture.toSoftwareBitmap(): Bitmap {
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    android.graphics.Canvas(bitmap).drawPicture(this)
    return bitmap
}

/** True on API 29 and up, where MediaStore owns the write and no permission is asked for. */
private val ScopedStorage: Boolean get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

/** The one permission [savePng] needs, and only below API 29. */
internal const val LEGACY_SAVE_PERMISSION = android.Manifest.permission.WRITE_EXTERNAL_STORAGE

/**
 * True when [savePng] can run without asking — always on API 29+, and below it only once
 * `WRITE_EXTERNAL_STORAGE` has been granted.
 */
internal fun canSaveWithoutAsking(context: Context): Boolean = ScopedStorage ||
    ContextCompat.checkSelfPermission(context, LEGACY_SAVE_PERMISSION) == PackageManager.PERMISSION_GRANTED

/**
 * The same PNG [sharePng] hands the chooser, written into the device's own gallery under
 * `Pictures/FitPulse` — the copy that survives the cache being cleared.
 *
 * Returns false rather than throwing: a full disk, a revoked permission and a provider that
 * refuses the insert all look the same from here, and none of them is worth taking the sheet down
 * over. `IS_PENDING` keeps the half-written file out of every gallery until the bytes are there;
 * below API 29 there is no pending flag, which is the API the permission exists for.
 */
suspend fun savePng(context: Context, picture: Picture, fileName: String): Boolean =
    withContext(Dispatchers.IO) {
        runCatching {
            val bitmap = picture.toSoftwareBitmap()
            val resolver = context.contentResolver
            val pending = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                if (ScopedStorage) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/FitPulse")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, pending)
                ?: return@runCatching false
            resolver.openOutputStream(uri)?.use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
                ?: return@runCatching false
            if (ScopedStorage) {
                val done = ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }
                resolver.update(uri, done, null, null)
            }
            true
        }.getOrDefault(false)
    }

/**
 * One file per [fileName], overwritten: the last image shared is the only one worth keeping, and a
 * fixed name means nothing accumulates in the cache. The grant is read-only and scoped to
 * `cacheDir/share` by `@xml/file_paths` — nothing in `filesDir` (the progress photos, the
 * database) is reachable through the provider.
 */
suspend fun sharePng(context: Context, picture: Picture, fileName: String) {
    val uri = withContext(Dispatchers.IO) {
        val bitmap = picture.toSoftwareBitmap()
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

    // Zero until the sheet has drawn a frame — a tap that fast would otherwise write an empty file.
    fun save() {
        scope.launch { if (picture.width > 0 && savePng(context, picture, fileName)) onDismiss() }
    }
    // Below API 29 the gallery write needs asking for; at 29 and up the launcher is never reached.
    val permissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) save()
        }

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
                        // A tall card can exhaust the heap on the intermediate bitmap, a full disk
                        // fails the write, and a device with nothing to receive an image has no
                        // chooser to open. The sheet simply stays put — the same refusal-to-crash
                        // `HealthConnectionScreen` makes around its own intent.
                        runCatching { sharePng(context, picture, fileName) }.onSuccess { onDismiss() }
                    }
                }
            },
            modifier = Modifier.padding(top = 16.dp),
        )
        // The chooser is a hand-off; this is the copy that stays. Same picture, same failure
        // policy — the sheet stays put rather than announcing what went wrong.
        TextButton(
            label = stringResource(R.string.ds_share_save),
            onClick = {
                if (canSaveWithoutAsking(context)) save() else permissionLauncher.launch(LEGACY_SAVE_PERMISSION)
            },
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
