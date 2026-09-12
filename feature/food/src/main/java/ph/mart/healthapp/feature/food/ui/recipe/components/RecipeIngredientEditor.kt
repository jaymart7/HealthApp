package ph.mart.healthapp.feature.food.ui.recipe.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.data.food.SavedMealItem
import ph.mart.healthapp.core.designsystem.component.FoodItemRow
import ph.mart.healthapp.core.designsystem.component.FoodItemRowVariant
import ph.mart.healthapp.core.designsystem.component.MacroFieldGroup
import ph.mart.healthapp.core.designsystem.component.MicronutrientInputGroup
import ph.mart.healthapp.core.designsystem.component.SecondaryButton
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.food.R
import ph.mart.healthapp.feature.food.ui.search.components.FoodSearchPanel
import ph.mart.healthapp.feature.food.ui.shared.withPortionAmount

/**
 * One ingredient's fields, plus the button that commits it to the recipe. Identical in shape to the
 * add-entry sheet's form — including [FoodSearchPanel], which owns its own ViewModel and so drops
 * in for free, letting an ingredient come from FoodData Central instead of the keyboard.
 */
@Composable
internal fun RecipeIngredientEditor(
    draft: SavedMealItem,
    canAdd: Boolean,
    onDraftChange: (SavedMealItem) -> Unit,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = stringResource(R.string.food_recipe_add_ingredient_title),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FoodSearchPanel(
            onSelect = { product ->
                onDraftChange(
                    SavedMealItem(
                        name = product.name,
                        portionAmount = product.portionAmount,
                        portionUnit = product.portionUnit,
                        calories = product.calories,
                        proteinG = product.proteinG,
                        carbsG = product.carbsG,
                        fatG = product.fatG,
                        nutrients = product.nutrients,
                    ),
                )
            },
        )
        FoodItemRow(
            variant = FoodItemRowVariant.Editable,
            name = draft.name,
            portionAmount = draft.portionAmount,
            portionUnit = draft.portionUnit,
            calories = draft.calories,
            proteinG = draft.proteinG,
            carbsG = draft.carbsG,
            fatG = draft.fatG,
            onNameChange = { onDraftChange(draft.copy(name = it)) },
            onPortionAmountChange = { onDraftChange(draft.withPortionAmount(it)) },
            onPortionUnitChange = { onDraftChange(draft.copy(portionUnit = it)) },
            onCaloriesChange = { onDraftChange(draft.copy(calories = it)) },
        )
        // A [SavedMealItem] is a `:core:data` type with non-null figures, so an ingredient's cells
        // never draw the dash: `0` here has always meant unknown-or-none and still does.
        MacroFieldGroup(
            proteinG = draft.proteinG,
            carbsG = draft.carbsG,
            fatG = draft.fatG,
            onProteinChange = { onDraftChange(draft.copy(proteinG = it ?: 0)) },
            onCarbsChange = { onDraftChange(draft.copy(carbsG = it ?: 0)) },
            onFatChange = { onDraftChange(draft.copy(fatG = it ?: 0)) },
        )
        MicronutrientInputGroup(
            fiberG = draft.nutrients.fiberG.takeIf { it > 0 },
            sugarG = draft.nutrients.sugarG.takeIf { it > 0 },
            sodiumMg = draft.nutrients.sodiumMg.takeIf { it > 0 },
            onFiberChange = { onDraftChange(draft.copy(nutrients = draft.nutrients.copy(fiberG = it ?: 0))) },
            onSugarChange = { onDraftChange(draft.copy(nutrients = draft.nutrients.copy(sugarG = it ?: 0))) },
            onSodiumChange = { onDraftChange(draft.copy(nutrients = draft.nutrients.copy(sodiumMg = it ?: 0))) },
        )
        SecondaryButton(
            label = stringResource(R.string.food_recipe_add_ingredient),
            onClick = onAdd,
            enabled = canAdd,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@PreviewLightDark
@Composable
private fun RecipeIngredientEditorPreview() {
    AppTheme {
        Surface {
            RecipeIngredientEditor(
                draft = SavedMealItem("Beef mince", 500.0, "g", 1100, 100, 0, 80),
                canAdd = true,
                onDraftChange = {},
                onAdd = {},
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}
