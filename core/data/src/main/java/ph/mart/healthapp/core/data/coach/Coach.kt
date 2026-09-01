package ph.mart.healthapp.core.data.coach

import kotlinx.coroutines.flow.Flow
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

/** What a send produced. There is no error *type*: offline, throttled, App Check refused and a
 * model with nothing usable to say all land on the same fallback, exactly as they do for the
 * daily insight. */
sealed interface CoachReply {
    data class Answered(val text: String) : CoachReply
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
 * transaction, so a call killed by process death or by leaving the screen loses the un-sent
 * question rather than stranding it in the history with nothing under it. That is the same
 * reading `FastingRepository.discardActive()` gives an unfinished fast — it never became history.
 */
interface CoachRepository {
    fun observeMessages(): Flow<List<ChatMessage>>
    suspend fun send(question: String, request: InsightRequest?): CoachReply

    /** Soft-deletes the whole conversation. Room's rows stay, like every other domain's. */
    suspend fun clear()
}

/**
 * Past this the model has written an essay into a chat bubble. Rejecting rather than truncating,
 * for the reason [ph.mart.healthapp.core.data.insight.MAX_INSIGHT_CHARS] gives: half a sentence
 * reads as a bug, and the rule-based line it falls back to is always complete.
 */
internal const val MAX_REPLY_CHARS = 900

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
