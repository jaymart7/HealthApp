package ph.mart.healthapp.feature.profile.ui.library

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import ph.mart.healthapp.core.data.food.Recipe
import ph.mart.healthapp.core.data.food.SavedMeal
import ph.mart.healthapp.core.data.food.ScannedProduct
import ph.mart.healthapp.core.data.food.perServing
import ph.mart.healthapp.core.data.food.totalKcal
import ph.mart.healthapp.feature.profile.R
import ph.mart.healthapp.feature.profile.ui.shared.components.Figure

/** Not copy — a unit symbol, the same one every calorie figure in the app prints. */
private const val KCAL = "kcal"

/**
 * Every food, saved meal and recipe the user owns — not the newest-N windows the add-entry sheet's
 * panels read. Those windows exist to keep that sheet short; this screen is the one place the rest
 * of them can be reached.
 */
data class FoodLibraryUiState(
    val myFoods: List<ScannedProduct> = emptyList(),
    val savedMeals: List<SavedMeal> = emptyList(),
    val recipes: List<Recipe> = emptyList(),
) {
    /** Distinguishes "nothing saved" from "not loaded yet" for the empty state — every list is
     * empty on the first frame, and a mascot that flashes before the rows arrive reads as a bug. */
    val loaded: Boolean get() = myFoods.isNotEmpty() || savedMeals.isNotEmpty() || recipes.isNotEmpty()
}

/** The chips over the list. [All] is where the screen opens. */
enum class LibraryFilter { All, Foods, Recipes, Meals }

/** One row of the single list — any of the three things, and where a tap on it goes. */
sealed interface LibraryEntry {
    val name: String

    /** Prefixed, because saved meals and recipes are one table and share an id space, and a food's
     * key is its name — without the prefix two rows could collide and Lazy reuse the wrong slot. */
    val key: String

    data class Food(val food: ScannedProduct) : LibraryEntry {
        override val name get() = food.name
        override val key get() = "food-$name"
    }

    data class Dish(val recipe: Recipe) : LibraryEntry {
        override val name get() = recipe.name
        override val key get() = "saved-${recipe.id}"
    }

    data class Meal(val meal: SavedMeal) : LibraryEntry {
        override val name get() = meal.name
        override val key get() = "saved-${meal.id}"
    }
}

/**
 * The chips worth drawing: one per kind the user actually has, behind All. A library holding one
 * kind has nothing to filter, so it gets none — a chip that can only ever show everything is chrome.
 */
fun FoodLibraryUiState.filters(): List<LibraryFilter> {
    val kinds = listOfNotNull(
        LibraryFilter.Foods.takeIf { myFoods.isNotEmpty() },
        LibraryFilter.Recipes.takeIf { recipes.isNotEmpty() },
        LibraryFilter.Meals.takeIf { savedMeals.isNotEmpty() },
    )
    return if (kinds.size < 2) emptyList() else listOf(LibraryFilter.All) + kinds
}

/**
 * The list the screen draws: the kinds [filter] lets through, whose **name** holds [query], A→Z.
 *
 * Alphabetical because a food has no date to sort by — `favorite_food` is keyed by name — and a
 * column for one would wipe every install under `fallbackToDestructiveMigration`. Name only, since
 * the row no longer prints its contents: a hit on a line nobody can see reads as a wrong answer.
 *
 * Pure and not `@Composable`: the half of the screen worth a JVM test, and not debounced because
 * all three lists are already in memory, so a keystroke costs a filter, not a query.
 */
fun FoodLibraryUiState.entries(filter: LibraryFilter, query: String): List<LibraryEntry> {
    val needle = query.trim()
    val all = buildList {
        if (filter == LibraryFilter.All || filter == LibraryFilter.Foods) myFoods.forEach { add(LibraryEntry.Food(it)) }
        if (filter == LibraryFilter.All || filter == LibraryFilter.Recipes) recipes.forEach { add(LibraryEntry.Dish(it)) }
        if (filter == LibraryFilter.All || filter == LibraryFilter.Meals) savedMeals.forEach { add(LibraryEntry.Meal(it)) }
    }
    return all
        .filter { needle.isEmpty() || it.name.contains(needle, ignoreCase = true) }
        .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })
}

/** "120 kcal / 30 g" — a food is priced for a stated amount everywhere else in the app, so the
 * row says which amount rather than a bare figure. */
internal fun ScannedProduct.figures(): List<Figure> =
    listOf(Figure(calories.toString(), KCAL), Figure(portionLabel(), portionUnit))

/** 100 g, not 100.0 g. */
internal fun ScannedProduct.portionLabel(): String =
    if (portionAmount % 1.0 == 0.0) portionAmount.toInt().toString() else portionAmount.toString()

/** "380 kcal / 2 items" — calories lead on every row, so the column of numbers scans. */
@Composable
internal fun SavedMeal.figures(): List<Figure> = listOf(
    Figure(totalKcal().toString(), KCAL),
    Figure(items.size.toString(), pluralStringResource(R.plurals.profile_library_items_unit, items.size)),
)

/** "480 kcal per serving / 4 servings" — a recipe is logged a serving at a time, so it is priced
 * per serving here too; the yield is what makes it a recipe and not a food. */
@Composable
internal fun Recipe.figures(): List<Figure> = listOf(
    Figure(perServing().calories.toString(), stringResource(R.string.profile_library_per_serving)),
    Figure(servings.toString(), pluralStringResource(R.plurals.profile_library_servings_unit, servings)),
)
