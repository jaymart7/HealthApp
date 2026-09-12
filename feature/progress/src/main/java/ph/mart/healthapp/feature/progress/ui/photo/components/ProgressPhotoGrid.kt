package ph.mart.healthapp.feature.progress.ui.photo.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Locale
import ph.mart.healthapp.core.data.progress.ProgressPhoto
import ph.mart.healthapp.core.data.todayEpochDay
import ph.mart.healthapp.core.designsystem.component.DockedFabContentPadding
import ph.mart.healthapp.core.designsystem.component.GRID_TILE_PX
import ph.mart.healthapp.core.designsystem.component.epochDayToDate
import ph.mart.healthapp.core.designsystem.component.formatDayMonth
import ph.mart.healthapp.core.designsystem.component.rememberBitmapFromFile
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.progress.R
import ph.mart.healthapp.feature.progress.ui.shared.components.OverlayLabelSize
import ph.mart.healthapp.feature.progress.ui.shared.components.PhotoOverlayLabel

/** 3:4, the shape a progress photo is actually taken in. A square centre-crop of one loses the
 * head and the feet first, which are the two things a body record is read for. */
private const val TILE_ASPECT = 0.75f

/**
 * The whole Photos page, and the page's **only** scroller: the header strip and the selection hint
 * ride inside the grid as full-span items rather than sitting in a column above it, so they scroll
 * away with the content and the month headers can pin against the top on their own.
 *
 * Tap-to-select up to two (oldest drops — the caller's selection state handles that), the second
 * pick opening the comparison. The empty state is the caller's (`FullScreenState`, Sleepy), since
 * it belongs to the page rather than to a grid with nothing in it.
 */
@Composable
fun ProgressPhotoGrid(
    photos: List<ProgressPhoto>,
    selectedIds: List<Long>,
    onToggleSelect: (Long) -> Unit,
    onClearSelection: () -> Unit,
    onOpenTimelapse: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val monthFormat = remember { SimpleDateFormat("MMMM yyyy", Locale.getDefault()) }
    val grouped = remember(photos) {
        photos.sortedByDescending { it.dateEpochDay }
            .groupBy { monthFormat.format(epochDayToDate(it.dateEpochDay)) }
    }
    val fullSpan: androidx.compose.foundation.lazy.grid.LazyGridItemSpanScope.() -> GridItemSpan =
        { GridItemSpan(maxLineSpan) }

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = modifier,
        contentPadding = PaddingValues(top = 8.dp, bottom = DockedFabContentPadding),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item(key = "header", span = fullSpan) {
            PhotosHeaderStrip(photos = photos, onOpenTimelapse = onOpenTimelapse)
        }
        item(key = "hint", span = fullSpan) {
            PhotoSelectionHint(selectedCount = selectedIds.size, onClear = onClearSelection)
        }

        grouped.forEach { (month, monthPhotos) ->
            // Pinned rather than scrolled off: three columns of near-identical crops lose their
            // place fast, and the month is the only thing on the page that says where you are.
            stickyHeader(key = month) {
                MonthHeader(month = month, count = monthPhotos.size)
            }
            items(monthPhotos, key = { it.id }) { photo ->
                PhotoTile(
                    photo = photo,
                    selectionNumber = selectedIds.indexOf(photo.id).takeIf { it >= 0 }?.plus(1),
                    onClick = { onToggleSelect(photo.id) },
                )
            }
        }
    }
}

/** Opaque `surface`, so the tiles pass underneath it rather than showing through. */
@Composable
private fun MonthHeader(month: String, count: Int, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(top = 12.dp, bottom = 8.dp),
    ) {
        Text(
            text = month,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = pluralStringResource(R.plurals.progress_photos_count, count, count),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun PhotoTile(photo: ProgressPhoto, selectionNumber: Int?, onClick: () -> Unit) {
    val imageBitmap = rememberBitmapFromFile(photo.filePath, GRID_TILE_PX)
    val selected = selectionNumber != null
    Box(
        modifier = Modifier
            .aspectRatio(TILE_ASPECT)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .then(
                if (selected) {
                    Modifier.border(3.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
                } else {
                    Modifier
                },
            )
            .clickable(onClick = onClick)
            .semantics { this.selected = selected },
    ) {
        imageBitmap?.let {
            Image(
                bitmap = it,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
        PhotoOverlayLabel(
            text = formatDayMonth(photo.dateEpochDay),
            size = OverlayLabelSize.Small,
            modifier = Modifier.align(Alignment.BottomStart).padding(4.dp),
        )
        // The border alone says *that* a tile is picked; the numeral says which of the two it is,
        // which is the half the comparison actually depends on.
        selectionNumber?.let {
            SelectionBadge(
                number = it,
                modifier = Modifier.align(Alignment.TopEnd).padding(4.dp),
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun ProgressPhotoGridPreview() {
    val today = todayEpochDay()
    AppTheme {
        Surface {
            ProgressPhotoGrid(
                photos = listOf(
                    ProgressPhoto(id = 1, dateEpochDay = today, filePath = ""),
                    ProgressPhoto(id = 2, dateEpochDay = today - 14, filePath = ""),
                    ProgressPhoto(id = 3, dateEpochDay = today - 45, filePath = ""),
                    ProgressPhoto(id = 4, dateEpochDay = today - 92, filePath = ""),
                ),
                selectedIds = listOf(1L),
                onToggleSelect = {},
                onClearSelection = {},
                onOpenTimelapse = {},
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            )
        }
    }
}
