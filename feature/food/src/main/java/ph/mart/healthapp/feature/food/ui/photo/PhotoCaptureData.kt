package ph.mart.healthapp.feature.food.ui.photo

import android.graphics.Bitmap
import ph.mart.healthapp.core.data.food.FoodEntry
import ph.mart.healthapp.core.data.food.RecognitionResult

sealed interface PhotoCaptureEvent {
    data class OnCapture(val photo: Bitmap) : PhotoCaptureEvent
    data object OnCancelAnalysis : PhotoCaptureEvent
    data class OnLogMeal(val entry: FoodEntry) : PhotoCaptureEvent
}

sealed interface PhotoCaptureSideEffect {
    data class RecognitionFinished(val result: RecognitionResult) : PhotoCaptureSideEffect
    data object MealLogged : PhotoCaptureSideEffect
}
