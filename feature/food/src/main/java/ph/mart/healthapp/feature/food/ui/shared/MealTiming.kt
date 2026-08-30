package ph.mart.healthapp.feature.food.ui.shared

import java.util.Calendar
import ph.mart.healthapp.core.data.food.MealType

/**
 * Same time-of-day heuristic the prototype used to preselect a meal type, ported to [Calendar]
 * (matches [ph.mart.healthapp.core.data.food.FoodRepositoryImpl]'s style — no core-library
 * desugaring in this project yet, so no `java.time`).
 *
 * In `shared/` because the photo flow and the barcode flow both seed their form with it, and a
 * helper two flows reach for does not live in whichever one declared it first.
 */
fun defaultMealTypeForNow(): MealType {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when {
        hour < 11 -> MealType.Breakfast
        hour < 15 -> MealType.Lunch
        hour < 21 -> MealType.Dinner
        else -> MealType.Snacks
    }
}
