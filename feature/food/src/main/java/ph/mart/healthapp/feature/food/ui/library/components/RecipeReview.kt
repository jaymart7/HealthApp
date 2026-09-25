package ph.mart.healthapp.feature.food.ui.library.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import ph.mart.healthapp.core.data.food.Recipe
import ph.mart.healthapp.core.data.food.SavedMealItem
import ph.mart.healthapp.core.data.food.perServing
import ph.mart.healthapp.core.designsystem.component.AppTextField
import ph.mart.healthapp.core.designsystem.component.FoodItemRow
import ph.mart.healthapp.core.designsystem.component.FoodItemRowVariant
import ph.mart.healthapp.core.designsystem.component.NumericStepperField
import ph.mart.healthapp.core.designsystem.component.TextButton
import ph.mart.healthapp.core.designsystem.icon.AppIcons
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.core.designsystem.theme.tabularNums
import ph.mart.healthapp.feature.food.R
import ph.mart.healthapp.feature.food.ui.library.LibraryItemForm
import ph.mart.healthapp.feature.food.ui.library.LibraryKind
import ph.mart.healthapp.feature.food.ui.shared.components.CardLabel

/**
 * A recipe or a saved meal as a form to check: its name, a recipe's yield, what one portion costs,
 * and what is in it. A row taps into [IngredientSheet] — how an AI estimate gets corrected without
 * retyping it — and ✕ takes it out.
 *
 * A saved meal has no yield, so it has no stepper and its summary is the whole plate.
 */
@Composable
internal fun RecipeReview(
    form: LibraryItemForm,
    onFormChange: (LibraryItemForm) -> Unit,
    onOpenIngredient: (Int) -> Unit,
    onRemoveIngredient: (Int) -> Unit,
    onAddIngredient: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isRecipe = form.kind == LibraryKind.Recipe
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        AppTextField(
            value = form.name,
            onValueChange = { onFormChange(form.copy(name = it)) },
            label = stringResource(R.string.food_library_name),
            placeholder = stringResource(R.string.food_recipe_placeholder),
        )
        if (isRecipe) {
            NumericStepperField(
                label = stringResource(R.string.food_recipe_makes),
                value = form.servings.toString(),
                unitSuffix = pluralStringResource(R.plurals.food_recipe_servings, form.servings),
                onIncrement = { onFormChange(form.copy(servings = form.servings + 1)) },
                onDecrement = { onFormChange(form.copy(servings = (form.servings - 1).coerceAtLeast(1))) },
            )
        }
        Summary(form = form, perServing = isRecipe)
        CardLabel(stringResource(R.string.food_library_contents), modifier = Modifier.padding(top = 12.dp))
        form.ingredients.forEachIndexed { index, ingredient ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable(
                    onClickLabel = stringResource(R.string.food_recipe_edit, ingredient.name),
                    onClick = { onOpenIngredient(index) },
                ),
            ) {
                FoodItemRow(
                    variant = FoodItemRowVariant.Display,
                    name = ingredient.name,
                    portionAmount = ingredient.portionAmount,
                    portionUnit = ingredient.portionUnit,
                    calories = ingredient.calories,
                    proteinG = ingredient.proteinG,
                    carbsG = ingredient.carbsG,
                    fatG = ingredient.fatG,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { onRemoveIngredient(index) }) {
                    Icon(
                        imageVector = AppIcons.Close,
                        contentDescription = stringResource(R.string.food_remove, ingredient.name),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        TextButton(
            label = stringResource(if (isRecipe) R.string.food_recipe_add_ingredient else R.string.food_library_add_food),
            onClick = onAddIngredient,
            icon = AppIcons.Add,
        )
    }
}

/** What one portion costs — the number the whole screen exists to produce, so it sits above the
 * list rather than under it. A saved meal is eaten whole, so it states the whole. */
@Composable
private fun Summary(form: LibraryItemForm, perServing: Boolean) {
    val serving = Recipe(
        id = 0,
        name = form.name,
        servings = if (perServing) form.servings else 1,
        items = form.ingredients,
    ).perServing()
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = stringResource(
                    if (perServing) R.string.food_recipe_per_serving else R.string.food_library_in_total,
                    serving.calories,
                ),
                style = MaterialTheme.typography.titleMedium.tabularNums,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(R.string.food_recipe_macro_line, serving.proteinG, serving.carbsG, serving.fatG),
                style = MaterialTheme.typography.bodySmall.tabularNums,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private val PREVIEW_ITEMS = listOf(
    SavedMealItem("Chicken thighs", 1000.0, "g", 1640, 170, 0, 104),
    SavedMealItem("Soy sauce", 60.0, "ml", 32, 5, 3, 0),
    SavedMealItem("Cane vinegar", 120.0, "ml", 22, 0, 1, 0),
)

@PreviewLightDark
@Composable
private fun RecipeReviewPreview() {
    AppTheme {
        Surface {
            RecipeReview(
                form = LibraryItemForm(kind = LibraryKind.Recipe, name = "Chicken adobo", servings = 4, ingredients = PREVIEW_ITEMS),
                onFormChange = {},
                onOpenIngredient = {},
                onRemoveIngredient = {},
                onAddIngredient = {},
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

/** A saved meal: no yield, and the summary is the plate. */
@PreviewLightDark
@Composable
private fun RecipeReviewMealPreview() {
    AppTheme {
        Surface {
            RecipeReview(
                form = LibraryItemForm(kind = LibraryKind.Meal, name = "Usual breakfast", ingredients = PREVIEW_ITEMS.take(2)),
                onFormChange = {},
                onOpenIngredient = {},
                onRemoveIngredient = {},
                onAddIngredient = {},
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}
