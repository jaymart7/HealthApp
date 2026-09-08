package ph.mart.healthapp.feature.progress.ui.nutrition.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import ph.mart.healthapp.core.data.food.FoodEntry
import ph.mart.healthapp.core.designsystem.component.DockedFabContentPadding
import ph.mart.healthapp.core.designsystem.component.FULL_FRAME_PX
import ph.mart.healthapp.core.designsystem.component.GRID_TILE_PX
import ph.mart.healthapp.core.designsystem.component.SecondaryButton
import ph.mart.healthapp.core.designsystem.component.formatEpochDay
import ph.mart.healthapp.core.designsystem.component.rememberBitmapFromFile
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.core.designsystem.theme.tabularNums
import ph.mart.healthapp.feature.progress.R

/**
 * Every kept plate, newest first, grouped under the day it was eaten — the whole of what the Food
 * page's strip is a window onto.
 *
 * A full-screen overlay inside the tab rather than a route, like the timelapse and the recap: it
 * reads the already-combined state, writes nothing, and a route would buy a second
 * `ViewModelStoreOwner` and a second copy of thirteen repositories to draw a grid.
 *
 * [viewedId] is the plate opened full-frame over the grid. Two levels, one handler: back closes the
 * frame if one is open and the gallery otherwise, so one press is one level either way.
 */
@Composable
internal fun MealPhotoGallery(
    photos: List<FoodEntry>,
    viewedId: Long?,
    onView: (Long?) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewed = photos.firstOrNull { it.id == viewedId }

    val navigationState = rememberNavigationEventState(currentInfo = NavigationEventInfo.None)
    NavigationBackHandler(
        state = navigationState,
        onBackCompleted = { if (viewed != null) onView(null) else onClose() },
    )

    Surface(color = MaterialTheme.colorScheme.surface, modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = stringResource(R.string.progress_meal_photos),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 16.dp),
            )
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(bottom = DockedFabContentPadding),
                modifier = Modifier.weight(1f),
            ) {
                // Newest first, and already in that order from the query — grouping preserves it,
                // so nothing here re-sorts and the two views cannot disagree about "newest".
                photos.groupBy { it.dateEpochDay }.forEach { (day, dayPhotos) ->
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Text(
                            text = formatEpochDay(day),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 8.dp),
                        )
                    }
                    items(dayPhotos, key = { it.id }) { entry ->
                        GalleryTile(entry = entry, onClick = { onView(entry.id) })
                    }
                }
            }
            SecondaryButton(
                label = stringResource(R.string.progress_close),
                onClick = onClose,
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            )
        }
    }

    viewed?.let { MealPhotoFrame(entry = it, onClose = { onView(null) }) }
}

@Composable
private fun GalleryTile(entry: FoodEntry, onClick: () -> Unit) {
    val bitmap = rememberBitmapFromFile(entry.photoPath.orEmpty(), GRID_TILE_PX)
    val spoken = stringResource(R.string.progress_meal_photo_spoken, entry.name, entry.calories)
    Box(
        modifier = Modifier
            .padding(4.dp)
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .clickable(onClick = onClick)
            .clearAndSetSemantics { contentDescription = spoken },
    ) {
        bitmap?.let {
            Image(bitmap = it, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        }
    }
}

/** One plate, full width, over the grid — with what it was logged as, which is the only reason to
 * keep the picture beside the numbers rather than in the gallery app. */
@Composable
private fun MealPhotoFrame(entry: FoodEntry, onClose: () -> Unit) {
    val bitmap = rememberBitmapFromFile(entry.photoPath.orEmpty(), FULL_FRAME_PX)
    Surface(color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerLow),
            ) {
                bitmap?.let {
                    Image(bitmap = it, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                }
            }
            Text(text = entry.name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            // No meal name here — "Breakfast" is `:feature:food`'s string to resolve, and features
            // don't import each other. The day and the calories are what this frame is for.
            Text(
                text = stringResource(
                    R.string.progress_meal_photo_caption,
                    formatEpochDay(entry.dateEpochDay),
                    entry.calories,
                ),
                style = MaterialTheme.typography.bodyMedium.tabularNums,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            SecondaryButton(label = stringResource(R.string.progress_close), onClick = onClose, modifier = Modifier.fillMaxWidth())
        }
    }
}

@PreviewLightDark
@Composable
private fun MealPhotoGalleryPreview() {
    AppTheme {
        MealPhotoGallery(photos = previewMealPhotos(), viewedId = null, onView = {}, onClose = {})
    }
}

/** The second level: a plate open over the grid, which is what back closes first. */
@PreviewLightDark
@Composable
private fun MealPhotoFramePreview() {
    AppTheme {
        MealPhotoGallery(photos = previewMealPhotos(), viewedId = 1L, onView = {}, onClose = {})
    }
}
