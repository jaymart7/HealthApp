package ph.mart.healthapp.core.data.food

import android.graphics.Bitmap
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.ThinkingLevel
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import com.google.firebase.ai.type.thinkingConfig
import kotlinx.coroutines.CancellationException
import org.json.JSONObject
import ph.mart.healthapp.core.data.AI_MODEL_NAME
import ph.mart.healthapp.core.data.logAiFailure

private const val PROMPT = """
You are a nutrition-estimation assistant for a food-logging app. Look at the photo and identify
the single most prominent food item.

Respond with the food's name, its estimated portion, and its estimated calories and macros for
that portion, plus its fiber and sugar in grams and its sodium in milligrams for that same
portion. If you are not confident about the identification or the portion estimate, set
confidence to "low"; otherwise "high".

If no food is visible in the photo, set foodDetected to false. Otherwise every number must be
your best estimate for the portion you state — never zero, and never a placeholder. Estimate
rather than decline: the user reviews and corrects every figure before it is logged.
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

    private val model = Firebase.ai(
        backend = GenerativeBackend.googleAI(),
        useLimitedUseAppCheckTokens = true,
    ).generativeModel(
        modelName = AI_MODEL_NAME,
        generationConfig = generationConfig {
            // The one call site `AI_THINKING` invites to raise itself. Every other call in the app
            // states something already known — a figure off the diary, a line of encouragement —
            // while this one estimates: identify the food, judge how much of it is on the plate,
            // recall its figures per unit and scale them. At the floor the model answered often
            // enough with a name and twelve zeroes, which `isLoggable` now catches and this stops
            // producing. Nothing caps output here, so there is no budget to raise alongside it.
            thinkingConfig = thinkingConfig { thinkingLevel = ThinkingLevel.LOW }
            responseMimeType = "application/json"
            responseSchema = RESPONSE_SCHEMA
        },
    )

    override suspend fun recognize(photo: Bitmap): RecognitionResult = try {
        val response = model.generateContent(content { image(photo); text(PROMPT) })
        parse(response.text)
    } catch (e: CancellationException) {
        // Backing out of the screen cancels the scope, and that is not an AI failure: without
        // this the catch below swallows the cancellation and logs a request the user withdrew.
        // The rule `CameraCaptureController` and `CoachRepositoryImpl` already follow.
        throw e
    } catch (e: Exception) {
        logAiFailure("photo recognize", e)
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
        val food = RecognizedFood(
            name = body.getString("name"),
            portionAmount = body.getDouble("portionAmount"),
            portionUnit = body.getString("portionUnit"),
            calories = body.getInt("calories"),
            proteinG = body.getInt("proteinG"),
            carbsG = body.getInt("carbsG"),
            fatG = body.getInt("fatG"),
            // optInt, not getInt: a response that predates these three fields, or omits them
            // for a food the model has nothing to say about, still parses as an estimate.
            //
            // Three, not seven. The four panel nutrients are deliberately absent from the
            // schema: a model asked what calcium is in a photographed plate will produce a
            // number, and a fabricated micronutrient is what the coverage count exists to
            // expose.
            nutrients = Nutrients(
                fiberG = body.optInt("fiberG"),
                sugarG = body.optInt("sugarG"),
                sodiumMg = body.optInt("sodiumMg"),
            ),
            confidence = confidence,
        )
        // A food the model named and then priced at zero is not an estimate — the same judgement
        // the voice parse makes with `loggable()`. Thrown rather than returned so the response
        // that produced it reaches logcat with every other AI failure, instead of a Retry screen
        // that says nothing about why.
        check(food.isLoggable) { "zero-calorie photo estimate: $json" }
        return RecognitionResult.Success(food)
    }
}
