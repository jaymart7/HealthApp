package ph.mart.healthapp.feature.profile.ui.library

import ph.mart.healthapp.core.data.food.Recipe
import ph.mart.healthapp.core.data.food.SavedMeal
import ph.mart.healthapp.core.data.food.SavedMealItem
import ph.mart.healthapp.core.data.food.perServing
import ph.mart.healthapp.core.data.food.totalKcal

/**
 * Every saved meal and every recipe — not the newest-N windows the add-entry sheet's panels read.
 * Those windows exist to keep that sheet short; this screen is the one place the rest of them can
 * be reached, which is the whole reason it exists.
 */
data class FoodLibraryUiState(
    val savedMeals: List<SavedMeal> = emptyList(),
    val recipes: List<Recipe> = emptyList(),
) {
    /** Distinguishes "nothing saved" from "not loaded yet" for the empty state — both lists are
     * empty on the first frame, and a mascot that flashes before the rows arrive reads as a bug. */
    val loaded: Boolean get() = savedMeals.isNotEmpty() || recipes.isNotEmpty()
}

/** "3 items · 540 kcal" — what the row says a saved meal is. */
fun SavedMeal.summary(): String =
    "${items.size} ${if (items.size == 1) "item" else "items"} · ${totalKcal()} kcal"

/** A recipe is priced per serving everywhere it is logged, so it is priced per serving here too —
 * the total would be a number the user never eats in one sitting. */
fun Recipe.summary(): String = "${perServing().calories} kcal per serving · makes $servings"

/** The item names, for the row's third line. A row that only counts its items is a row you delete
 * blind; the items are already loaded, so naming them costs nothing. Empty when there are none,
 * which the row renders as no line at all rather than a blank one. */
fun List<SavedMealItem>.contents(): String = joinToString { it.name }

sealed interface FoodLibraryEvent {
    data class OnDeleteSavedMeal(val id: Long) : FoodLibraryEvent
    data class OnDeleteRecipe(val id: Long) : FoodLibraryEvent
    data class OnRenameSavedMeal(val id: Long, val name: String) : FoodLibraryEvent
    data class OnRenameRecipe(val id: Long, val name: String) : FoodLibraryEvent
}
