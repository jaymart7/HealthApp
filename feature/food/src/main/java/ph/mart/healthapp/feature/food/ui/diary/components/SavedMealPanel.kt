package ph.mart.healthapp.feature.food.ui.diary.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.data.food.SavedMeal
import ph.mart.healthapp.core.data.food.SavedMealItem
import ph.mart.healthapp.core.data.food.totalKcal
import ph.mart.healthapp.core.designsystem.icon.AppIcons
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.food.R

/**
 * Saved meals, above the recents in the add-entry sheet. Tapping a row logs every item at once
 * into the meal the sheet is open on — a saved meal is logged whole, so unlike a
 * [FoodSuggestionPanel] row it never seeds the form below.
 *
 * Renders nothing until the user has saved a meal from a diary section.
 */
@Composable
internal fun SavedMealPanel(
    savedMeals: List<SavedMeal>,
    onLog: (SavedMeal) -> Unit,
    onDelete: (SavedMeal) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (savedMeals.isEmpty()) return
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = stringResource(R.string.food_saved_meals),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        savedMeals.forEach { meal ->
            SavedMealRow(meal = meal, onClick = { onLog(meal) }, onDelete = { onDelete(meal) })
        }
    }
}

@Composable
private fun SavedMealRow(meal: SavedMeal, onClick: () -> Unit, onDelete: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 12.dp, top = 8.dp, bottom = 8.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = meal.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(
                        R.string.food_meal_summary,
                        pluralStringResource(R.plurals.food_items, meal.items.size, meal.items.size),
                        meal.totalKcal(),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(44.dp)) {
                Icon(
                    imageVector = AppIcons.Delete,
                    contentDescription = stringResource(R.string.food_delete_saved_meal, meal.name),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onClick, modifier = Modifier.size(44.dp)) {
                Icon(
                    imageVector = AppIcons.Add,
                    contentDescription = stringResource(R.string.food_log_meal, meal.name),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun SavedMealPanelPreview() {
    AppTheme {
        Surface {
            SavedMealPanel(
                savedMeals = listOf(
                    SavedMeal(
                        id = 1,
                        name = "Usual breakfast",
                        items = listOf(
                            SavedMealItem("Greek yogurt", 1.0, "cup", 150, 20, 8, 4),
                            SavedMealItem("Oats", 60.0, "g", 230, 8, 40, 4),
                        ),
                    ),
                    SavedMeal(
                        id = 2,
                        name = "Post-gym shake",
                        items = listOf(SavedMealItem("Whey shake", 1.0, "scoop", 120, 24, 3, 1)),
                    ),
                ),
                onLog = {},
                onDelete = {},
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}
