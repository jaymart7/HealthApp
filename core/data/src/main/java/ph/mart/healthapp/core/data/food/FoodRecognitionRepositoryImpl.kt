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
import ph.mart.healthapp.core.data.AI_MODEL_NAME
import ph.mart.healthapp.core.data.logAiFailure

/**
 * A plate in, the foods on it out.
 *
 * The two constraints that matter are the ones a list makes possible and a list makes dangerous.
 * **Every distinct food**, because a meal is rice *and* chicken *and* greens and the old prompt's
 * "single most prominent item" threw two thirds of a lunch away. But **only what is actually
 * visible**, the rule `MealParseRepositoryImpl`'s prompt already carries and which matters more
 * here: a model listing a plate will otherwise reach for the cooking oil it assumes and the
 * garnish it expects, and every invention is a row the user has to notice and delete.
 */
private val PROMPT = """
You are a nutrition-estimation assistant for a food-logging app. Look at the photo and list every
distinct food you can see, most prominent first, at most $MAX_PARSED_FOODS of them.

Give each one its name, its estimated portion as it appears in the photo, and its estimated
calories and macros for that portion, plus its fiber and sugar in grams and its sodium in
milligrams for that same portion.

List only food you can actually see. Do not add sides, drinks, condiments, garnishes or cooking
fat you cannot see in the photo. Foods that are plainly one dish stay one entry — a sandwich is a
sandwich, not bread plus filling.

Set confidence to "low" for any item whose identity or portion you are unsure of, otherwise
"high". If there is no food in the photo at all, return an empty array. Otherwise every number
must be your best estimate for the portion you state — never zero, and never a placeholder.
Estimate rather than decline: the user reviews and corrects every figure before it is logged.
"""

/**
 * [MAX_FOOD_LIST_TOKENS] plus headroom, and the headroom is not optional. This is the one call site
 * in the app on [ThinkingLevel.LOW] rather than `AI_THINKING`, and `Ai.kt` documents what that
 * costs: thinking tokens are spent from `maxOutputTokens`, so a budget sized for the answer alone
 * finishes on `MAX_TOKENS` with nothing in it and `validate()` discards the lot.
 */
private const val MAX_OUTPUT_TOKENS = 1600

/** [org.json.JSONArray] parses the response — see [parseRecognizedFoods], which the meal parse
 * shares. */
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
            // enough with a name and twelve zeroes, which `loggable()` now catches and this stops
            // producing. Doing it several times over a plate is more of the same work, not
            // different work, so the level holds and the budget above is what moved.
            thinkingConfig = thinkingConfig { thinkingLevel = ThinkingLevel.LOW }
            maxOutputTokens = MAX_OUTPUT_TOKENS
            responseMimeType = "application/json"
            responseSchema = Schema.array(RECOGNIZED_FOOD_SCHEMA)
        },
    )

    override suspend fun recognize(photo: Bitmap): RecognitionResult = try {
        val response = model.generateContent(content { image(photo); text(PROMPT) })
        val json = response.text
        val foods = parseRecognizedFoods(json).loggable()
        if (foods.isEmpty()) {
            // A plate the model named and then priced at zero is not an estimate, and it is not
            // "no food" either — it is the model declining while appearing to answer. Both land on
            // the search screen, so this is the only thing that tells them apart afterwards.
            if (!json.isNullOrBlank() && json.trim() != "[]") {
                logAiFailure("photo recognize", IllegalStateException("no loggable estimate: $json"))
            }
            RecognitionResult.NoFoodDetected
        } else {
            RecognitionResult.Success(foods)
        }
    } catch (e: CancellationException) {
        // Backing out of the screen cancels the scope, and that is not an AI failure: without
        // this the catch below swallows the cancellation and logs a request the user withdrew.
        // The rule `CameraCaptureController` and `CoachRepositoryImpl` already follow.
        throw e
    } catch (e: Exception) {
        logAiFailure("photo recognize", e)
        RecognitionResult.Failed
    }
}
