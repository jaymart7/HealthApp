package ph.mart.healthapp.feature.progress.ui.photo.components

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
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ph.mart.healthapp.core.data.progress.ProgressPhoto
import ph.mart.healthapp.core.designsystem.component.DockedFabContentPadding
import ph.mart.healthapp.core.designsystem.component.epochDayToDate
import ph.mart.healthapp.core.designsystem.component.formatEpochDay
import ph.mart.healthapp.core.designsystem.component.todayEpochDay
import ph.mart.healthapp.core.designsystem.theme.AppTheme

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
    val imageBitmap = rememberBitmapFromFile(photo.filePath)
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

@Composable
fun rememberBitmapFromFile(path: String): ImageBitmap? {
    val state = produceState<ImageBitmap?>(initialValue = null, key1 = path) {
        value = withContext(Dispatchers.IO) { BitmapFactory.decodeFile(path)?.asImageBitmap() }
    }
    return state.value
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
