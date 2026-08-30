package ph.mart.healthapp.core.data.food

import android.graphics.Bitmap
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.FirebaseAIException
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import org.json.JSONException
import org.json.JSONObject

private const val MODEL_NAME = "gemini-2.5-flash"

private const val PROMPT = """
You are a nutrition-estimation assistant for a food-logging app. Look at the photo and identify
the single most prominent food item.

Respond with the food's name, its estimated portion, and its estimated calories and macros for
that portion, plus its fiber and sugar in grams and its sodium in milligrams for that same
portion. If you are not confident about the identification or the portion estimate, set
confidence to "low"; otherwise "high". If no food is visible in the photo, set foodDetected to
false and leave the other fields at their defaults.
"""

private val RESPONSE_SCHEMA = Schema.obj(
    mapOf(
        "foodDetected" to Schema.boolean(),
        "name" to Schema.string(),
        "portionAmount" to Schema.double(),
        "portionUnit" to Schema.string(description = "e.g. g, oz, cup"),
        "calories" to Schema.integer(),
        "proteinG" to Schema.integer(),
        "carbsG" to Schema.integer(),
        "fatG" to Schema.integer(),
        "fiberG" to Schema.integer(),
        "sugarG" to Schema.integer(),
        "sodiumMg" to Schema.integer(description = "milligrams, not grams"),
        "confidence" to Schema.enumeration(listOf("high", "low")),
    ),
)

/** [org.json.JSONObject] parses the flat 12-field response — no kotlinx-serialization dependency
 * needed for this. */
internal class FoodRecognitionRepositoryImpl : FoodRecognitionRepository {

    private val model = Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(
        modelName = MODEL_NAME,
        generationConfig = generationConfig {
            responseMimeType = "application/json"
            responseSchema = RESPONSE_SCHEMA
        },
    )

    override suspend fun recognize(photo: Bitmap): RecognitionResult = try {
        val response = model.generateContent(content { image(photo); text(PROMPT) })
        parse(response.text)
    } catch (_: FirebaseAIException) {
        RecognitionResult.Failed
    } catch (_: JSONException) {
        RecognitionResult.Failed
    }

    private fun parse(json: String?): RecognitionResult {
        if (json == null) return RecognitionResult.Failed
        val body = JSONObject(json)
        if (!body.optBoolean("foodDetected", false)) return RecognitionResult.NoFoodDetected
        val confidence = if (body.optString("confidence") == "low") {
            RecognitionConfidence.Low
        } else {
            RecognitionConfidence.High
        }
        return RecognitionResult.Success(
            RecognizedFood(
                name = body.getString("name"),
                portionAmount = body.getDouble("portionAmount"),
                portionUnit = body.getString("portionUnit"),
                calories = body.getInt("calories"),
                proteinG = body.getInt("proteinG"),
                carbsG = body.getInt("carbsG"),
                fatG = body.getInt("fatG"),
                // optInt, not getInt: a response that predates these three fields, or omits them
                // for a food the model has nothing to say about, still parses as an estimate.
                fiberG = body.optInt("fiberG"),
                sugarG = body.optInt("sugarG"),
                sodiumMg = body.optInt("sodiumMg"),
                confidence = confidence,
            ),
        )
    }
}
