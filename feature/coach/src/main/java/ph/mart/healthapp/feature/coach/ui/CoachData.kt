package ph.mart.healthapp.feature.coach.ui

import ph.mart.healthapp.core.data.coach.ChatMessage
import ph.mart.healthapp.core.data.insight.InsightRequest

/**
 * Read model. [messages] is the persisted conversation; [request] is the day the model will be
 * told about, rebuilt from the repositories on every emission so a meal logged in another tab is
 * already in the next answer.
 *
 * [failure] is UI-only and deliberately not persisted: a send that didn't land wrote no rows, so
 * there is nothing in Room for it to describe. It clears on the next successful send.
 */
data class CoachUiState(
    /** False until the first emission. An empty conversation and an unread one look identical
     * otherwise, and the empty state must not flash on every open. */
    val loaded: Boolean = false,
    val messages: List<ChatMessage> = emptyList(),
    val request: InsightRequest? = null,
    val sending: Boolean = false,
    val failure: CoachFailure? = null,
)

/**
 * What to show when a send didn't produce an answer. [reason] says why in one line; [insight] is
 * the rule-based line for the same day — the identical fallback Home's insight card uses, so
 * offline the coach still says something true about today rather than only apologising.
 */
data class CoachFailure(val reason: String, val insight: String?, val question: String)

const val OFFLINE_REASON = "You're offline — the coach needs a connection."
const val FAILED_REASON = "The coach couldn't answer that just now."

/** All the screen's writes. [OnRetry] resends the question the failure is holding, so a dropped
 * connection doesn't cost the user their typing. */
sealed interface CoachEvent {
    data class OnSend(val question: String) : CoachEvent
    data object OnRetry : CoachEvent
    data object OnClear : CoachEvent
}

/**
 * Openers for an empty conversation. A blank text box against a coach the user has never used is
 * a dead end — and these three are the questions the day's numbers can actually answer, which is
 * also what teaches the coach's limits without a paragraph explaining them.
 */
val STARTERS = listOf(
    "How am I doing today?",
    "What should I eat tonight?",
    "Am I getting enough protein?",
)
