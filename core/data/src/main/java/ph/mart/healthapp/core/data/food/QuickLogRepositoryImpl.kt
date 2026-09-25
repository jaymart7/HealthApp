package ph.mart.healthapp.core.data.food

import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import org.json.JSONArray
import org.json.JSONObject
import ph.mart.healthapp.core.data.AI_MODEL_NAME
import ph.mart.healthapp.core.data.AI_THINKING
import ph.mart.healthapp.core.data.exercise.PARSED_EXERCISE_SCHEMA
import ph.mart.healthapp.core.data.exercise.readActivity
import ph.mart.healthapp.core.data.logAiFailure

/**
 * [MealParseRepositoryImpl]'s shape with both parses' schemas nested in one reply: the food items
 * are [RECOGNIZED_FOOD_SCHEMA] and the activities are `PARSED_EXERCISE_SCHEMA`, read back by the
 * same two functions, so a meal typed here and one typed on talk-to-log land identically.
 */
internal class QuickLogRepositoryImpl : QuickLogRepository {

    private val model = Firebase.ai(
        backend = GenerativeBackend.googleAI(),
        useLimitedUseAppCheckTokens = true,
    ).generativeModel(
        modelName = AI_MODEL_NAME,
        generationConfig = generationConfig {
            thinkingConfig = AI_THINKING
            maxOutputTokens = MAX_QUICK_LOG_TOKENS
            responseMimeType = "application/json"
            responseSchema = QUICK_LOG_SCHEMA
        },
    )

    override suspend fun parse(turns: List<QuickLogTurn>): QuickLogResult = try {
        val mayAsk = turns.mayAsk()
        val response = model.generateContent(content { text(promptFor(turns, mayAsk)) })
        val body = JSONObject(response.text ?: "{}")
        quickLogResult(
            question = body.optString("question"),
            foods = parseRecognizedFoods(body.optJSONArray("foods") ?: JSONArray()),
            activities = body.optJSONArray("activities")?.let { array ->
                (0 until array.length()).map { readActivity(array.getJSONObject(it)) }
            }.orEmpty(),
            mayAsk = mayAsk,
            mealType = body.optString("mealType"),
            waterGlasses = body.optInt("waterGlasses").takeIf { body.has("waterGlasses") },
            weight = body.optDouble("weight").takeIf { body.has("weight") },
        )
    } catch (e: CancellationException) {
        // Dismissing the sheet cancels the call — the rule every other parse here follows.
        throw e
    } catch (e: Exception) {
        logAiFailure("quick log", e)
        QuickLogResult.Failed
    }
}

/** Both lists are required and may be empty; the question and the meal slot are optional, for
 * `uncertainAbout`'s reason — required, a model with nothing to say there says something anyway. */
private val QUICK_LOG_SCHEMA = Schema.obj(
    mapOf(
        "question" to Schema.string(
            description = "One short follow-up question, only when allowed and truly needed",
        ),
        "foods" to Schema.array(RECOGNIZED_FOOD_SCHEMA),
        "activities" to Schema.array(PARSED_EXERCISE_SCHEMA),
        "mealType" to Schema.enumeration(
            values = MealType.entries.map { it.name },
            description = "Only when they named the meal, e.g. \"for lunch\"",
        ),
        "waterGlasses" to Schema.integer(
            description = "Glasses of plain water they drank, only when they said so",
        ),
        "weight" to Schema.double(
            description = "Their body weight, the number exactly as they said it, only when they " +
                "stated what they weigh",
        ),
    ),
    optionalProperties = listOf("question", "mealType", "waterGlasses", "weight"),
)

/** A full food list plus a few activities and a question. */
private const val MAX_QUICK_LOG_TOKENS = MAX_FOOD_LIST_TOKENS + 400

/**
 * The first turn is the meal, so it is always kept; the rest are the latest, where the answers and
 * corrections are. Each is cut to [MAX_PARSE_CHARS] for that constant's reason.
 */
private fun promptFor(turns: List<QuickLogTurn>, mayAsk: Boolean): String = buildString {
    appendLine(
        "You are a logging assistant for a nutrition and fitness app. The user has typed what " +
            "they ate, what exercise they did, or both. Turn it into foods with estimated " +
            "nutrition and activities with durations.",
    )
    appendLine()
    appendLine("The conversation so far:")
    val kept = if (turns.size <= MAX_QUICK_LOG_TURNS) {
        turns
    } else {
        listOf(turns.first()) + turns.takeLast(MAX_QUICK_LOG_TURNS - 1)
    }
    kept.forEach { turn ->
        append(if (turn.fromUser) "User: " else "You asked: ")
        appendLine(turn.text.take(MAX_PARSE_CHARS))
    }
    appendLine()
    appendLine(
        "Later messages answer or correct earlier ones; your lists must describe everything " +
            "they have said, with corrections applied. " +
            "Foods: one entry per distinct food they named, at most $MAX_PARSED_FOODS, each with " +
            "a realistic portion for the quantity they gave and its calories and macros for that " +
            "portion. Include only foods they actually named — no sides, drinks, condiments or " +
            "cooking fat they did not mention. Set confidence to \"low\" for any item whose " +
            "portion or identity you are unsure of, otherwise \"high\"; on a \"low\" item only, " +
            "set uncertainAbout to their words you could not pin down. " +
            "Activities: pick the closest type from the list, with the duration in minutes as " +
            "they gave it; set name to a short note in their own words, or leave it out. Do not " +
            "estimate calories burned — the app works that out from their own weight. " +
            "Set mealType only if they said which meal it was. " +
            "Water: if they said how many glasses of plain water they drank, set waterGlasses and " +
            "do not also list it as a food. Weight: if they stated their own body weight, set weight " +
            "to the number exactly as they said it, with no unit conversion.",
    )
    appendLine()
    if (mayAsk) {
        appendLine(
            "If a detail that would change the estimate a lot is missing — how much of a food " +
                "whose portion varies widely, like rice, pasta or \"some chips\", or how long an " +
                "activity lasted — set question to ONE short question (under 15 words) asking " +
                "for it, and return both lists empty. Do not ask about anything a typical " +
                "serving answers well enough.",
        )
    } else {
        appendLine(
            "Do not ask anything. Where a detail is missing, assume one ordinary serving or the " +
                "shortest plausible duration, and mark those foods \"low\".",
        )
    }
    appendLine(
        "If they named nothing edible and no physical activity, return both lists empty. Give no " +
            "medical advice, no diagnosis, and no supplement or medication suggestions.",
    )
}
