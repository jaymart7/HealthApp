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
import ph.mart.healthapp.feature.profile.ui.shared.components.Figure

/** Not copy — a unit symbol, the same one every calorie figure in the app prints. */
private const val KCAL = "kcal"

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

    val total: Int get() = myFoods.size + savedMeals.size + recipes.size
}

/** The three sections after the query has run over them. The same shape as the state it came
 * from, so the list renders one thing whether or not anything is being searched for. */
data class LibraryResults(
    val myFoods: List<ScannedProduct> = emptyList(),
    val savedMeals: List<SavedMeal> = emptyList(),
    val recipes: List<Recipe> = emptyList(),
) {
    val total: Int get() = myFoods.size + savedMeals.size + recipes.size
}

/**
 * Filters all three sections at once, keeping the grouping. A section with no matches ends up
 * empty and so loses its header — the same rule the screen already applies to a section that was
 * empty to begin with.
 *
 * Matching is case-insensitive and runs over the **contents line as well as the name**, so "oat"
 * finds a saved meal whose own name never mentions oats. It deliberately does not match kcal or
 * macro figures: "high protein" is a filter feature, not a search.
 *
 * A food has no contents line — its detail slot is the macro triplet — so it matches on name
 * alone, which is also the only text it has.
 *
 * Pure and not `@Composable`: this is the half of search worth a JVM test, and it is not
 * debounced because there is nothing to debounce. All three lists are already in memory, so a
 * keystroke costs a filter over them, not the Room read `HistorySearchField` throttles.
 */
fun FoodLibraryUiState.filter(query: String): LibraryResults {
    val needle = query.trim()
    if (needle.isEmpty()) return LibraryResults(myFoods, savedMeals, recipes)
    return LibraryResults(
        myFoods = myFoods.filter { it.name.matches(needle) },
        savedMeals = savedMeals.filter { it.name.matches(needle) || it.items.contents().matches(needle) },
        recipes = recipes.filter { it.name.matches(needle) || it.items.contents().matches(needle) },
    )
}

private fun String.matches(needle: String): Boolean = contains(needle, ignoreCase = true)

/** "420 kcal / 1 serving" — a food is priced for a stated amount everywhere else in the app, so
 * the row says which amount rather than a bare figure. A unit price, which is what a food is. */
internal fun ScannedProduct.figures(): List<Figure> =
    listOf(Figure(calories.toString(), KCAL), Figure(portionLabel(), portionUnit))

/** 100 g, not 100.0 g. */
internal fun ScannedProduct.portionLabel(): String =
    if (portionAmount % 1.0 == 0.0) portionAmount.toInt().toString() else portionAmount.toString()

/** "3 items / 540 kcal" — a saved meal is a bundle, so it leads with how many things are in it. */
@Composable
internal fun SavedMeal.figures(): List<Figure> = listOf(
    Figure(items.size.toString(), pluralStringResource(R.plurals.profile_library_items_unit, items.size)),
    Figure(totalKcal().toString(), KCAL),
)

/** A recipe is priced per serving everywhere it is logged, so it is priced per serving here too —
 * the total would be a number the user never eats in one sitting. The yield rides its own pill
 * beside this, because "makes 4" is the thing that makes a recipe not a food. */
@Composable
internal fun Recipe.figures(): List<Figure> =
    listOf(Figure(perServing().calories.toString(), stringResource(R.string.profile_library_per_serving)))

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
