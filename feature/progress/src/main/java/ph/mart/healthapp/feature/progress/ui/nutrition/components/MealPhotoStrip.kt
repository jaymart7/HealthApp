package ph.mart.healthapp.feature.progress.ui.nutrition.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.data.food.FoodEntry
import ph.mart.healthapp.core.data.food.MealType
import ph.mart.healthapp.core.data.todayEpochDay
import ph.mart.healthapp.core.designsystem.component.MealThumbnail
import ph.mart.healthapp.core.designsystem.component.TextButton
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.core.designsystem.theme.tabularNums
import ph.mart.healthapp.feature.progress.R

/** How many plates the strip shows before "See all" is the only way to the rest. Enough to fill a
 * phone's width twice over, few enough that the card stays a footnote under the chart. */
private const val STRIP_LIMIT = 12

private val TileSize = 72.dp

/**
 * The plates behind the calories — the newest [STRIP_LIMIT], newest first, under the Food page's
 * chart and averages.
 *
 * Drawn only when there is something to draw. Every other card on a detail page has a number even
 * on a bare day; this one is the record of a thing the user may simply never have done, and an
 * empty "Meal photos" card is an ad for the camera on a page that is about what was eaten.
 */
@Composable
internal fun MealPhotoStrip(
    photos: List<FoodEntry>,
    onOpen: (Long?) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (photos.isEmpty()) return
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(start = 16.dp, end = 4.dp, top = 16.dp, bottom = 12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.progress_meal_photos),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                TextButton(label = stringResource(R.string.progress_meal_photos_all), onClick = { onOpen(null) })
            }
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(top = 12.dp, end = 12.dp),
            ) {
                items(photos.take(STRIP_LIMIT), key = { it.id }) { entry ->
                    MealPhotoTile(entry = entry, onClick = { onOpen(entry.id) })
                }
            }
        }
    }
}

/** A plate and what it was logged as. The name is capped at one line: a tile is 72dp wide and
 * "Chicken adobo with rice" wrapping to four lines would set the row's height by its longest
 * label. */
@Composable
private fun MealPhotoTile(entry: FoodEntry, onClick: () -> Unit) {
    val spoken = stringResource(R.string.progress_meal_photo_spoken, entry.name, entry.calories)
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .width(TileSize)
            .clickable(onClick = onClick)
            // One target, one label: the tile, its name and its calories are one thing to a screen
            // reader, and the resource is resolved above because a semantics lambda cannot.
            .clearAndSetSemantics { contentDescription = spoken },
    ) {
        MealThumbnail(path = entry.photoPath.orEmpty(), size = TileSize)
        Text(
            text = entry.name,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = stringResource(R.string.progress_meal_photo_kcal, entry.calories),
            style = MaterialTheme.typography.labelSmall.tabularNums,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

internal fun previewMealPhotos(): List<FoodEntry> {
    val today = todayEpochDay()
    return listOf(
        FoodEntry(id = 1, name = "Chicken adobo", dateEpochDay = today, mealType = MealType.Dinner, portionAmount = 1.0, portionUnit = "serving", calories = 430, proteinG = 28, carbsG = 12, fatG = 29, photoPath = "/preview/none.jpg"),
        FoodEntry(id = 2, name = "Tapsilog", dateEpochDay = today, mealType = MealType.Breakfast, portionAmount = 1.0, portionUnit = "serving", calories = 620, proteinG = 30, carbsG = 65, fatG = 24, photoPath = "/preview/none.jpg"),
        FoodEntry(id = 3, name = "Sinigang na baboy", dateEpochDay = today - 1, mealType = MealType.Lunch, portionAmount = 1.0, portionUnit = "serving", calories = 380, proteinG = 26, carbsG = 18, fatG = 21, photoPath = "/preview/none.jpg"),
    )
}

@PreviewLightDark
@Composable
private fun MealPhotoStripPreview() {
    AppTheme {
        Surface {
            MealPhotoStrip(photos = previewMealPhotos(), onOpen = {}, modifier = Modifier.padding(16.dp))
        }
    }
}
