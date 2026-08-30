package ph.mart.healthapp.core.data.food

import kotlin.math.roundToInt

/** A dish authored from its ingredients — "Chili" is six [items] that make [servings] portions.
 * Unlike a [SavedMeal], which re-logs its items as one diary row each, a recipe logs as a *single*
 * row priced at [perServing]: the diary should read "Chili · 1 serving", not list the onions.
 *
 * The two are the same rows in the same table (see `SavedMealEntity`); [servings] is what tells
 * them apart, and it is always at least 1 for a recipe. */
data class Recipe(
    val id: Long,
    val name: String,
    val servings: Int,
    val items: List<SavedMealItem>,
)

/** Same cap and same reason as [MAX_SAVED_MEALS] — the panel sits above the entry form in the
 * add-entry sheet, and a longer list pushes the form off-screen.
 *
 * The newest 5 only, for the same reason and with the same escape hatch:
 * [FoodRepository.observeAllRecipes] and Profile's library screen reach the rest. */
const val MAX_RECIPES = 5

/** What one portion of a [Recipe] costs — the shape a diary entry is built from. */
data class RecipeServing(
    val calories: Int,
    val proteinG: Int,
    val carbsG: Int,
    val fatG: Int,
    val fiberG: Int = 0,
    val sugarG: Int = 0,
    val sodiumMg: Int = 0,
)

fun Recipe.totalKcal(): Int = items.sumOf { it.calories }

/**
 * The whole of the feature's arithmetic: the ingredient totals divided by the number of portions,
 * rounded to whole units because that is what a [FoodEntry] stores.
 *
 * [servings] is coerced to at least 1 rather than trusted — a 0 would be a divide-by-zero, and the
 * honest reading of "makes 0 servings" is "makes one".
 */
fun Recipe.perServing(): RecipeServing {
    val portions = servings.coerceAtLeast(1).toDouble()
    return RecipeServing(
        calories = (items.sumOf { it.calories } / portions).roundToInt(),
        proteinG = (items.sumOf { it.proteinG } / portions).roundToInt(),
        carbsG = (items.sumOf { it.carbsG } / portions).roundToInt(),
        fatG = (items.sumOf { it.fatG } / portions).roundToInt(),
        fiberG = (items.sumOf { it.fiberG } / portions).roundToInt(),
        sugarG = (items.sumOf { it.sugarG } / portions).roundToInt(),
        sodiumMg = (items.sumOf { it.sodiumMg } / portions).roundToInt(),
    )
}
