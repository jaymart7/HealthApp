package ph.mart.healthapp.feature.coach.ui

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import org.orbitmvi.orbit.OrbitContainer
import org.orbitmvi.orbit.OrbitContainerHost
import org.orbitmvi.orbit.viewmodel.orbitContainer
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

    fun handleEvent(event: CoachEvent) {
        when (event) {
            is CoachEvent.OnSend -> onSend(event.question)
            CoachEvent.OnRetry -> onRetry()
            CoachEvent.OnClear -> intent { coachRepository.clear() }
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

    private fun onRetry() = intent {
        state.failure?.question?.let(::onSend)
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
     */
    private fun onSend(question: String) = intent {
        val text = question.trim()
        if (text.isEmpty() || state.pending != null) return@intent
        reduce { state.copy(pending = text, streaming = null, failure = null) }

        // Read once and reused for the message below: a second recheck could disagree with the
        // one that decided whether to call, and then an offline send would report a model failure.
        val online = networkMonitor.isOnline()
        val replies =
            if (online) coachRepository.send(text, state.request) else flowOf(CoachReply.Failed)

        replies.collect { reply ->
            reduce {
                when (reply) {
                    is CoachReply.Partial -> state.copy(streaming = reply.text)
                    CoachReply.Failed -> state.copy(
                        pending = null,
                        streaming = null,
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
