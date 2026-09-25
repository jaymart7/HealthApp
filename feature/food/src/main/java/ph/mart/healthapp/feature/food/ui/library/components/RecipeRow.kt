package ph.mart.healthapp.feature.food.ui.library.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import ph.mart.healthapp.core.data.food.Recipe
import ph.mart.healthapp.core.data.food.SavedMealItem
import ph.mart.healthapp.core.data.food.perServing
import ph.mart.healthapp.core.designsystem.icon.AppIcons
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.food.R
import ph.mart.healthapp.feature.food.ui.diary.components.RowIconButton
import ph.mart.healthapp.feature.food.ui.diary.components.RowText
import ph.mart.healthapp.feature.food.ui.diary.components.SheetRow

/**
 * One recipe in the add-entry sheet's Recipes tab.
 *
 * **No `+`, and that is the only thing distinguishing it from its neighbours.** A recipe is a dish
 * with servings in it: logging one whole is almost never what anyone means, so tapping it loads one
 * serving into the form where the portion stepper can halve it. The chevron says the row leads
 * somewhere; the absence of the filled button says there is nothing here that writes.
 *
 * It lives beside the library's editor rather than with the sheet's other rows because grouping in
 * this app is by *subject* — the same reason `RecipePanel` was here before it became this row.
 */
@Composable
internal fun RecipeRow(
    recipe: Recipe,
    onSelect: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SheetRow(onClick = onSelect, modifier = modifier) {
        RowText(
            name = recipe.name,
            detail = stringResource(R.string.food_recipe_summary, recipe.perServing().calories, recipe.servings),
            modifier = Modifier.weight(1f),
        )
        RowIconButton(
            icon = AppIcons.Delete,
            contentDescription = stringResource(R.string.food_recipe_delete, recipe.name),
            onClick = onDelete,
        )
        RowIconButton(
            icon = AppIcons.ChevronRight,
            // The row itself is the control and already announces the recipe by name.
            contentDescription = null,
            onClick = onSelect,
        )
    }
}

@PreviewLightDark
@Composable
private fun RecipeRowPreview() {
    AppTheme {
        Surface {
            Column {
                RecipeRow(
                    recipe = Recipe(
                        id = 1,
                        name = "Chili",
                        servings = 4,
                        items = listOf(
                            SavedMealItem("Beans", 400.0, "g", 480, 28, 80, 4),
                            SavedMealItem("Beef mince", 500.0, "g", 1100, 100, 0, 80),
                        ),
                    ),
                    onSelect = {},
                    onDelete = {},
                )
            }
        }
    }
}
