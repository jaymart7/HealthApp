package ph.mart.healthapp.feature.food.ui.library.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.data.food.SavedMealItem
import ph.mart.healthapp.core.designsystem.component.AppBottomSheet
import ph.mart.healthapp.core.designsystem.component.FoodItemRow
import ph.mart.healthapp.core.designsystem.component.FoodItemRowVariant
import ph.mart.healthapp.core.designsystem.component.MacroFieldGroup
import ph.mart.healthapp.core.designsystem.component.MicronutrientInputGroup
import ph.mart.healthapp.core.designsystem.component.PrimaryButton
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.food.R
import ph.mart.healthapp.feature.food.ui.search.components.FoodSearchPanel
import ph.mart.healthapp.feature.food.ui.shared.withPortionAmount

/**
 * One ingredient's fields in a sheet over the review, so the recipe stays the page and the fields
 * come and go. Identical in shape to the add-entry sheet's form — including [FoodSearchPanel], which
 * owns its own ViewModel and so drops in for free, letting an ingredient come from FoodData Central
 * instead of the keyboard.
 *
 * [isNew] picks the button's word — Add appends, Done writes a tapped row back where it was — and
 * whether the search leads: a new ingredient is usually found, a tapped one is corrected.
 * Dismissing — the scrim, the handle or back — keeps the list as it was.
 */
@Composable
internal fun IngredientSheet(
    draft: SavedMealItem,
    isNew: Boolean,
    onDraftChange: (SavedMealItem) -> Unit,
    onDone: () -> Unit,
    onDismiss: () -> Unit,
) {
    AppBottomSheet(
        title = stringResource(if (isNew) R.string.food_recipe_add_ingredient_title else R.string.food_library_edit_ingredient),
        onDismiss = onDismiss,
    ) {
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Only for a new one: a tapped row is opened to correct its figures, and eight search
            // hits above them would push the thing being corrected below the fold.
            if (isNew) {
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
            }
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
            PrimaryButton(
                label = stringResource(if (isNew) R.string.food_recipe_add_ingredient else R.string.food_library_done),
                onClick = onDone,
                enabled = draft.name.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun IngredientSheetPreview() {
    AppTheme {
        IngredientSheet(
            draft = SavedMealItem("Beef mince", 500.0, "g", 1100, 100, 0, 80),
            isNew = false,
            onDraftChange = {},
            onDone = {},
            onDismiss = {},
        )
    }
}
