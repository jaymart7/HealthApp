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
 * ponytail: the newest 5 only, so a 6th recipe pushes the oldest out of view (and out of reach of
 * its own delete button). Add a manage-recipes list in Profile if anyone keeps more. */
const val MAX_RECIPES = 5

/** What one portion of a [Recipe] costs — the shape a diary entry is built from. */
data class RecipeServing(
    val calories: Int,
    val proteinG: Int,
    val carbsG: Int,
    val fatG: Int,
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
    )
}
