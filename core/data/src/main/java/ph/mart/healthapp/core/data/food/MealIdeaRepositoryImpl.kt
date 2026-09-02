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
import ph.mart.healthapp.core.data.profile.DietaryPreference

private const val MODEL_NAME = "gemini-2.5-flash"

/** Three foods with ten fields each. [fitting] rejects whatever gets past it, but capping here is
 * cheaper than paying for a list that will be thrown away. */
private const val MAX_OUTPUT_TOKENS = 600

private val IDEA_SCHEMA = Schema.obj(
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
    ),
)

/**
 * JSON out, like the photo path and unlike the insight and the coach: this answer is three foods
 * the app has to price, portion and log, not a sentence it has to print. [org.json.JSONArray]
 * parses it — no kotlinx-serialization dependency needed for ten flat fields, the same call
 * [FoodRecognitionRepositoryImpl] makes.
 *
 * The model is built once and held, unlike the coach's: nothing about this configuration carries
 * the day's numbers — those ride the prompt, which is written fresh per request.
 */
internal class MealIdeaRepositoryImpl : MealIdeaRepository {

    private val model = Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(
        modelName = MODEL_NAME,
        generationConfig = generationConfig {
            maxOutputTokens = MAX_OUTPUT_TOKENS
            responseMimeType = "application/json"
            responseSchema = Schema.array(IDEA_SCHEMA)
        },
    )

    override suspend fun ideas(request: MealIdeaRequest): MealIdeaResult = try {
        val response = model.generateContent(content { text(promptFor(request)) })
        val ideas = parse(response.text).fitting(request.remainingKcal)
        // An empty list is a failure, not an answer: the screen's fallback — the user's own foods —
        // is better than a heading over nothing.
        if (ideas.isEmpty()) MealIdeaResult.Failed else MealIdeaResult.Success(ideas)
    } catch (_: FirebaseAIException) {
        // Offline, throttled, App Check refused — all the same to the caller.
        MealIdeaResult.Failed
    } catch (_: JSONException) {
        MealIdeaResult.Failed
    }

    private fun parse(json: String?): List<MealIdea> {
        if (json == null) return emptyList()
        val array = JSONArray(json)
        return (0 until array.length()).map { index ->
            val body = array.getJSONObject(index)
            MealIdea(
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
            )
        }
    }
}

/**
 * Gaps in, three foods out. The constraints are the two the other prompts carry, for the same
 * reasons, plus one this call needs on its own:
 * - **No medical advice** — FitPulse tracks food and bodies; a model asked what to eat will
 *   otherwise reach for supplements and symptoms.
 * - **Only the numbers given** — the screen sits over a diary that will contradict anything else.
 * - **Ordinary food, real portions** — the answer has to be loggable and cookable tonight, not a
 *   menu item. A "quinoa and pomegranate bowl" is a recipe, not an idea.
 */
private fun promptFor(request: MealIdeaRequest): String = buildString {
    appendLine(
        "You are a nutrition coach suggesting what someone could eat for their next meal in a " +
            "food-tracking app.",
    )
    appendLine()
    appendLine("The meal is ${request.mealType.name.lowercase()}, for a user whose goal is ${request.goal.name.lowercase()} weight.")
    appendLine("What is left of their day:")
    appendLine("- Calories: ${request.remainingKcal} kcal")
    appendLine("- Protein: ${request.remainingProteinG} g")
    appendLine("- Carbs: ${request.remainingCarbsG} g")
    appendLine("- Fat: ${request.remainingFatG} g")
    request.dietLine()?.let(::appendLine)
    appendLine()
    appendLine(
        "Suggest exactly $MAX_MEAL_IDEAS ordinary foods or simple meals that fit within those " +
            "calories, with a realistic portion for each and its calories and macros for that " +
            "portion. Prefer things that help close the protein gap. Nothing needing more than a " +
            "few common ingredients. Base it only on the numbers above: give no medical advice, " +
            "no diagnosis, and no supplement or medication suggestions.",
    )
}

/** Null for `None` and for a profile that never answered — a line saying "no restrictions" is one
 * more thing for the model to over-read. */
private fun MealIdeaRequest.dietLine(): String? = when (diet) {
    DietaryPreference.Vegetarian -> "They are vegetarian: no meat and no fish."
    DietaryPreference.Vegan -> "They are vegan: no animal products at all."
    // "Other" is the onboarding option for a diet FitPulse never asked them to name, so the
    // honest instruction is to stay unremarkable rather than to guess at what it is.
    DietaryPreference.Other -> "They follow a dietary restriction they have not described: keep " +
        "suggestions plain and easy to swap an ingredient out of."
    DietaryPreference.None, null -> null
}
