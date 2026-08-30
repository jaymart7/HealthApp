package ph.mart.healthapp.feature.food.ui.recipe.components

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
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.data.food.Recipe
import ph.mart.healthapp.core.data.food.SavedMealItem
import ph.mart.healthapp.core.data.food.perServing
import ph.mart.healthapp.core.designsystem.icon.AppIcons
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.food.ui.diary.components.SavedMealPanel

/**
 * Recipes, at the top of the add-entry sheet. Two differences from [SavedMealPanel], both
 * deliberate: tapping a recipe *seeds the form below* with one serving rather than logging it whole
 * — a recipe is one diary row, and the user may be eating half of it — and this panel renders even
 * when the list is empty, because its "New recipe" row is the only way into the builder.
 */
@Composable
internal fun RecipePanel(
    recipes: List<Recipe>,
    onSelect: (Recipe) -> Unit,
    onDelete: (Recipe) -> Unit,
    onNewRecipe: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = "Recipes",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        recipes.forEach { recipe ->
            RecipeRow(recipe = recipe, onClick = { onSelect(recipe) }, onDelete = { onDelete(recipe) })
        }
        NewRecipeRow(onClick = onNewRecipe)
    }
}

@Composable
private fun RecipeRow(recipe: Recipe, onClick: () -> Unit, onDelete: () -> Unit) {
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
                    text = recipe.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "${recipe.perServing().calories} kcal per serving · makes ${recipe.servings}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(44.dp)) {
                Icon(
                    imageVector = AppIcons.Delete,
                    contentDescription = "Delete recipe ${recipe.name}",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onClick, modifier = Modifier.size(44.dp)) {
                Icon(
                    imageVector = AppIcons.Add,
                    contentDescription = "Use ${recipe.name}",
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun NewRecipeRow(onClick: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
        ) {
            Icon(
                imageVector = AppIcons.Add,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = "New recipe",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun RecipePanelPreview() {
    AppTheme {
        Surface {
            RecipePanel(
                recipes = listOf(
                    Recipe(
                        id = 1,
                        name = "Chili",
                        servings = 4,
                        items = listOf(
                            SavedMealItem("Beans", 400.0, "g", 480, 28, 80, 4),
                            SavedMealItem("Beef mince", 500.0, "g", 1100, 100, 0, 80),
                        ),
                    ),
                ),
                onSelect = {},
                onDelete = {},
                onNewRecipe = {},
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

/** The empty case is a first-class state here — the "New recipe" row has to be reachable before
 * any recipe exists. */
@PreviewLightDark
@Composable
private fun RecipePanelEmptyPreview() {
    AppTheme {
        Surface {
            RecipePanel(
                recipes = emptyList(),
                onSelect = {},
                onDelete = {},
                onNewRecipe = {},
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}
