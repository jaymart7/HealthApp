package ph.mart.healthapp.core.data.food

import android.graphics.Bitmap

enum class RecognitionConfidence { High, Low }

/**
 * One food FitPulse identified from something the user gave it — a photo, or a sentence they said
 * or typed. Both are an identification carrying a [confidence], which is why the voice path reuses
 * this rather than adding a fourth ten-field type beside it.
 *
 * It is not a [MealIdea]: that one means "you could eat this", and an idea is not an identification
 * of anything.
 */
data class RecognizedFood(
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
    val confidence: RecognitionConfidence,
)

sealed interface RecognitionResult {
    data class Success(val food: RecognizedFood) : RecognitionResult
    data object NoFoodDetected : RecognitionResult
    data object Failed : RecognitionResult
}

interface FoodRecognitionRepository {
    suspend fun recognize(photo: Bitmap): RecognitionResult
}
