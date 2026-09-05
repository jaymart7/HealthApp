package ph.mart.healthapp.feature.progress.ui.photo.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ph.mart.healthapp.core.data.progress.ProgressPhoto
import ph.mart.healthapp.core.data.todayEpochDay
import ph.mart.healthapp.core.designsystem.component.DockedFabContentPadding
import ph.mart.healthapp.core.designsystem.component.epochDayToDate
import ph.mart.healthapp.core.designsystem.component.formatEpochDay
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.progress.R

/** 3-col grid grouped by month header, tap-to-select up to 2 (oldest drops — handled by the
 * caller's selection state). Empty state is the caller's responsibility (FullScreenState,
 * Sleepy) since it needs the "Add photo" CTA wired to the FAB sheet. */
@Composable
fun ProgressPhotoGrid(
    photos: List<ProgressPhoto>,
    selectedIds: List<Long>,
    onToggleSelect: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val monthFormat = remember(photos) { SimpleDateFormat("MMMM yyyy", Locale.getDefault()) }
    val grouped = photos.sortedByDescending { it.dateEpochDay }.groupBy { monthFormat.format(epochDayToDate(it.dateEpochDay)) }

    LazyVerticalGrid(columns = GridCells.Fixed(3), modifier = modifier, contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = DockedFabContentPadding)) {
        grouped.forEach { (month, monthPhotos) ->
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    text = month,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            }
            items(monthPhotos, key = { it.id }) { photo ->
                PhotoTile(
                    photo = photo,
                    selected = photo.id in selectedIds,
                    onClick = { onToggleSelect(photo.id) },
                )
            }
        }
    }
}

@Composable
private fun PhotoTile(photo: ProgressPhoto, selected: Boolean, onClick: () -> Unit) {
    val imageBitmap = rememberBitmapFromFile(photo.filePath, GRID_TILE_PX)
    Box(
        modifier = Modifier
            .padding(4.dp)
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .then(if (selected) Modifier.border(3.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp)) else Modifier)
            .clickable(onClick = onClick),
    ) {
        imageBitmap?.let {
            Image(bitmap = it, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        }
        Text(
            text = formatEpochDay(photo.dateEpochDay).substringBefore(","),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(4.dp)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
                .padding(horizontal = 4.dp, vertical = 4.dp),
        )
    }
}

/**
 * Every progress photo in the app is decoded through here — the grid's tiles, the comparison
 * slider and the timelapse player. It downsamples because it has to: a full-frame JPEG off the
 * camera is several megabytes decoded, and a grid holding a year of them (or a timelapse cycling
 * one after another) has no business keeping any of them at capture resolution.
 *
 * [maxWidthPx] is what the caller will actually draw into. `inSampleSize` only halves, so the
 * result is the smallest power-of-two reduction still at least that wide — never narrower than
 * asked for, so nothing is drawn upscaled.
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

private fun decodeSampled(path: String, maxWidthPx: Int): Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(path, bounds)
    if (bounds.outWidth <= 0) return null
    var sample = 1
    while (bounds.outWidth / (sample * 2) >= maxWidthPx) sample *= 2
    return BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sample })
}

@PreviewLightDark
@Composable
private fun ProgressPhotoGridPreview() {
    AppTheme {
        Surface {
            ProgressPhotoGrid(
                photos = listOf(
                    ProgressPhoto(id = 1, dateEpochDay = todayEpochDay(), filePath = ""),
                    ProgressPhoto(id = 2, dateEpochDay = todayEpochDay() - 30, filePath = ""),
                ),
                selectedIds = listOf(1L),
                onToggleSelect = {},
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
