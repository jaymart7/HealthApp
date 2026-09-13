package ph.mart.healthapp.feature.coach.ui

import androidx.annotation.StringRes
import ph.mart.healthapp.core.data.coach.ChatMessage
import ph.mart.healthapp.core.data.coach.CoachAction
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
 *
 * [proposal] is the third thing not in Room, and for the same reason as the other two: the coach
 * drafted a row and the user has not agreed to it, so neither the row nor the turn that drafted it
 * exists yet. It is retired the way [pending] and [streaming] are — by the Room emission that the
 * user's own tap eventually causes.
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
    /** A row the coach drafted, waiting on a tap. [streaming] holds the prose that came with it. */
    val proposal: CoachAction? = null,
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
        proposal = proposal.takeUnless { landed },
    )
}

/**
 * The turn in flight, dropped. Nothing was persisted — the repository writes a question only once
 * it has an answer — so there is nothing to reconcile and all three go together.
 *
 * Two callers, and they are the same ending reached two ways: the stop button, and a proposal
 * dismissed when no prose came with it (no answer to persist means no write, so no Room emission
 * arrives to retire the bubbles). A shared function rather than the same `copy` twice, because
 * missing a field in one of them strands the input bar.
 */
internal fun CoachUiState.withTurnAbandoned(): CoachUiState =
    copy(pending = null, streaming = null, proposal = null)

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

/**
 * All the screen's writes. [OnRetry] resends the question the failure is holding, so a dropped
 * connection doesn't cost the user their typing.
 *
 * [OnConfirmProposal] carries its own copy because the line it appends to the persisted answer is
 * user-facing, and the screen is the only place that can resolve a resource — *composables
 * resolve, ViewModels name*, and no `Context` reaches this one. [OnDismissProposal] needs no such
 * line: the turn is persisted with the coach's prose alone.
 */
sealed interface CoachEvent {
    data class OnSend(val question: String) : CoachEvent

    /** Abandons the turn in flight. Nothing is persisted — a stopped turn is one the user walked
     * away from, which is the reading leaving the screen already had. */
    data object OnStop : CoachEvent
    data object OnRetry : CoachEvent
    data object OnClear : CoachEvent
    data class OnConfirmProposal(val loggedLine: String) : CoachEvent
    data object OnDismissProposal : CoachEvent
}

/**
 * Openers for an empty conversation. A blank text box against a coach the user has never used is
 * a dead end, and these four teach its reach faster than a paragraph would: one about today, one
 * about a past day, one about a span, and one that needs an opinion. The first three exist to show
 * that the diary questions now have answers — before the tools they were the deflections.
 */
val STARTERS = listOf(
    R.string.coach_starter_today,
    R.string.coach_starter_yesterday,
    R.string.coach_starter_week,
    R.string.coach_starter_dinner,
)
