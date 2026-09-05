package ph.mart.healthapp.feature.profile.ui.library

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import ph.mart.healthapp.core.data.food.Recipe
import ph.mart.healthapp.core.data.food.SavedMeal
import ph.mart.healthapp.core.data.food.SavedMealItem
import ph.mart.healthapp.core.data.food.ScannedProduct
import ph.mart.healthapp.core.data.food.perServing
import ph.mart.healthapp.core.data.food.totalKcal
import ph.mart.healthapp.feature.profile.R

/**
 * Every saved meal and every recipe — not the newest-N windows the add-entry sheet's panels read.
 * Those windows exist to keep that sheet short; this screen is the one place the rest of them can
 * be reached, which is the whole reason it exists.
 */
data class FoodLibraryUiState(
    /** Every food the user owns — authored from the add-entry sheet, or starred there, which is
     * the same row. Not a window either: the search panel is where they are *used*, and this is
     * the only screen that can rename or remove one. */
    val myFoods: List<ScannedProduct> = emptyList(),
    val savedMeals: List<SavedMeal> = emptyList(),
    val recipes: List<Recipe> = emptyList(),
) {
    /** Distinguishes "nothing saved" from "not loaded yet" for the empty state — every list is
     * empty on the first frame, and a mascot that flashes before the rows arrive reads as a bug. */
    val loaded: Boolean get() = myFoods.isNotEmpty() || savedMeals.isNotEmpty() || recipes.isNotEmpty()
}

/** "165 kcal · 100 g" — a food is priced for a stated amount everywhere else in the app, so the
 * row says which amount rather than a bare figure. */
@Composable
fun ScannedProduct.summary(): String =
    stringResource(R.string.profile_library_food_summary, calories, portionLabel(), portionUnit)

/** The macros, for the row's third line — the same job the item names do for a saved meal: a row
 * that only quotes calories is a row you delete blind, and they are already loaded. */
@Composable
fun ScannedProduct.macroLine(): String =
    stringResource(R.string.profile_library_macro_line, proteinG, carbsG, fatG)

/** 100 g, not 100.0 g. */
internal fun ScannedProduct.portionLabel(): String =
    if (portionAmount % 1.0 == 0.0) portionAmount.toInt().toString() else portionAmount.toString()

/** "3 items · 540 kcal" — what the row says a saved meal is. */
@Composable
fun SavedMeal.summary(): String =
    stringResource(
        R.string.profile_library_meal_summary,
        pluralStringResource(R.plurals.profile_library_items, items.size, items.size),
        totalKcal(),
    )

/** A recipe is priced per serving everywhere it is logged, so it is priced per serving here too —
 * the total would be a number the user never eats in one sitting. */
@Composable
fun Recipe.summary(): String =
    stringResource(R.string.profile_library_recipe_summary, perServing().calories, servings)

/** The item names, for the row's third line. A row that only counts its items is a row you delete
 * blind; the items are already loaded, so naming them costs nothing. Empty when there are none,
 * which the row renders as no line at all rather than a blank one. */
fun List<SavedMealItem>.contents(): String = joinToString { it.name }

sealed interface FoodLibraryEvent {
    data class OnDeleteMyFood(val name: String) : FoodLibraryEvent
    data class OnRenameMyFood(val oldName: String, val newName: String) : FoodLibraryEvent
    data class OnDeleteSavedMeal(val id: Long) : FoodLibraryEvent
    data class OnDeleteRecipe(val id: Long) : FoodLibraryEvent
    data class OnRenameSavedMeal(val id: Long, val name: String) : FoodLibraryEvent
    data class OnRenameRecipe(val id: Long, val name: String) : FoodLibraryEvent
}
