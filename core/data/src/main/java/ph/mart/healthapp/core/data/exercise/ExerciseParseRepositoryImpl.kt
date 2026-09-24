package ph.mart.healthapp.core.data.exercise

import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import org.json.JSONObject
import ph.mart.healthapp.core.data.AI_MODEL_NAME
import ph.mart.healthapp.core.data.AI_THINKING
import ph.mart.healthapp.core.data.logAiFailure

/**
 * JSON out and [org.json.JSONObject] in, the call every other parse in this app makes: three flat
 * fields need no kotlinx-serialization dependency.
 *
 * The schema and the read-back sit here rather than in a `…Json.kt` of their own — the split
 * `RecognizedFoodJson.kt` made exists because a plate and a sentence are two features sharing one
 * wire shape, and this one has a single caller. What is kept on the other side of the line is the
 * judgement: [parsedExercise] is pure, and it is where a JVM test reaches the rules.
 *
 * The model is built once and held — nothing about this configuration carries anything about the
 * user, and the only thing that varies per call is the sentence itself.
 */
internal class ExerciseParseRepositoryImpl : ExerciseParseRepository {

    private val model = Firebase.ai(
        backend = GenerativeBackend.googleAI(),
        useLimitedUseAppCheckTokens = true,
    ).generativeModel(
        modelName = AI_MODEL_NAME,
        generationConfig = generationConfig {
            thinkingConfig = AI_THINKING
            maxOutputTokens = MAX_ACTIVITY_TOKENS
            responseMimeType = "application/json"
            responseSchema = PARSED_EXERCISE_SCHEMA
        },
    )

    override suspend fun parse(text: String): ExerciseParseResult = try {
        val prompt = promptFor(text.take(MAX_EXERCISE_PARSE_CHARS))
        val response = model.generateContent(content { text(prompt) })
        val activity = response.text?.let { readActivity(JSONObject(it)) }
        // Null means the sentence named nothing physical — a real answer with its own line on the
        // sheet, not a failure to retry.
        if (activity == null) ExerciseParseResult.NoActivityFound else ExerciseParseResult.Success(activity)
    } catch (e: CancellationException) {
        // Backing out of the sheet cancels the scope, and that is not an AI failure: without this
        // the catch below swallows the cancellation and logs a request the user withdrew. The rule
        // `MealParseRepositoryImpl` and `CoachRepositoryImpl` already follow.
        throw e
    } catch (e: Exception) {
        logAiFailure("exercise parse", e)
        // Offline, throttled, App Check refused — all the same to the caller.
        ExerciseParseResult.Failed
    }
}

/**
 * Three fields, and the absent fourth is the point: there is nowhere in this schema to put a
 * calorie burn, so the model cannot volunteer one and no prompt has to forbid it. See
 * [ParsedExercise].
 *
 * `type` is an enumeration over [ExerciseType]'s own entry names rather than a free string, which
 * is what makes `Other` a *choice* the model makes instead of the bucket a typo falls into.
 */
internal val PARSED_EXERCISE_SCHEMA = Schema.obj(
    mapOf(
        "type" to Schema.enumeration(
            values = ExerciseType.entries.map { it.name },
            description = "The kind of activity. Use Other when none of the rest fit.",
        ),
        "minutes" to Schema.integer(
            description = "How long it lasted, in minutes. 0 if they named no physical activity.",
        ),
        "name" to Schema.string(
            description = "A short note in their own words — where or how. Omit it entirely if " +
                "they said nothing beyond the activity itself.",
        ),
    ),
    // The one optional property, [RECOGNIZED_FOOD_SCHEMA]'s `uncertainAbout` reasoning: required,
    // a model with nothing to add here adds something anyway, and an invented note on a plain run
    // is worse than no note at all.
    optionalProperties = listOf("name"),
)

/** Three flat fields. `MAX_FOOD_LIST_TOKENS`' rule at a twentieth of the size — and
 * `AI_THINKING`'s KDoc is why a generous cap is not free. */
private const val MAX_ACTIVITY_TOKENS = 80

/**
 * The object in, an activity out. Every read is an `opt*` with a default and the judgement is
 * [parsedExercise]'s, which is the thing that decides whether this was a workout at all.
 *
 * Takes the object rather than the response text because the quick log reads the same shape out
 * of an array nested in its own reply.
 */
internal fun readActivity(body: JSONObject): ParsedExercise? =
    parsedExercise(
        type = body.optString("type"),
        name = body.optString("name"),
        minutes = body.optInt("minutes"),
    )

/**
 * A sentence in, one activity out. Two of `promptFor`'s constraints plus the one this call needs:
 * - **Only what they said.** A model asked what someone did will otherwise round the workout up —
 *   the walk becomes a jog, the half hour becomes an hour — and every invention is a figure the
 *   user has to notice and correct.
 * - **The note is theirs, never invented.** It is the one free-text field, and a model that fills
 *   it in when they gave nothing writes prose into a diary row.
 * - **No medical advice**, for the reason every other prompt here gives.
 */
private fun promptFor(sentence: String): String = buildString {
    appendLine(
        "You are an activity-logging assistant for a fitness app. The user has said or typed what " +
            "they just did. Turn it into one activity.",
    )
    appendLine()
    appendLine("What they said:")
    appendLine(sentence)
    appendLine()
    appendLine(
        "Pick the single closest type from the list, and give the duration in minutes exactly as " +
            "they gave it — do not round it up or invent one they did not say. If they gave no " +
            "duration, estimate the shortest plausible one for what they described. Set name to a " +
            "short note in their own words about where or how, and leave it out entirely if they " +
            "said nothing beyond the activity. If they named no physical activity at all, set " +
            "minutes to 0. Do not estimate calories burned — the app works that out from their " +
            "own weight. Give no medical advice, no diagnosis, and no training-safety judgements.",
    )
}
