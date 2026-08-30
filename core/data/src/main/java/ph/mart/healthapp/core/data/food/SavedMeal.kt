package ph.mart.healthapp.core.data.food

/** A named snapshot of what was logged to one meal section — "Usual breakfast" is the four items
 * that were in Breakfast the day it was saved. Re-logging inserts a copy of every [items] entry
 * into whichever meal slot the add-entry sheet is open on; the saved meal itself is never touched
 * by logging, and deleting it leaves the already-logged entries alone. */
data class SavedMeal(
    val id: Long,
    val name: String,
    val items: List<SavedMealItem>,
)

/** One food inside a [SavedMeal] — the same fields a [FoodEntry] carries, minus the day and meal
 * slot, which are supplied at log time. */
data class SavedMealItem(
    val name: String,
    val portionAmount: Double,
    val portionUnit: String,
    val calories: Int,
    val proteinG: Int,
    val carbsG: Int,
    val fatG: Int,
    val fiberG: Int = 0,
    val sugarG: Int = 0,
    val sodiumMg: Int = 0,
)

fun SavedMeal.totalKcal(): Int = items.sumOf { it.calories }

/** Same cap and same reason as [MAX_SUGGESTIONS]: the panel sits above the entry form in the
 * add-entry sheet, and a longer list pushes the form off-screen.
 *
 * The newest 5 only — a 6th saved meal is out of view *here*, which is what
 * [FoodRepository.observeAllSavedMeals] and Profile's library screen exist for: this window is a
 * display choice about sheet height, not a cap on what the user can reach. */
const val MAX_SAVED_MEALS = 5
