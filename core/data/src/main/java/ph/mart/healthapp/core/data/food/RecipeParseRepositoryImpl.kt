package ph.mart.healthapp.core.data.food

import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import org.json.JSONArray
import org.json.JSONObject
import ph.mart.healthapp.core.data.aiModel
import ph.mart.healthapp.core.data.AI_THINKING
import ph.mart.healthapp.core.data.logAiFailure
import ph.mart.healthapp.core.data.generate

/**
 * [QuickLogRepositoryImpl]'s shape: the ingredients are [RECOGNIZED_FOOD_SCHEMA] nested under one
 * key and read back by [parseRecognizedFoods], so an ingredient estimated here and a food parsed on
 * talk-to-log carry the same twelve fields.
 */
internal class RecipeParseRepositoryImpl : RecipeParseRepository {

    private val model = aiModel(
        generationConfig = generationConfig {
            thinkingConfig = AI_THINKING
            maxOutputTokens = MAX_RECIPE_TOKENS
            responseMimeType = "application/json"
            responseSchema = RECIPE_SCHEMA
        },
    )

    override suspend fun parse(text: String): RecipeParseResult = try {
        val response = model.generate("recipe parse", content { text(promptFor(text.take(MAX_RECIPE_CHARS))) })
        val body = JSONObject(response.text ?: "{}")
        recipeParseResult(
            name = body.optString("name"),
            servings = body.optInt("servings", 1),
            ingredients = parseRecognizedFoods(body.optJSONArray("ingredients") ?: JSONArray()),
            kind = body.optString("kind"),
        )
    } catch (e: CancellationException) {
        // Back out of the fill cancels the call — the rule every other parse here follows.
        throw e
    } catch (e: Exception) {
        logAiFailure("recipe parse", e)
        RecipeParseResult.Failed
    }
}

private val RECIPE_SCHEMA = Schema.obj(
    mapOf(
        "kind" to Schema.enumeration(listOf(PARSE_KIND_FOOD, "recipe")),
        "name" to Schema.string(description = "the dish, as a person would name it"),
        "servings" to Schema.integer(description = "how many portions the whole recipe makes"),
        "ingredients" to Schema.array(RECOGNIZED_FOOD_SCHEMA),
    ),
)

/** [MAX_FOOD_LIST_TOKENS] is [MAX_PARSED_FOODS] items; this is the same rate for a full recipe. */
private const val MAX_RECIPE_TOKENS = MAX_FOOD_LIST_TOKENS * MAX_RECIPE_INGREDIENTS / MAX_PARSED_FOODS + 100

/**
 * The meal parse's prompt turned round on its one load-bearing rule: a list is taken as given, but
 * a dish named on its own has to become its ingredients — that is the whole point of the field.
 * Portions are the recipe's, not a serving's, because `perServing()` does the dividing.
 */
private fun promptFor(text: String): String = buildString {
    appendLine(
        "You are a recipe assistant for a nutrition app. The user has described a food, a dish or " +
            "pasted a recipe to save. Turn it into a recipe: its name, how many servings it makes, " +
            "and its ingredients with estimated nutrition.",
    )
    appendLine()
    appendLine(
        "First decide the kind. If they described one thing eaten as-is — a packaged product, a " +
            "single ingredient, or a dish they gave the nutrition of per portion — set kind to " +
            "\"$PARSE_KIND_FOOD\", servings to 1, and return exactly one ingredient: that thing, " +
            "for one portion, with the figures they gave where they gave them. Otherwise set kind " +
            "to \"recipe\" and follow the rules below.",
    )
    appendLine()
    appendLine("What they wrote:")
    appendLine(text)
    appendLine()
    appendLine(
        "If they listed ingredients, use exactly those, with the amounts they gave — do not add " +
            "any they did not list. If they only named a dish, list the ingredients a typical home " +
            "recipe for it uses. At most $MAX_RECIPE_INGREDIENTS ingredients. Each ingredient's " +
            "portion is the amount used in the whole recipe, and its calories and macros are for " +
            "that amount, not per serving. Servings is the number they said; if they gave none, " +
            "the usual yield of that recipe. Set confidence to \"low\" for any ingredient whose " +
            "amount you had to guess, otherwise \"high\"; on a \"low\" one only, set " +
            "uncertainAbout to their words you could not pin down. " +
            "If what they wrote is not food, return an empty ingredients list. Give no medical " +
            "advice, no diagnosis, and no supplement or medication suggestions.",
    )
}
