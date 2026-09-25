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
import ph.mart.healthapp.core.data.profile.UnitSystem

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

    /** The strength screen's parse: a different schema and a longer answer, so a second held
     * model rather than a per-call config — still nothing about the user in it. */
    private val setsModel = Firebase.ai(
        backend = GenerativeBackend.googleAI(),
        useLimitedUseAppCheckTokens = true,
    ).generativeModel(
        modelName = AI_MODEL_NAME,
        generationConfig = generationConfig {
            thinkingConfig = AI_THINKING
            maxOutputTokens = MAX_SETS_TOKENS
            responseMimeType = "application/json"
            responseSchema = PARSED_SETS_SCHEMA
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

    override suspend fun parseSets(text: String, unit: UnitSystem): StrengthParseResult = try {
        val prompt = setsPromptFor(text.take(MAX_STRENGTH_PARSE_CHARS))
        val response = setsModel.generateContent(content { text(prompt) })
        val sets = response.text?.let { parsedSets(readLifts(JSONObject(it)), unit) }.orEmpty()
        if (sets.isEmpty()) StrengthParseResult.NoLiftsFound else StrengthParseResult.Success(sets)
    } catch (e: CancellationException) {
        // [parse]'s reason: a withdrawn request is not a failure to log.
        throw e
    } catch (e: Exception) {
        logAiFailure("strength parse", e)
        StrengthParseResult.Failed
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

/**
 * One object per lift at one load, and — like [PARSED_EXERCISE_SCHEMA] — no calorie field
 * anywhere in it. `sets` is a count rather than a row per set, because "3x8" is how a session is
 * said and expanding it is [parsedSets]' job, not the model's tokens.
 *
 * `unit` is the one optional property: a model made to give one for "bench at 60" guesses, and a
 * guessed unit is a 2.2× error. Absent, it is the user's own unit, applied on-device.
 */
internal val PARSED_SETS_SCHEMA = Schema.obj(
    mapOf(
        "lifts" to Schema.array(
            Schema.obj(
                mapOf(
                    "lift" to Schema.string(description = "The exercise, as they named it."),
                    "sets" to Schema.integer(description = "How many sets of this lift at this load."),
                    "reps" to Schema.integer(description = "Reps in each of those sets."),
                    "weight" to Schema.double(
                        description = "The load, as they said it. 0 for bodyweight or when they gave none.",
                    ),
                    "unit" to Schema.enumeration(
                        values = listOf("kg", "lb"),
                        description = "The unit they said. Omit it if they said none.",
                    ),
                ),
                optionalProperties = listOf("unit"),
            ),
            description = "Every lift they named, in order. Empty if they named none.",
        ),
    ),
)

/** Up to a dozen small objects — [MAX_ACTIVITY_TOKENS]' rule, scaled to a list. */
private const val MAX_SETS_TOKENS = 600

/** Every read an `opt*` with a default; [parsedSets] decides what survives. */
internal fun readLifts(body: JSONObject): List<ParsedLiftRow> {
    val lifts = body.optJSONArray("lifts") ?: return emptyList()
    return (0 until lifts.length()).mapNotNull { i ->
        lifts.optJSONObject(i)?.let {
            ParsedLiftRow(
                lift = it.optString("lift"),
                sets = it.optInt("sets", 1),
                reps = it.optInt("reps"),
                weight = it.optDouble("weight", 0.0),
                unit = it.optString("unit").takeIf { unit -> unit.isNotEmpty() },
            )
        }
    }
}

/**
 * A sentence in, a session out. [promptFor]'s constraints, for lifts:
 * - **Only what they said** — no lift, set or load they didn't name. Rounding a session up is
 *   the same invention as rounding a walk into a jog.
 * - **"NxM" is sets × reps**, the convention every programme is written in, stated so the model
 *   doesn't have to guess which way round.
 * - **No unit they didn't say**, for [PARSED_SETS_SCHEMA]'s reason.
 * - **No medical advice or training-safety judgements**, as everywhere else.
 */
private fun setsPromptFor(sentence: String): String = buildString {
    appendLine(
        "You are a workout-logging assistant for a fitness app. The user has said or typed the " +
            "strength training they just did. Turn it into a list of lifts.",
    )
    appendLine()
    appendLine("What they said:")
    appendLine(sentence)
    appendLine()
    appendLine(
        "Give one entry per lift at one load, in the order they said them. Read \"3x8\" as 3 sets " +
            "of 8 reps. If they gave reps but no set count, that is 1 set. Give the weight exactly " +
            "as they said it, and 0 for bodyweight or when they gave no weight. Set unit only if " +
            "they said kg or lb (or pounds, kilos); otherwise leave it out. Do not add lifts, sets, " +
            "reps or weights they did not say. If they named no lifts at all, return an empty " +
            "list. Do not estimate calories burned. Give no medical advice, no diagnosis, and no " +
            "training-safety judgements.",
    )
}
