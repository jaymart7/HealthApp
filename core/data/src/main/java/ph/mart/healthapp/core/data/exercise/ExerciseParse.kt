package ph.mart.healthapp.core.data.exercise

import ph.mart.healthapp.core.data.profile.UnitSystem
import ph.mart.healthapp.core.data.profile.displayUnitToKg
import ph.mart.healthapp.core.data.stripMarkdown

/**
 * The sixth Gemini-backed feature, and the narrowest payload of all of them: one sentence about
 * what the user just did, and nothing else. No profile, no weight, no history —
 * [MealParseRepository][ph.mart.healthapp.core.data.food.MealParseRepository]'s argument, applied
 * to a workout. Parsing "45 minute run along the river" needs none of them, and the one figure a
 * profile could contribute is the one this call deliberately never asks for.
 *
 * Nothing is cached: every sentence is its own answer.
 */
interface ExerciseParseRepository {
    suspend fun parse(text: String): ExerciseParseResult

    /**
     * A sentence about a lifting session in, its sets out — the strength screen's describe panel.
     * Still only the sentence goes out: [unit] is **not sent**. It is applied on-device to a load
     * the sentence gave no unit for, which is what "bench at 60" means to the person who typed it.
     */
    suspend fun parseSets(text: String, unit: UnitSystem): StrengthParseResult
}

/**
 * [NoActivityFound] is its own state rather than a null [Success], the call `MealParseResult`
 * makes: "there was no workout in that sentence" and "the call didn't work" are different answers,
 * and the sheet says different things about them.
 */
sealed interface ExerciseParseResult {
    data class Success(val activity: ParsedExercise) : ExerciseParseResult
    data object NoActivityFound : ExerciseParseResult
    data object Failed : ExerciseParseResult
}

/**
 * One activity the app is willing to seed a form with — and **no calorie figure**, which is the
 * whole shape of this type.
 *
 * `CoachAction.LogExercise` already argues it: a model asked for a burn invents one, the app owns
 * the MET arithmetic ([estimateBurnedKcal]), and pricing on-device is what keeps a spoken run and
 * a typed one of the same length identical and keeps the weight that priced them off the wire.
 * That rule was written for the coach's tool call; this is the second path it governs, so the
 * absence is enforced by the schema rather than by a prompt asking nicely.
 *
 * [name] may be empty, which is what [ExerciseEntry] means by "call it by its type".
 */
data class ParsedExercise(
    val type: ExerciseType,
    val name: String,
    val minutes: Int,
)

/**
 * How much of the sentence is sent. A workout is a phrase — shorter than a meal, which can list
 * courses — and `MAX_PARSE_CHARS`' reasoning applies: it is cheaper to cap the input than to pay
 * for a parse of something that was never a workout.
 */
const val MAX_EXERCISE_PARSE_CHARS = 200

/** A note is a line under the type, not a paragraph. `ExerciseEntry.name`'s practical ceiling. */
const val MAX_EXERCISE_NAME_CHARS = 60

/** Ten hours. Longer than any session anybody logs, short enough that a misread "100" for a
 * duration in seconds cannot seed a form claiming a day and a half of swimming. */
const val MAX_EXERCISE_MINUTES = 600

/**
 * The whole of the trust boundary on the model's parse — everything the sheet seeds a form with
 * passes through here.
 *
 * A pure function for
 * [loggable][ph.mart.healthapp.core.data.food.loggable]'s reason: the [org.json] read around it is
 * stubbed in JVM unit tests, so the judgement is kept on this side of it where a test can reach it.
 *
 * The rules are `parseLogExercise`'s, deliberately, so a spoken workout and a coach-drafted one
 * validate identically — an unknown type is rejected rather than folded into [ExerciseType.Other],
 * because a type the model made up is a claim about the burn the app is about to compute.
 *
 * Null is [ExerciseParseResult.NoActivityFound]: a sentence naming nothing physical comes back
 * with zero minutes, and zero minutes is not a shorter workout, it is not one.
 */
fun parsedExercise(type: String?, name: String?, minutes: Int?): ParsedExercise? {
    val activity = ExerciseType.entries.firstOrNull { it.name.equals(type, ignoreCase = true) }
        ?: return null
    val duration = minutes?.takeIf { it in 1..MAX_EXERCISE_MINUTES } ?: return null
    return ParsedExercise(
        type = activity,
        // The model's prose, and it becomes a diary row's title — the field `loggable()` strips
        // for exactly this reason. Truncated rather than rejected: an over-long note is the one
        // field here whose surplus costs nothing, and the user is looking at it before it saves.
        name = stripMarkdown(name.orEmpty()).trim().take(MAX_EXERCISE_NAME_CHARS).trim(),
        minutes = duration,
    )
}

/** [ExerciseParseResult]'s three answers, for a list of sets rather than one activity. */
sealed interface StrengthParseResult {
    data class Success(val sets: List<StrengthSet>) : StrengthParseResult
    data object NoLiftsFound : StrengthParseResult
    data object Failed : StrengthParseResult
}

/** A session lists several lifts, so twice what one activity is allowed. */
const val MAX_STRENGTH_PARSE_CHARS = 400

/** Past any real set, short enough that a misread "3x800" cannot seed one. */
const val MAX_PARSED_REPS = 100

/** Sets of one lift at one load. "3x8" is the common case; ten already covers a warm-up ladder. */
const val MAX_PARSED_SET_COUNT = 10

/** Above every raw world record, so a real load always fits and a unit mix-up that turns 225 lb
 * into 225 × something does not. */
const val MAX_PARSED_LOAD_KG = 500.0

/** One session's worth. The list and its editor are reviewed before saving, and thirty rows is
 * already a long scroll. */
const val MAX_PARSED_SETS = 30

/** One lift as the model gave it — every field raw, so the judgement stays in [parsedSets]. */
internal data class ParsedLiftRow(
    val lift: String?,
    val sets: Int?,
    val reps: Int?,
    val weight: Double?,
    val unit: String?,
)

/**
 * The whole trust boundary on a parsed session — [parsedExercise]'s job for the strength screen,
 * and pure for the same reason: the `org.json` read around it is stubbed on the JVM.
 *
 * A row is dropped rather than repaired when a figure is out of range: a set the user never did
 * is worse than a set they have to add by hand. Weight is the exception, because a missing or
 * negative one is bodyweight — 0 kg, a real value here — and "pull-ups 3x10" says nothing more.
 * A unit the model did not give is [preferred]'s.
 *
 * Each row becomes that many identical sets, which is what "3x8" means and what the set list
 * draws; the total is capped at [MAX_PARSED_SETS]. Empty is
 * [StrengthParseResult.NoLiftsFound].
 */
internal fun parsedSets(rows: List<ParsedLiftRow>, preferred: UnitSystem): List<StrengthSet> =
    rows.flatMap { row ->
        val name = stripMarkdown(row.lift.orEmpty()).trim().take(MAX_EXERCISE_NAME_CHARS).trim()
        val reps = row.reps?.takeIf { it in 1..MAX_PARSED_REPS }
        val count = (row.sets ?: 1).takeIf { it in 1..MAX_PARSED_SET_COUNT }
        val unit = when (row.unit?.lowercase()) {
            "kg" -> UnitSystem.Metric
            "lb" -> UnitSystem.Imperial
            else -> preferred
        }
        val kg = (row.weight ?: 0.0).coerceAtLeast(0.0).displayUnitToKg(unit)
        if (name.isEmpty() || reps == null || count == null || kg > MAX_PARSED_LOAD_KG) {
            emptyList()
        } else {
            List(count) { StrengthSet(exerciseName = name, reps = reps, weightKg = kg) }
        }
    }.take(MAX_PARSED_SETS)
