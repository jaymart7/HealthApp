package ph.mart.healthapp.feature.food.ui.photo

import android.graphics.Bitmap
import ph.mart.healthapp.core.data.food.FoodEntry
import ph.mart.healthapp.core.data.food.RecognitionResult

sealed interface PhotoCaptureEvent {
    data class OnCapture(val photo: Bitmap) : PhotoCaptureEvent
    data object OnCancelAnalysis : PhotoCaptureEvent
    /** [photo] is whatever the flow is holding — the plate that was recognized, the one picked
     * from the gallery, or the one that failed to analyze and was typed in by hand. All three are
     * a picture of the meal being logged, which is the whole rule; null is the flow reached
     * without a capture at all. */
    data class OnLogMeal(val entry: FoodEntry, val photo: Bitmap?) : PhotoCaptureEvent
}

sealed interface PhotoCaptureSideEffect {
    data class RecognitionFinished(val result: RecognitionResult) : PhotoCaptureSideEffect
    data object MealLogged : PhotoCaptureSideEffect
}
