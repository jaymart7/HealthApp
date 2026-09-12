package ph.mart.healthapp.feature.coach.ui

import androidx.annotation.StringRes
import ph.mart.healthapp.core.data.coach.ChatMessage
import ph.mart.healthapp.core.data.insight.InsightRequest
import ph.mart.healthapp.feature.coach.R

/**
 * Read model. [messages] is the persisted conversation; [request] is the day the model will be
 * told about, rebuilt from the repositories on every emission so a meal logged in another tab is
 * already in the next answer.
 *
 * [pending] and [streaming] are the turn in flight, and neither is in Room — the pair of rows is
 * written on the stream's last chunk, so until then the question and the half-arrived answer exist
 * only here. A `sending` flag would say strictly less: `pending != null` is the same boolean, and
 * it also carries the text to draw above the answer.
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
    /** The question being answered right now, drawn as the user's bubble until Room has it. */
    val pending: String? = null,
    /** The answer so far, null until the first chunk lands. */
    val streaming: String? = null,
    val failure: CoachFailure? = null,
)

/**
 * A Room emission folded in.
 *
 * [pending] and [streaming] stand until the list itself changes, rather than being cleared when
 * the stream ends: the write and Room's invalidation are not the same instant, and dropping the
 * bubbles at completion blinks the finished turn off screen for the frames in between. Any change
 * to the list while a send is in flight is Room having spoken — a shrink counts too, which is what
 * keeps a `clear()` mid-send from stranding the input bar.
 */
internal fun CoachUiState.withMessages(
    messages: List<ChatMessage>,
    request: InsightRequest?,
): CoachUiState {
    val landed = messages.size != this.messages.size
    return copy(
        loaded = true,
        messages = messages,
        request = request,
        pending = pending.takeUnless { landed },
        streaming = streaming.takeUnless { landed },
    )
}

/**
 * What to show when a send didn't produce an answer. [reason] says why in one line; [insight] is
 * the rule-based line for the same day — the identical fallback Home's insight card uses, so
 * offline the coach still says something true about today rather than only apologising.
 */
data class CoachFailure(@StringRes val reason: Int, val insight: String?, val question: String)

// Resources rather than `const val`s: a library module's R fields aren't compile-time
// constants, and the screen is where a reason gets read anyway.
@StringRes val OFFLINE_REASON = R.string.coach_failure_offline

@StringRes val FAILED_REASON = R.string.coach_failure_failed

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
    R.string.coach_starter_today,
    R.string.coach_starter_dinner,
    R.string.coach_starter_protein,
)
