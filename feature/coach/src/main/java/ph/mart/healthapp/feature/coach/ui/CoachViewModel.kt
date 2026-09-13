package ph.mart.healthapp.feature.coach.ui

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import org.orbitmvi.orbit.OrbitContainer
import org.orbitmvi.orbit.OrbitContainerHost
import org.orbitmvi.orbit.viewmodel.orbitContainer
import ph.mart.healthapp.core.data.coach.CoachAction
import ph.mart.healthapp.core.data.coach.CoachReply
import ph.mart.healthapp.core.data.coach.CoachRepository
import ph.mart.healthapp.core.data.exercise.ExerciseRepository
import ph.mart.healthapp.core.data.food.FoodRepository
import ph.mart.healthapp.core.data.insight.insightFor
import ph.mart.healthapp.core.data.insight.observeInsightRequest
import ph.mart.healthapp.core.data.network.NetworkMonitor
import ph.mart.healthapp.core.data.profile.ProfileRepository
import ph.mart.healthapp.core.data.progress.ProgressRepository
import ph.mart.healthapp.core.data.water.WaterRepository

/**
 * The five repositories are here only to build the day's payload — the screen reads none of them
 * directly. `observeInsightRequest` does the combining in `:core:data` so this and Home describe
 * the same day to the same model without either owning that knowledge.
 *
 * The conversation and the payload are combined rather than snapshotted, for the reason Home
 * combines everything: a meal logged in another tab must be in the next answer, not in the next
 * cold start.
 */
class CoachViewModel(
    private val coachRepository: CoachRepository,
    private val networkMonitor: NetworkMonitor,
    profileRepository: ProfileRepository,
    private val foodRepository: FoodRepository,
    private val progressRepository: ProgressRepository,
    waterRepository: WaterRepository,
    exerciseRepository: ExerciseRepository,
) : ViewModel(), OrbitContainerHost<CoachUiState, CoachUiState, Nothing> {

    override val container: OrbitContainer<CoachUiState, CoachUiState, Nothing> =
        orbitContainer<CoachUiState, Nothing>(CoachUiState()) {
            observeConversation(
                profileRepository,
                waterRepository,
                exerciseRepository,
            )
        }

    /** Lets [CoachEvent.OnStop] cancel just the turn in flight — the shape
     * `PhotoCaptureViewModel` already uses for its recognition call, and cancellation reaches the
     * Firebase AI SDK cooperatively through structured concurrency. */
    private var sendJob: Job? = null

    fun handleEvent(event: CoachEvent) {
        when (event) {
            is CoachEvent.OnSend -> onSend(event.question)
            CoachEvent.OnStop -> onStop()
            CoachEvent.OnRetry -> onRetry()
            CoachEvent.OnClear -> intent { coachRepository.clear() }
            is CoachEvent.OnConfirmProposal -> onSettle(event.kept, event.loggedLine)
            CoachEvent.OnDismissProposal -> onSettle(emptyList(), null)
        }
    }

    private fun observeConversation(
        profileRepository: ProfileRepository,
        waterRepository: WaterRepository,
        exerciseRepository: ExerciseRepository,
    ) = intent {
        combine(
            coachRepository.observeMessages(),
            observeInsightRequest(
                profileRepository,
                foodRepository,
                progressRepository,
                waterRepository,
                exerciseRepository,
            ),
            // Paired rather than folded here: `state` inside a `combine` transform is read when
            // the transform runs, so building the new state there would carry a snapshot of the
            // in-flight turn from before whatever arrived since.
        ) { messages, request -> messages to request }.collect { (messages, request) ->
            reduce { state.withMessages(messages, request) }
        }
    }

    /**
     * Ends the turn where it stands. Nothing is persisted, which is not a special case: the
     * repository only ever writes a question once it has an answer, so a stopped turn is exactly
     * a turn the user walked away from.
     *
     * The clearing is its own intent because a cancelled one cannot reduce — the same reason
     * [onSettle]'s empty-answer branch exists.
     */
    private fun onStop() {
        sendJob?.cancel()
        intent { reduce { state.withTurnAbandoned() } }
    }

    private fun onRetry() = intent {
        state.failure?.question?.let(::onSend)
    }

    /**
     * Ends a turn that stopped on a proposal. [loggedLine] non-null is a confirmation: the
     * repository commits [kept] and the line is appended to what gets persisted, so reopening the
     * chat still shows that something was logged. Null is a dismissal.
     *
     * Nothing is cleared here on success, for the reason [onSend] gives — `withMessages` is what
     * knows when Room has the rows. The one case that needs clearing is a dismissal of a proposal
     * that came with no prose: there is no answer to persist, so no write happens, so no emission
     * arrives to retire the bubbles.
     */
    private fun onSettle(kept: List<CoachAction>, loggedLine: String?) = intent {
        if (state.proposal.isEmpty()) return@intent
        val question = state.pending ?: return@intent
        val answer = listOfNotNull(state.streaming, loggedLine).joinToString("\n")
        if (answer.isEmpty()) {
            return@intent reduce { state.withTurnAbandoned() }
        }
        // [kept] rather than `state.proposal`: the card is where a row was struck out, and what
        // comes back from it is what the user agreed to. A dismissal sends nothing at all.
        coachRepository.settle(question, answer, kept)
    }

    /**
     * Offline the model is never asked at all — the check is the same `NetworkMonitor` recheck
     * Home makes before its one insight call. Either way a failure writes nothing: the repository
     * only persists a question once it has an answer, so a retry is a fresh send and not a repair.
     *
     * The question goes into the state before the first chunk, so it is on screen from the tap
     * rather than appearing above a finished answer. Nothing clears it here on success: the
     * repository's flow completing is not the moment Room has the rows, and `withMessages` is what
     * knows that.
     *
     * A third ending arrives now: a [CoachReply.Proposal] leaves the turn in flight on purpose —
     * the coach has drafted a row nobody has agreed to, so no write has happened and the bubbles
     * must stay up. [onSettle] is what ends it.
     */
    private fun onSend(question: String) {
        sendJob = intent {
            val text = question.trim()
            if (text.isEmpty() || state.pending != null) return@intent
            reduce {
                state.copy(pending = text, streaming = null, failure = null, proposal = emptyList())
            }

            // Read once and reused for the message below: a second recheck could disagree with
            // the one that decided whether to call, and then an offline send would report a model
            // failure.
            val online = networkMonitor.isOnline()
            val replies =
                if (online) coachRepository.send(text, state.request) else flowOf(CoachReply.Failed)

            replies.collect { reply ->
                reduce {
                    when (reply) {
                        // Empty is the preface of a tool round being dropped, and `null` is what
                        // puts the thinking mascot back while the tool runs — an empty bubble
                        // would be the wrong half of that.
                        is CoachReply.Partial -> state.copy(streaming = reply.text.takeIf(String::isNotEmpty))
                        // Nothing is written and nothing is cleared: the turn stays in flight, on
                        // screen, until the user's tap ends it through `onSettle`.
                        is CoachReply.Proposal -> state.copy(proposal = reply.actions)
                        CoachReply.Failed -> state.copy(
                            pending = null,
                            streaming = null,
                            proposal = emptyList(),
                            failure = CoachFailure(
                                reason = if (online) FAILED_REASON else OFFLINE_REASON,
                                insight = state.request?.let(::insightFor),
                                question = text,
                            ),
                        )
                    }
                }
            }
        }
    }
}
