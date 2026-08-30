package ph.mart.healthapp.feature.food.ui.photo

import android.graphics.Bitmap
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
    fiberG = fiberG,
    sugarG = sugarG,
    sodiumMg = sodiumMg,
)
