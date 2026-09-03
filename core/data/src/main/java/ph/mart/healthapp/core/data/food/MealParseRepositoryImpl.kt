package ph.mart.healthapp.core.data.food

import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.FirebaseAIException
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import org.json.JSONArray
import org.json.JSONException

private const val MODEL_NAME = "gemini-2.5-flash"

/** [MAX_PARSED_FOODS] foods with eleven fields each. [loggable] rejects whatever gets past it, but
 * capping here is cheaper than paying for a list that will be thrown away. */
private const val MAX_OUTPUT_TOKENS = 1200

/** [FoodRecognitionRepositoryImpl]'s schema minus `foodDetected` — an empty array says that here,
 * and a per-item flag on a list would need answering item by item. */
private val PARSED_FOOD_SCHEMA = Schema.obj(
    mapOf(
        "name" to Schema.string(description = "the food, as a person would say it"),
        "portionAmount" to Schema.double(),
        "portionUnit" to Schema.string(description = "e.g. g, oz, cup, serving"),
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

/**
 * JSON out and [org.json.JSONArray] in, the call [MealIdeaRepositoryImpl] makes for the same
 * reason: eleven flat fields per item need no kotlinx-serialization dependency.
 *
 * The model is built once and held — nothing about this configuration carries anything about the
 * user, and the only thing that varies per call is the sentence itself.
 */
internal class MealParseRepositoryImpl : MealParseRepository {

    private val model = Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(
        modelName = MODEL_NAME,
        generationConfig = generationConfig {
            maxOutputTokens = MAX_OUTPUT_TOKENS
            responseMimeType = "application/json"
            responseSchema = Schema.array(PARSED_FOOD_SCHEMA)
        },
    )

    override suspend fun parse(text: String): MealParseResult = try {
        val prompt = promptFor(text.take(MAX_PARSE_CHARS))
        val response = model.generateContent(content { text(prompt) })
        val foods = parseFoods(response.text).loggable()
        // An empty list means the sentence named nothing edible — a real answer with its own
        // screen, not a failure to retry.
        if (foods.isEmpty()) MealParseResult.NoFoodFound else MealParseResult.Success(foods)
    } catch (_: FirebaseAIException) {
        // Offline, throttled, App Check refused — all the same to the caller.
        MealParseResult.Failed
    } catch (_: JSONException) {
        MealParseResult.Failed
    }

    private fun parseFoods(json: String?): List<RecognizedFood> {
        if (json == null) return emptyList()
        val array = JSONArray(json)
        return (0 until array.length()).map { index ->
            val body = array.getJSONObject(index)
            RecognizedFood(
                name = body.optString("name"),
                portionAmount = body.optDouble("portionAmount", 1.0),
                portionUnit = body.optString("portionUnit").ifBlank { "serving" },
                calories = body.optInt("calories"),
                proteinG = body.optInt("proteinG"),
                carbsG = body.optInt("carbsG"),
                fatG = body.optInt("fatG"),
                fiberG = body.optInt("fiberG"),
                sugarG = body.optInt("sugarG"),
                sodiumMg = body.optInt("sodiumMg"),
                confidence = if (body.optString("confidence") == "low") {
                    RecognitionConfidence.Low
                } else {
                    RecognitionConfidence.High
                },
            )
        }
    }
}

/**
 * A sentence in, a list of foods out. Two constraints the other prompts carry, plus the one this
 * call needs on its own:
 * - **Only the foods actually named.** A model asked what someone ate will otherwise round the
 *   meal up — the toast gets butter, the coffee gets milk — and every invention is a row the user
 *   has to notice and delete.
 * - **Portions for the quantity said.** "Two eggs" is two, "a large coffee" is large; where no
 *   quantity is given, one ordinary serving.
 * - **No medical advice**, for the reason [promptFor][MealIdeaRepositoryImpl]'s twin gives.
 */
private fun promptFor(sentence: String): String = buildString {
    appendLine(
        "You are a nutrition-estimation assistant for a food-logging app. The user has said or " +
            "typed what they ate. Turn it into a list of foods with estimated nutrition.",
    )
    appendLine()
    appendLine("What they said:")
    appendLine(sentence)
    appendLine()
    appendLine(
        "List one entry per distinct food they named, at most $MAX_PARSED_FOODS, each with a " +
            "realistic portion for the quantity they gave (one ordinary serving where they gave " +
            "none) and its calories and macros for that portion. Include only foods they actually " +
            "named — do not add sides, drinks, condiments or cooking fat they did not mention. " +
            "Set confidence to \"low\" for any item whose portion or identity you are unsure of, " +
            "otherwise \"high\". If they named nothing edible, return an empty array. Give no " +
            "medical advice, no diagnosis, and no supplement or medication suggestions.",
    )
}
