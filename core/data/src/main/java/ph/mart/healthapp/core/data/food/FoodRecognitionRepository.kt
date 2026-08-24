package ph.mart.healthapp.core.data.food

import android.graphics.Bitmap

enum class RecognitionConfidence { High, Low }

data class RecognizedFood(
    val name: String,
    val portionAmount: Double,
    val portionUnit: String,
    val calories: Int,
    val proteinG: Int,
    val carbsG: Int,
    val fatG: Int,
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
