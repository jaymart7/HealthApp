package ph.mart.healthapp.core.data.food

import com.google.firebase.ai.type.Schema
import org.json.JSONArray

/**
 * The wire shape of a [RecognizedFood], and the one place that reads it back.
 *
 * Two features ask a model to identify food and both want the same twelve flat fields: the photo
 * flow, which reads a plate, and talk-to-log, which reads a sentence. They used to declare a schema
 * and a parser each — the photo one a single object, the parse one an array of them — which was a
 * real difference right up until a photographed plate stopped being one food. It isn't, so this is
 * one file rather than two copies drifting apart.
 *
 * Here rather than in [MealParse.kt][loggable] because that file is pure and JVM-tested;
 * [org.json] is stubbed on the JVM, so the parse below cannot be tested there and the judgement it
 * feeds ([loggable]) deliberately can.
 */
internal val RECOGNIZED_FOOD_SCHEMA = Schema.obj(
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
        "uncertainAbout" to Schema.string(
            description = "only when confidence is low: the words from the input you were unsure " +
                "about, quoted as they appeared",
        ),
    ),
    // The one optional property. Required, a model with nothing to say here says something
    // anyway — and an invented doubt on a confident item is worse than no chip at all.
    optionalProperties = listOf("uncertainAbout"),
)

/**
 * [MAX_PARSED_FOODS] foods with twelve fields each. [loggable] rejects whatever gets past it, but
 * capping here is cheaper than paying for a list that will be thrown away.
 */
internal const val MAX_FOOD_LIST_TOKENS = 1400

/**
 * The array in, foods out. Every read is an `opt*` with a default: a response missing a field is a
 * shorter answer, not a failure, and [loggable] is the thing that decides what is worth acting on.
 *
 * An empty list is a real answer — "nothing edible here" — which is why there is no `foodDetected`
 * flag in the schema above. A flag on a list would have to be answered item by item.
 */
internal fun parseRecognizedFoods(json: String?): List<RecognizedFood> =
    if (json == null) emptyList() else parseRecognizedFoods(JSONArray(json))

/** The same read over an array already parsed — the quick log's reply nests one under `foods`. */
internal fun parseRecognizedFoods(array: JSONArray): List<RecognizedFood> =
    (0 until array.length()).map { index ->
        val body = array.getJSONObject(index)
        RecognizedFood(
            name = body.optString("name"),
            portionAmount = body.optDouble("portionAmount", 1.0),
            // Stays in Kotlin: a persisted, compared portion unit, not copy — `SERVING_UNIT`
            // in :feature:food switches on this exact string.
            portionUnit = body.optString("portionUnit").ifBlank { "serving" },
            calories = body.optInt("calories"),
            proteinG = body.optInt("proteinG"),
            carbsG = body.optInt("carbsG"),
            fatG = body.optInt("fatG"),
            // Only the three the model is asked for. Vitamin D, calcium, iron and potassium are
            // deliberately absent from the schema: a model asked to estimate the calcium in a
            // photographed plate will produce a number, and a fabricated micronutrient is exactly
            // what the coverage count exists to expose.
            nutrients = Nutrients(
                fiberG = body.optInt("fiberG"),
                sugarG = body.optInt("sugarG"),
                sodiumMg = body.optInt("sodiumMg"),
            ),
            confidence = if (body.optString("confidence") == "low") {
                RecognitionConfidence.Low
            } else {
                RecognitionConfidence.High
            },
            // Optional in the schema, so absent is the ordinary case and not a short answer:
            // blank and missing are the same thing, which is no phrase to quote.
            uncertainAbout = body.optString("uncertainAbout").ifBlank { null },
        )
    }
