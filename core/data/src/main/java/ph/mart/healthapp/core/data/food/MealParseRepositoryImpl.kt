package ph.mart.healthapp.core.data.food

import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import ph.mart.healthapp.core.data.AI_MODEL_NAME
import ph.mart.healthapp.core.data.AI_THINKING
import ph.mart.healthapp.core.data.logAiFailure

/**
 * JSON out and [org.json.JSONArray] in, the call [MealIdeaRepositoryImpl] makes for the same
 * reason: eleven flat fields per item need no kotlinx-serialization dependency. The schema and the
 * read-back are [RECOGNIZED_FOOD_SCHEMA] and [parseRecognizedFoods], shared with the photo flow:
 * a sentence and a plate are both "identify the food and price it", and the two stopped differing
 * at all when a photographed plate stopped being a single food.
 *
 * The model is built once and held — nothing about this configuration carries anything about the
 * user, and the only thing that varies per call is the sentence itself.
 */
internal class MealParseRepositoryImpl : MealParseRepository {

    private val model = Firebase.ai(
        backend = GenerativeBackend.googleAI(),
        useLimitedUseAppCheckTokens = true,
    ).generativeModel(
        modelName = AI_MODEL_NAME,
        generationConfig = generationConfig {
            thinkingConfig = AI_THINKING
            maxOutputTokens = MAX_FOOD_LIST_TOKENS
            responseMimeType = "application/json"
            responseSchema = Schema.array(RECOGNIZED_FOOD_SCHEMA)
        },
    )

    override suspend fun parse(text: String): MealParseResult = try {
        val prompt = promptFor(text.take(MAX_PARSE_CHARS))
        val response = model.generateContent(content { text(prompt) })
        val foods = parseRecognizedFoods(response.text).loggable()
        // An empty list means the sentence named nothing edible — a real answer with its own
        // screen, not a failure to retry.
        if (foods.isEmpty()) MealParseResult.NoFoodFound else MealParseResult.Success(foods)
    } catch (e: CancellationException) {
        // Backing out of the screen cancels the scope, and that is not an AI failure: without
        // this the catch below swallows the cancellation and logs a request the user withdrew.
        // The rule `CameraCaptureController` and `CoachRepositoryImpl` already follow.
        throw e
    } catch (e: Exception) {
        logAiFailure("meal parse", e)
        // Offline, throttled, App Check refused — all the same to the caller.
        MealParseResult.Failed
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
            "otherwise \"high\". On a \"low\" item only, set uncertainAbout to the words of " +
            "theirs you could not pin down, quoted as they said them — \"a slice\", \"a " +
            "handful\". Leave it out entirely on a \"high\" one. " +
            "If they named nothing edible, return an empty array. Give no " +
            "medical advice, no diagnosis, and no supplement or medication suggestions.",
    )
}
