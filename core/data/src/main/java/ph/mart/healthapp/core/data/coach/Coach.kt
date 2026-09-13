package ph.mart.healthapp.core.data.coach

import kotlinx.coroutines.flow.Flow
import ph.mart.healthapp.core.data.exercise.ExerciseType
import ph.mart.healthapp.core.data.food.MealType
import ph.mart.healthapp.core.data.insight.InsightRequest

/**
 * One turn of the conversation. [fromUser] rather than a role string because there are exactly
 * two speakers and the model's own `"user"`/`"model"` vocabulary is an API detail that stops at
 * [CoachRepositoryImpl].
 */
data class ChatMessage(
    val id: Long = 0,
    val fromUser: Boolean,
    val text: String,
    val sentAtMillis: Long,
)

/**
 * A row the coach has drafted and the user has not yet agreed to.
 *
 * The coach never writes. It calls `log_food`/`log_water`, the call is validated into one of these
 * by [parseAction], and the *user* commits it — which is the add-entry sheet's confirm step
 * reached through a different door, not the coach logging. Everything here has already cleared
 * that trust boundary, so the screen can render it and [CoachRepository.settle] can write it
 * without re-checking anything.
 */
sealed interface CoachAction {
    data class LogFood(
        val name: String,
        val mealType: MealType,
        val calories: Int,
        val proteinG: Int,
        val carbsG: Int,
        val fatG: Int,
        val portionAmount: Double,
        val portionUnit: String,
    ) : CoachAction

    /** Glasses to *add* to the day, never the day's new total — a model that reads "one glass"
     * and writes `1` must not erase the six already logged. */
    data class LogWater(val glasses: Int) : CoachAction

    /**
     * [burnedKcal] is **not** the model's figure — it is `estimateBurnedKcal()`'s, filled in from
     * the user's own latest weigh-in once the call has parsed. A model asked for a calorie burn
     * invents one, and the app already owns the MET arithmetic that the log-exercise sheet uses;
     * this way a coach-drafted run and a hand-logged one of the same length price identically,
     * and the weight that priced it never leaves the device.
     *
     * [name] may be empty, which is what [ExerciseEntry] means by "call it by its type".
     */
    data class LogExercise(
        val type: ExerciseType,
        val name: String,
        val minutes: Int,
        val burnedKcal: Int,
    ) : CoachAction
}

/**
 * What a send emits while it runs. [Partial] is the whole answer *so far*, re-emitted as each
 * chunk lands; there is no `Answered` variant because there is nothing left to say once the stream
 * ends — the finished answer reaches the screen the way every other row does, through
 * [CoachRepository.observeMessages]. Completing without a [Failed] or a [Proposal] is the success
 * signal.
 *
 * There is no error *type*: offline, throttled, App Check refused and a model with nothing usable
 * to say all land on the same fallback, exactly as they do for the daily insight.
 */
sealed interface CoachReply {
    /**
     * The answer so far. Empty is a real value and means *forget what I said*: it is emitted when
     * a tool round's preface is dropped, and the screen reads it as "no answer yet" — the thinking
     * mascot — rather than as an empty bubble.
     */
    data class Partial(val text: String) : CoachReply

    /**
     * The model asked to write something, so the stream stops here and *nothing* is persisted —
     * neither the row it drafted nor the turn that drafted it. [CoachRepository.settle] is what
     * ends this turn, once the user has confirmed or dismissed.
     *
     * It carries no text: whatever prose came with the call already reached the screen as
     * [Partial]s, sanitized, and that is the copy the turn is eventually persisted with.
     */
    data class Proposal(val action: CoachAction) : CoachReply

    data object Failed : CoachReply
}

/**
 * The second Gemini-backed feature, and the first that talks back.
 *
 * It is told the same [InsightRequest] the home-screen insight sends and nothing else — no age,
 * sex, height, absolute weight, diary rows or photos. One payload type is what keeps "what leaves
 * the device" auditable in one place.
 *
 * A question is only persisted once it has been answered: [send] writes both rows in one
 * transaction *after* the stream completes, so a call killed by process death or by leaving the
 * screen — a cancelled collection simply never reaches the write — loses the un-sent question
 * rather than stranding it in the history with nothing under it. That is the same reading
 * `FastingRepository.discardActive()` gives an unfinished fast: it never became history.
 */
interface CoachRepository {
    fun observeMessages(): Flow<List<ChatMessage>>

    /** The answer as it arrives. Cold: nothing is sent until it is collected, and the row pair is
     * written on the last chunk — unless the turn ends in a [CoachReply.Proposal], which writes
     * nothing and hands the ending to [settle]. */
    fun send(question: String, request: InsightRequest?): Flow<CoachReply>

    /**
     * Ends a turn that stopped on a proposal: commits [action] when the user confirmed it, then
     * writes the question/answer pair exactly as [send] does on its last chunk. A null [action] is
     * a dismissal — the turn is still history, because the user read the answer either way.
     *
     * The write goes through the ordinary repository the add-entry sheet uses, so a coach-drafted
     * row is indistinguishable from a hand-typed one once it lands.
     */
    suspend fun settle(question: String, answer: String, action: CoachAction?)

    /** Soft-deletes the whole conversation. Room's rows stay, like every other domain's. */
    suspend fun clear()
}

/**
 * Past this the model has written an essay into a chat bubble. Rejecting rather than truncating,
 * for the reason [ph.mart.healthapp.core.data.insight.MAX_INSIGHT_CHARS] gives: half a sentence
 * reads as a bug, and the rule-based line it falls back to is always complete.
 *
 * Raised from 900 when the coach was allowed to answer in a short list: six lines of "what should
 * I eat tonight?" is legitimately longer than three sentences, and a cap that rejects the format
 * the prompt now asks for is a cap that fails every list.
 */
internal const val MAX_REPLY_CHARS = 1400

/**
 * How much of the conversation is replayed to the model on each send.
 *
 * ponytail: a flat message count, not a token budget — ten turns of a nutrition chat is nowhere
 * near the context window. Price it in tokens if the coach ever grows attachments.
 */
internal const val MAX_HISTORY_MESSAGES = 20

private val WHITESPACE = Regex("[ \\t]+")

/**
 * The whole of the trust boundary on the coach's output — everything a bubble renders passes
 * through here.
 *
 * A pure function for the same reason `sanitizeInsight` is one: it is the part a JVM test can
 * reach. Unlike that one it keeps line breaks, because an answer to "what should I eat tonight?"
 * legitimately spans a short paragraph; only runs of spaces and tabs collapse.
 */
internal fun sanitizeReply(raw: String?): String? {
    val text = raw
        ?.replace(WHITESPACE, " ")
        ?.lines()
        ?.joinToString("\n") { it.trim() }
        ?.trim()
        ?.trim('"', '“', '”')
        ?.trim()
    if (text.isNullOrEmpty()) return null
    return text.takeIf { it.length <= MAX_REPLY_CHARS }
}
