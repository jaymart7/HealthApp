package ph.mart.healthapp.core.data.food

import ph.mart.healthapp.core.data.coach.MAX_ACTION_GLASSES
import ph.mart.healthapp.core.data.coach.MAX_ACTION_WEIGHT
import ph.mart.healthapp.core.data.coach.MIN_ACTION_WEIGHT
import ph.mart.healthapp.core.data.exercise.ParsedExercise
import ph.mart.healthapp.core.data.profile.round1
import ph.mart.healthapp.core.data.stripMarkdown

/**
 * The FAB's one field: a sentence about what the user ate *or* did, and a short conversation
 * when the sentence leaves out the one thing the estimate turns on.
 *
 * [MealParseRepository] and `ExerciseParseRepository` each read one sentence of one kind; this
 * reads both kinds at once, because the FAB cannot know which the user is about to type, and a
 * classify-then-parse pair would spend two requests on every log. The payload is theirs and no
 * wider — the words the user typed and the questions the model asked back, nothing from the
 * profile. Burned calories are still never the model's: the activities come back as
 * [ParsedExercise], which has nowhere to put one.
 */
interface QuickLogRepository {
    suspend fun parse(turns: List<QuickLogTurn>): QuickLogResult
}

/** One line of the conversation — the user's words, or a question the model asked back. */
data class QuickLogTurn(val fromUser: Boolean, val text: String)

/**
 * [Question] is the follow-up, and it replaces a parse rather than riding beside one: a model that
 * needs to know how much rice there was has not got an estimate worth showing yet.
 */
sealed interface QuickLogResult {
    data class Question(val text: String) : QuickLogResult
    /**
     * [mealType] is the slot the user named ("for lunch"), null when they named none — the sheet
     * then keeps its time-of-day guess.
     *
     * [waterGlasses] is glasses to **add**, never the day's total — `CoachAction.LogWater`'s rule.
     * [weight] is the number the user said, in the **profile's** unit: the model is never asked
     * which unit a figure was in, `CoachAction.LogWeight`'s rule, and the caller converts.
     */
    data class Parsed(
        val foods: List<RecognizedFood>,
        val activities: List<ParsedExercise>,
        val mealType: MealType? = null,
        val waterGlasses: Int? = null,
        val weight: Double? = null,
    ) : QuickLogResult
    data object NothingFound : QuickLogResult
    data object Failed : QuickLogResult
}

/**
 * Two questions, then an estimate. A third round of "and how much of that?" is the app refusing to
 * log; past this the model is told to guess and flag the guess, which is what the single-sentence
 * parses have always done.
 */
const val MAX_FOLLOW_UPS = 2

/** One short question. A paragraph back is a lecture, not a follow-up. */
const val MAX_QUESTION_CHARS = 120

/** How many turns are sent. The first is always kept — it is the meal — and the rest are the
 * latest, which is where the corrections are. */
const val MAX_QUICK_LOG_TURNS = 8

/** Whether the model may still ask rather than estimate — counted off the questions it has
 * already asked, so the cap survives a correction typed after the review. */
fun List<QuickLogTurn>.mayAsk(): Boolean = count { !it.fromUser } < MAX_FOLLOW_UPS

/**
 * The whole of the trust boundary on the quick-log parse, pure for [loggable]'s reason: the
 * [org.json] read around it is stubbed on the JVM, so the judgement is kept where a test reaches it.
 *
 * A question is honoured only while [mayAsk] — past the cap the model was told not to ask, and one
 * that asks anyway gets its lists read instead. Foods pass [loggable]; activities have already
 * passed `parsedExercise`, and a null there is one it rejected.
 */
fun quickLogResult(
    question: String?,
    foods: List<RecognizedFood>,
    activities: List<ParsedExercise?>,
    mayAsk: Boolean,
    mealType: String? = null,
    waterGlasses: Int? = null,
    weight: Double? = null,
): QuickLogResult {
    val asked = question?.let { stripMarkdown(it).trim().take(MAX_QUESTION_CHARS).trim() }
    if (mayAsk && !asked.isNullOrEmpty()) return QuickLogResult.Question(asked)
    val eaten = foods.loggable()
    val done = activities.filterNotNull()
    // The coach's bands, so a sentence and a coach draft judge the same figure the same way: past
    // twenty glasses is a miscount, and a weight outside the band is a misread number, not a body.
    val glasses = waterGlasses?.takeIf { it in 1..MAX_ACTION_GLASSES }
    val body = weight?.takeIf { it in MIN_ACTION_WEIGHT..MAX_ACTION_WEIGHT }?.let(::round1)
    return if (eaten.isEmpty() && done.isEmpty() && glasses == null && body == null) {
        QuickLogResult.NothingFound
    } else {
        QuickLogResult.Parsed(
            foods = eaten,
            activities = done,
            // Only against the enum's own names — the schema's enumeration — so a slot the model
            // made up is no slot, not a crash.
            mealType = MealType.entries.firstOrNull { it.name.equals(mealType, ignoreCase = true) },
            waterGlasses = glasses,
            weight = body,
        )
    }
}
