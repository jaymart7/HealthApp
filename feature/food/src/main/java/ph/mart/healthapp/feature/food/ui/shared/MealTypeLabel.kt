package ph.mart.healthapp.feature.food.ui.shared

import androidx.annotation.StringRes
import ph.mart.healthapp.core.data.food.MealType
import ph.mart.healthapp.feature.food.R

/**
 * What each meal slot is called on screen. [MealType] is `:core:data`'s and carries no copy — its
 * `name` is what the diary row, the export and the Google Health push all store, so the display
 * name lives here, beside the six screens that show one. The shape `ActivityLevel.label()` takes
 * in `:feature:profile`.
 */
@StringRes
internal fun MealType.labelRes(): Int = when (this) {
    MealType.Breakfast -> R.string.food_meal_breakfast
    MealType.Lunch -> R.string.food_meal_lunch
    MealType.Dinner -> R.string.food_meal_dinner
    MealType.Snacks -> R.string.food_meal_snacks
}
