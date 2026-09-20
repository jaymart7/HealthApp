package ph.mart.healthapp.feature.coach.ui

import androidx.annotation.StringRes
import ph.mart.healthapp.core.data.epochDayOf
import ph.mart.healthapp.core.data.coach.ChatMessage
import ph.mart.healthapp.core.data.coach.CoachAction
import ph.mart.healthapp.core.data.coach.draftedOn
import ph.mart.healthapp.core.data.insight.InsightRequest
import ph.mart.healthapp.core.data.recap.Report
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
 * [loggedToDiary] is the fourth, and the shortest-lived: it is set by the tap that confirmed a
 * draft into *today's* diary and cleared by the next send, and all it does is offer a way to go
 * and look at what was written.
 *
 * [proposal] is the third thing not in Room, and for the same reason as the other two: the coach
 * drafted some rows and the user has not agreed to them, so neither the rows nor the turn that
 * drafted them exist yet. It is retired the way [pending] and [streaming] are — by the Room emission that the
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
    /** The rows the coach drafted, waiting on a tap — empty when there is no card up. One meal is
     * several of them. [streaming] holds the prose that came with them. */
    val proposal: List<CoachAction> = emptyList(),
    /** Where the last confirmed draft put rows, when that was today's diary — a `@StringRes` meal
     * label, which the screen turns into "View it in Breakfast". Null when there is nowhere true to
     * send anyone. See [diaryDestination]. */
    @StringRes val loggedDestination: Int? = null,
    /** Whether the device can reach the network *right now*, from `NetworkMonitor.observe()`. It
     * drives the pinned strip under the top bar and nothing else — the decision to call the model
     * is still a recheck at the moment of the send, because that is the only answer that matters to
     * a request about to be spent. */
    val offline: Boolean = false,
    /** Every window a report card can be drawn at, folded from Room and keyed by
     * [Report.days] — `REPORT_DAYS`, and both of them always, because the card's period chips
     * switch between them with no round trip. Empty until the first emission.
     *
     * It is *not* per message: two reports in one conversation at the same window are the same
     * fold, and holding a copy on each would be a second answer to the same question. What a
     * message carries is only which window it asked for. */
    val reports: Map<Int, Report> = emptyMap(),
    /** Whether the last thing that happened to this conversation was the user stopping a turn.
     * UI-only and never persisted, for [failure]'s reason: a stopped turn wrote no rows, so there
     * is nothing in Room for it to describe. Cleared by the next send. */
    val stopped: Boolean = false,
)

/**
 * Where a confirmed draft landed, when the diary is a true answer to that — a `@StringRes` meal
 * label, or null.
 *
 * It used to be a bare Boolean and the door read "View it in your diary", which was true and
 * vague: a draft goes into a *meal*, the diary opens on a day of four of them, and naming the one
 * that grew is the difference between a link and a direction. So the food rows name their slot and
 * everything else that still earns a door — water, an activity — says "diary", which is where those
 * actually appear.
 *
 * The two conditions [opensTheDiary] carried are unchanged and are both about not offering a door
 * that lands in the wrong place. **The kind has to be a diary row** — food, water and an activity
 * all appear on the day the diary draws, while a weigh-in, a mood, a cuff reading and a measurement
 * are Progress's, a supplement tick is Profile's, a fast is Home's timer and a started routine has
 * written nothing at all yet, and a door that opens the wrong screen is the shrug the coach's own
 * subject actions are written against. **And it has to be today**, because the diary opens on today
 * and its day is ViewModel state rather than something a route carries: a door from a backdated
 * draft would open a day that does not hold the rows it just promised. A backdated draft therefore
 * gets no door, which is the honest half.
 *
 * The **first** qualifying row names it, not the commonest: a draft is one meal by construction
 * (`send()` refuses rows that disagree about the day, and a mixed draft is a meal plus a glass), so
 * there is never a second slot to choose between.
 */
@StringRes
internal fun List<CoachAction>.diaryDestination(): Int? = firstNotNullOfOrNull { action ->
    when (action) {
        is CoachAction.LogFood -> action.mealType.labelRes.takeIf { action.draftedOn == null }
        is CoachAction.LogSavedMeal -> action.mealType.labelRes.takeIf { action.draftedOn == null }
        is CoachAction.LogWater,
        is CoachAction.LogExercise,
        // The diary's foot rather than a meal section, but the same screen and the same rule:
        // the door is worth it for today's note and would open the wrong day for any other.
        is CoachAction.LogNote,
        -> R.string.coach_destination_diary.takeIf { action.draftedOn == null }
        else -> null
    }
}

/**
 * Whether a confirmed draft is worth offering a door to the diary for. [diaryDestination] is what
 * the screen reads; this stays as the exhaustive statement of the rule, so a new [CoachAction] kind
 * cannot be added without answering the question — a `when` with an `else` cannot force that and
 * this one has no `else`.
 */
internal fun List<CoachAction>.opensTheDiary(): Boolean = any {
    when (it) {
        is CoachAction.LogFood,
        is CoachAction.LogWater,
        is CoachAction.LogExercise,
        is CoachAction.LogSavedMeal,
        is CoachAction.LogNote,
        -> it.draftedOn == null
        is CoachAction.LogWeight,
        is CoachAction.LogSupplement,
        is CoachAction.LogMood,
        is CoachAction.LogBloodPressure,
        is CoachAction.LogMeasurement,
        // Nothing was written at all: the tap opened a form the user has not saved. The workout
        // screen it opened *is* the place to go and look, and it is already on screen.
        is CoachAction.StartRoutine,
        // A fast is Home's card and Progress's page, and it writes no diary row at all.
        is CoachAction.SetFast,
        -> false
    }
}

/**
 * The routine a confirmed draft is about to open, or null when it is not one.
 *
 * The one action whose Confirm navigates rather than writes, so the screen needs to know — and it
 * is a pure function beside [opensTheDiary] for that one's reason: a composable that pattern-matches
 * on an action kind is a rule nothing can test. `routineDraftStandsAlone()` in `:core:data` is what
 * guarantees the singleton, so anything else here is null rather than the first routine it finds.
 */
internal fun List<CoachAction>.routineIdToStart(): Long? =
    (singleOrNull() as? CoachAction.StartRoutine)?.routineId?.takeIf { it > 0 }

/**
 * The report to draw under the message at [index], or null where there is none.
 *
 * Three ways to be null, and they are different: the message drew no report, the window it drew
 * has not been folded yet (the first frame, before the flow emits), or the model asked for a
 * window that is no longer one this app offers — a conversation outlives a change to
 * `REPORT_DAYS`, and a card headed with a window nobody folds is a card that would draw zeros.
 * The screen draws nothing in all three cases and the answer above it still reads correctly,
 * which is the whole reason the *window* is stored and never the figures.
 */
internal fun CoachUiState.reportAt(index: Int): Report? =
    messages.getOrNull(index)?.report?.let(reports::get)

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
        proposal = if (landed) emptyList() else proposal,
    )
}

/**
 * The question to re-ask for the message at [index], or null where re-asking makes no sense.
 *
 * Only the **newest** answer offers it: re-asking an old turn would append a fresh pair at the
 * bottom and bury the answer the user was looking at, which is a worse outcome than scrolling. Not
 * while a turn is in flight either — the input bar is locked for that, and this is the same send.
 * Null also when the row above is not the user's, which is what an unpaired history row looks like.
 *
 * The re-ask itself is a fresh send, not a repair — the reading `OnRetry` already has — so the
 * conversation keeps both answers. That is the honest record: the coach was asked twice.
 */
internal fun CoachUiState.askAgainQuestion(index: Int): String? {
    if (pending != null || index != messages.lastIndex) return null
    if (messages.getOrNull(index)?.fromUser != false) return null
    return messages.getOrNull(index - 1)?.takeIf { it.fromUser }?.text
}

/**
 * The day a separator goes above the message at [index], or null where none does.
 *
 * A transcript persists, so reopening the coach after a week is a wall of bubbles with no way to
 * tell Tuesday's question from this morning's. One label per day boundary answers that and costs
 * the list nothing on the common case, where every message is from today and only the first one
 * gets a label.
 *
 * Index 0 always opens a day — the top of a conversation is a boundary by definition — and every
 * other message opens one only where its local day differs from the message above it.
 * [ChatMessage.sentAtMillis] is the only ordering this type carries, which is deliberate
 * (`ChatMessageEntity` has no `date` column: a conversation is a sequence, not a series of days),
 * so the day is *derived at render* rather than stored. That is what keeps a row correct when it is
 * read in a different timezone from the one it was written in.
 *
 * Pure, and a JVM test rather than a comment: the rule is an off-by-one waiting to happen and the
 * screen is where it would be least visible.
 */
internal fun daySeparatorAt(messages: List<ChatMessage>, index: Int): Long? {
    val message = messages.getOrNull(index) ?: return null
    val day = epochDayOf(message.sentAtMillis)
    val previous = messages.getOrNull(index - 1) ?: return day
    return day.takeIf { it != epochDayOf(previous.sentAtMillis) }
}

/**
 * The turn in flight, dropped. Nothing was persisted — the repository writes a question only once
 * it has an answer — so there is nothing to reconcile and all three go together.
 *
 * Two callers, and they are the same ending reached two ways: the stop button, and a proposal
 * dismissed when no prose came with it (no answer to persist means no write, so no Room emission
 * arrives to retire the bubbles). A shared function rather than the same `copy` twice, because
 * missing a field in one of them strands the input bar.
 *
 * They differ in exactly one thing, which is what [stopped] carries: a **stop leaves a mark** in
 * the transcript, because the user did something and both bubbles vanishing with no trace reads as
 * the app losing their question. A dismissed proposal leaves none — the card going away *is* the
 * acknowledgement, and there was never a turn to mark.
 */
internal fun CoachUiState.withTurnAbandoned(stopped: Boolean = false): CoachUiState =
    copy(pending = null, streaming = null, proposal = emptyList(), stopped = stopped)

/**
 * What to show when a send didn't produce an answer.
 *
 * [offline] is the whole of the distinction and it is a Boolean rather than the pair of
 * `@StringRes` reasons it used to be: the two failures are now two *shapes*, not two sentences —
 * offline is a **state** and carries a heading, a body, the on-device fallback and two actions,
 * while a failed turn is an **event** with one action and nothing to fall back to. The screen picks
 * every one of those words, which is the rule this feature already follows: composables resolve,
 * ViewModels name.
 *
 * [insight] is the rule-based line for the same day — the identical fallback Home's insight card
 * uses, so offline the coach still says something true about today rather than only apologising.
 * It is *attributed* on screen rather than drawn as an answer, which is the whole reason the
 * failure shapes stopped being bubbles.
 */
data class CoachFailure(val offline: Boolean, val insight: String?, val question: String)

/**
 * All the screen's writes. [OnRetry] resends the question the failure is holding, so a dropped
 * connection doesn't cost the user their typing.
 *
 * [OnConfirmProposal] carries its own copy because the line naming what was written is user-facing,
 * and the screen is the only place that can resolve a resource — *composables resolve, ViewModels
 * name*, and no `Context` reaches this one. [OnDismissProposal] needs no such line: the turn is
 * persisted with the coach's prose alone.
 */
sealed interface CoachEvent {
    data class OnSend(val question: String) : CoachEvent

    /** Abandons the turn in flight. Nothing is persisted — a stopped turn is one the user walked
     * away from, which is the reading leaving the screen already had. */
    data object OnStop : CoachEvent
    data object OnRetry : CoachEvent
    data object OnClear : CoachEvent
    /** [kept] is what survived the card's per-row `✕`, which is why the screen sends the rows back
     * rather than the ViewModel reading them off the state: striking a row out is a decision the
     * user made on the card, and only what is left was agreed to.
     *
     * [receipt] is the line naming what is about to be written. Blank on the one confirm that
     * writes nothing — a drafted routine opens a form — and the repository stores it in its own
     * column rather than joined onto the answer. */
    data class OnConfirmProposal(val kept: List<CoachAction>, val receipt: String) : CoachEvent
    data object OnDismissProposal : CoachEvent
}

/**
 * One opener: the question, and the **reach** it demonstrates.
 *
 * The eyebrow is the half that earns its line. Four questions stacked as identical pills read as
 * four arbitrary examples; "TODAY / A LOGGED DAY / A SPAN / AN OPINION" over them says the set is
 * a *range* and that each one is standing in for a kind of question, which is the whole job the
 * empty state has. It is a label on the card and never sent — [question] is what the tap sends,
 * verbatim, the rule every door in this feature follows.
 */
data class Starter(@StringRes val reach: Int, @StringRes val question: Int)

/**
 * Openers for an empty conversation. A blank text box against a coach the user has never used is
 * a dead end, and these four teach its reach faster than a paragraph would: one about today, one
 * about a past day, one about a span, and one that needs an opinion. The first three exist to show
 * that the diary questions now have answers — before the tools they were the deflections.
 *
 * Four rather than three, and in this order, because they are drawn as a 2×2: the two that read
 * back a day sit on the top row and the two that reason over one sit beneath them.
 */
val STARTERS = listOf(
    Starter(R.string.coach_starter_reach_today, R.string.coach_starter_today),
    Starter(R.string.coach_starter_reach_logged, R.string.coach_starter_yesterday),
    Starter(R.string.coach_starter_reach_span, R.string.coach_starter_week),
    Starter(R.string.coach_starter_reach_opinion, R.string.coach_starter_dinner),
)
