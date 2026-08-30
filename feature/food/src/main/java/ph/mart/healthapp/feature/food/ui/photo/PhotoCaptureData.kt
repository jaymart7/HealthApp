package ph.mart.healthapp.feature.food.ui.photo

import android.graphics.Bitmap
import java.util.Calendar
import ph.mart.healthapp.core.data.food.FoodEntry
import ph.mart.healthapp.core.data.food.MealType
import ph.mart.healthapp.core.data.food.RecognitionResult
import ph.mart.healthapp.core.data.food.RecognizedFood
import ph.mart.healthapp.feature.food.ui.shared.AddEntryForm

sealed interface PhotoCaptureEvent {
    data class OnCapture(val photo: Bitmap) : PhotoCaptureEvent
    data object OnCancelAnalysis : PhotoCaptureEvent
    data class OnLogMeal(val entry: FoodEntry) : PhotoCaptureEvent
}

sealed interface PhotoCaptureSideEffect {
    data class RecognitionFinished(val result: RecognitionResult) : PhotoCaptureSideEffect
    data object MealLogged : PhotoCaptureSideEffect
}

fun RecognizedFood.toAddEntryForm(mealType: MealType): AddEntryForm = AddEntryForm(
    mealType = mealType,
    name = name,
    portionAmount = portionAmount,
    portionUnit = portionUnit,
    calories = calories,
    proteinG = proteinG,
    carbsG = carbsG,
    fatG = fatG,
)

/** Same time-of-day heuristic the prototype used to preselect a meal type, ported to [Calendar]
 * (matches [ph.mart.healthapp.core.data.food.FoodRepositoryImpl]'s style — no core-library
 * desugaring in this project yet, so no `java.time`). */
fun defaultMealTypeForNow(): MealType {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when {
        hour < 11 -> MealType.Breakfast
        hour < 15 -> MealType.Lunch
        hour < 21 -> MealType.Dinner
        else -> MealType.Snacks
    }
}
