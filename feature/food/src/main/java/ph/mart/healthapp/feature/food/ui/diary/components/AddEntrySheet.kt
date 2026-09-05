package ph.mart.healthapp.feature.food.ui.diary.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.data.food.FoodSuggestion
import ph.mart.healthapp.core.data.food.MealType
import ph.mart.healthapp.core.data.food.Recipe
import ph.mart.healthapp.core.data.food.SavedMeal
import ph.mart.healthapp.core.data.food.SavedMealItem
import ph.mart.healthapp.core.data.food.ScannedProduct
import ph.mart.healthapp.core.designsystem.component.AppBottomSheet
import ph.mart.healthapp.core.designsystem.component.FoodItemRow
import ph.mart.healthapp.core.designsystem.component.FoodItemRowVariant
import ph.mart.healthapp.core.designsystem.component.MacroInputGroup
import ph.mart.healthapp.core.designsystem.component.MicronutrientInputGroup
import ph.mart.healthapp.core.designsystem.component.PrimaryButton
import ph.mart.healthapp.core.designsystem.component.SecondaryButton
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.food.R
import ph.mart.healthapp.feature.food.ui.recipe.components.RecipePanel
import ph.mart.healthapp.feature.food.ui.search.components.FoodSearchPanel
import ph.mart.healthapp.feature.food.ui.shared.AddEntryForm
import ph.mart.healthapp.feature.food.ui.shared.SERVING_UNIT
import ph.mart.healthapp.feature.food.ui.shared.isSaveableFood
import ph.mart.healthapp.feature.food.ui.shared.isValid
import ph.mart.healthapp.feature.food.ui.shared.labelRes
import ph.mart.healthapp.feature.food.ui.shared.withPortionAmount

/**
 * The diary's log-a-food sheet: four shortcut panels that seed the form, then the form itself —
 * and, above them all, the one door in the app to a food the user hasn't decided on yet.
 *
 * [editing] turns the same sheet into the correct-a-logged-row sheet. The panels go with it: they
 * all seed a *new* log, and two of them ([onLogSavedMeal], [onLogAgain]) write rows the moment
 * they're tapped, which is not something that can happen while one row is being corrected.
 */
@Composable
internal fun AddEntrySheet(
    mealType: MealType,
    form: AddEntryForm,
    suggestions: List<FoodSuggestion>,
    savedMeals: List<SavedMeal>,
    recipes: List<Recipe>,
    onSelectRecipe: (Recipe) -> Unit,
    onDeleteRecipe: (Recipe) -> Unit,
    onNewRecipe: () -> Unit,
    onLogSavedMeal: (SavedMeal) -> Unit,
    onDeleteSavedMeal: (SavedMeal) -> Unit,
    onFormChange: (AddEntryForm) -> Unit,
    onSelectProduct: (ScannedProduct) -> Unit,
    onSelectSuggestion: (FoodSuggestion) -> Unit,
    onLogAgain: (FoodSuggestion) -> Unit,
    onToggleFavorite: (FoodSuggestion, Boolean) -> Unit,
    onDismiss: () -> Unit,
    onAdd: () -> Unit,
    /** Keeps the form as a food the user owns, without logging it. */
    onSaveMyFood: () -> Unit = {},
    /** Null when there is no day to suggest against — no profile yet, or nothing left in the
     * budget. Hidden rather than disabled: a control that can't answer shouldn't be there. */
    onGetIdeas: (() -> Unit)? = null,
    editing: Boolean = false,
) {
    AppBottomSheet(onDismiss = onDismiss) {
        Text(
            text = stringResource(
                if (editing) R.string.food_edit_meal_entry else R.string.food_add_to,
                stringResource(mealType.labelRes()),
            ),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 12.dp),
        )
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (!editing) {
                // Above the panels, because it answers a different question: they are faster
                // ways to log something already decided on, this is what to decide.
                onGetIdeas?.let { SecondaryButton(label = stringResource(R.string.food_get_ideas), onClick = it) }
                // Both panels seed the fields below; they stay editable either way, so this is a
                // shortcut past typing rather than a separate entry mode. Already-logged foods come
                // first — they cost no network round-trip and are the likelier match.
                RecipePanel(
                    recipes = recipes,
                    onSelect = onSelectRecipe,
                    onDelete = onDeleteRecipe,
                    onNewRecipe = onNewRecipe,
                )
                SavedMealPanel(
                    savedMeals = savedMeals,
                    onLog = onLogSavedMeal,
                    onDelete = onDeleteSavedMeal,
                )
                FoodSuggestionPanel(
                    suggestions = suggestions,
                    onSelect = onSelectSuggestion,
                    onLogAgain = onLogAgain,
                    onToggleFavorite = onToggleFavorite,
                )
                FoodSearchPanel(onSelect = onSelectProduct)
                // ponytail: on a diary with recipes and recents, a quick add is still a scroll to
                // the bottom of the sheet. A compact kcal-only row at the top is the upgrade if
                // that friction shows up.
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = stringResource(R.string.food_add_yourself),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = stringResource(R.string.food_blank_name_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            FoodItemRow(
                variant = FoodItemRowVariant.Editable,
                name = form.name,
                portionAmount = form.portionAmount,
                portionUnit = form.portionUnit,
                calories = form.calories,
                proteinG = form.proteinG,
                carbsG = form.carbsG,
                fatG = form.fatG,
                onNameChange = { onFormChange(form.copy(name = it)) },
                onPortionAmountChange = { onFormChange(form.withPortionAmount(it)) },
                onPortionUnitChange = { onFormChange(form.copy(portionUnit = it)) },
                onCaloriesChange = { onFormChange(form.copy(calories = it)) },
                // Stays in Kotlin: these are compared, not shown — `portionStep` switches on
                // them, and SERVING_UNIT is the value a recipe row is priced in.
                portionUnitOptions = listOf("g", "oz", "cup", SERVING_UNIT),
            )
            MacroInputGroup(
                proteinG = form.proteinG,
                carbsG = form.carbsG,
                fatG = form.fatG,
                onProteinChange = { onFormChange(form.copy(proteinG = it)) },
                onCarbsChange = { onFormChange(form.copy(carbsG = it)) },
                onFatChange = { onFormChange(form.copy(fatG = it)) },
            )
            MicronutrientInputGroup(
                fiberG = form.fiberG,
                sugarG = form.sugarG,
                sodiumMg = form.sodiumMg,
                onFiberChange = { onFormChange(form.copy(fiberG = it)) },
                onSugarChange = { onFormChange(form.copy(sugarG = it)) },
                onSodiumChange = { onFormChange(form.copy(sodiumMg = it)) },
            )
            // The authoring door, and the whole of it: the form above already holds every field a
            // food has, so keeping one is one more button rather than a second screen. Hidden
            // until there is something worth keeping — the rule the meal-ideas button follows —
            // and absent while correcting a logged row, where the panels are gone for the same
            // reason. Saving the same name twice edits it, which is how a food is corrected later.
            if (!editing && form.isSaveableFood()) {
                SecondaryButton(
                    label = stringResource(R.string.food_save_as_my_food),
                    onClick = onSaveMyFood,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                SecondaryButton(label = stringResource(R.string.food_cancel), onClick = onDismiss, modifier = Modifier.weight(1f))
                PrimaryButton(
                    // The label is the only thing telling the user a nameless entry will be
                    // accepted; the button itself is enabled the moment there are calories.
                    label = when {
                        editing -> stringResource(R.string.food_save)
                        form.name.isBlank() -> stringResource(R.string.food_quick_add)
                        else -> stringResource(R.string.food_add)
                    },
                    onClick = onAdd,
                    enabled = form.isValid(),
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun AddEntrySheetPreview() {
    AppTheme {
        AddEntrySheet(
            mealType = MealType.Breakfast,
            form = AddEntryForm(name = "Greek yogurt", portionAmount = 1.0, portionUnit = "cup", calories = 150, proteinG = 20, carbsG = 8, fatG = 4),
            suggestions = listOf(FoodSuggestion("Greek yogurt", 1.0, "cup", 150, 20, 8, 4, isFavorite = true)),
            savedMeals = listOf(
                SavedMeal(id = 1, name = "Usual breakfast", items = listOf(SavedMealItem("Greek yogurt", 1.0, "cup", 150, 20, 8, 4))),
            ),
            recipes = emptyList(),
            onSelectRecipe = {},
            onDeleteRecipe = {},
            onNewRecipe = {},
            onLogSavedMeal = {},
            onDeleteSavedMeal = {},
            onFormChange = {},
            onSelectProduct = {},
            onSelectSuggestion = {},
            onLogAgain = {},
            onToggleFavorite = { _, _ -> },
            onDismiss = {},
            onAdd = {},
        )
    }
}

/** Correcting a logged row: no shortcut panels, and the button commits over the row it opened. */
@PreviewLightDark
@Composable
private fun AddEntrySheetEditingPreview() {
    AppTheme {
        AddEntrySheet(
            mealType = MealType.Lunch,
            form = AddEntryForm(name = "Grilled chicken breast", portionAmount = 150.0, portionUnit = "g", calories = 210, proteinG = 32, carbsG = 2, fatG = 8),
            suggestions = emptyList(),
            savedMeals = emptyList(),
            recipes = emptyList(),
            onSelectRecipe = {},
            onDeleteRecipe = {},
            onNewRecipe = {},
            onLogSavedMeal = {},
            onDeleteSavedMeal = {},
            onFormChange = {},
            onSelectProduct = {},
            onSelectSuggestion = {},
            onLogAgain = {},
            onToggleFavorite = { _, _ -> },
            onDismiss = {},
            onAdd = {},
            editing = true,
        )
    }
}

/** The quick-add shape: a blank name, so the button reads "Quick add" rather than "Add". */
@PreviewLightDark
@Composable
private fun AddEntrySheetQuickAddPreview() {
    AppTheme {
        AddEntrySheet(
            mealType = MealType.Snacks,
            form = AddEntryForm(name = "", calories = 320),
            suggestions = emptyList(),
            savedMeals = emptyList(),
            recipes = emptyList(),
            onSelectRecipe = {},
            onDeleteRecipe = {},
            onNewRecipe = {},
            onLogSavedMeal = {},
            onDeleteSavedMeal = {},
            onFormChange = {},
            onSelectProduct = {},
            onSelectSuggestion = {},
            onLogAgain = {},
            onToggleFavorite = { _, _ -> },
            onDismiss = {},
            onAdd = {},
        )
    }
}
