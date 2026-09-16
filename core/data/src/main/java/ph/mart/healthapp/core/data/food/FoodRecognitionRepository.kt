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
    val nutrients: Nutrients = Nutrients(),
    val confidence: RecognitionConfidence,
    /** The words the model was unsure about, its own, when [confidence] is [RecognitionConfidence.Low]
     * — "a slice", "a handful". Quoted back on the row so the doubt names itself instead of
     * flagging the whole batch and leaving the user to guess which figure it meant. Null on a
     * confident item, and null on a low one the model declined to explain: the batch notice is
     * still there, so an absent phrase costs nothing. */
    val uncertainAbout: String? = null,
)

/**
 * [NoFoodDetected] is its own state rather than an empty [Success], the call [MealParseResult]
 * makes: "there is nothing edible in that photo" and "the call didn't work" are different answers,
 * and the flow shows a different screen for each.
 */
sealed interface RecognitionResult {
    /** Every distinct food on the plate, most prominent first — a plate is rice *and* chicken
     * *and* greens, and a single-food result is simply a list of one. Already through
     * [loggable], so it is non-empty and every item is worth acting on. */
    data class Success(val foods: List<RecognizedFood>) : RecognitionResult
    data object NoFoodDetected : RecognitionResult
    data object Failed : RecognitionResult
}

interface FoodRecognitionRepository {
    suspend fun recognize(photo: Bitmap): RecognitionResult
}
