package ph.mart.healthapp.feature.food.ui.voice

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.Job
import org.orbitmvi.orbit.OrbitContainerHost
import org.orbitmvi.orbit.viewmodel.orbitContainer
import ph.mart.healthapp.core.data.food.FoodEntry
import ph.mart.healthapp.core.data.food.FoodRepository
import ph.mart.healthapp.core.data.food.MealParseRepository
import ph.mart.healthapp.core.data.network.NetworkMonitor

/** Three is what fits under the field without pushing the meal chips and the button off the top
 * of the keyboard — the number `FoodHistoryViewModel` offers, for a neighbouring reason. */
private const val RECENT_SENTENCES = 3

/**
 * [PhotoCaptureViewModel][ph.mart.healthapp.feature.food.ui.photo.PhotoCaptureViewModel]'s three
 * dependencies exactly, and for its reasons: Orbit does the intent/side-effect coordination around
 * the repository calls (parse, log), and the sentence being typed, the slot and the reviewed rows
 * are the screen's rather than the container's.
 *
 * The state it does hold is one list that outlives the screen: the sentences that have already
 * become meals, mirrored from Room so the strip re-draws the moment [logMeal] records one.
 */
class VoiceLogViewModel(
    private val mealParseRepository: MealParseRepository,
    private val foodRepository: FoodRepository,
    private val networkMonitor: NetworkMonitor,
) : ViewModel(), OrbitContainerHost<VoiceLogUiState, VoiceLogUiState, VoiceLogSideEffect> {

    override val container = orbitContainer<VoiceLogUiState, VoiceLogSideEffect>(VoiceLogUiState()) {
        observeRecentSentences()
    }

    /** Lets [VoiceLogEvent.OnCancelParse] cancel just the in-flight call, the way the photo flow's
     * `analysisJob` does — cancellation reaches the Firebase AI SDK cooperatively. */
    private var parseJob: Job? = null

    fun isOnline(): Boolean = networkMonitor.isOnline()

    fun handleEvent(event: VoiceLogEvent) {
        when (event) {
            is VoiceLogEvent.OnParse -> parse(event.text)
            VoiceLogEvent.OnCancelParse -> parseJob?.cancel()
            is VoiceLogEvent.OnLogMeal -> logMeal(event.entries, event.sentence)
        }
    }

    private fun observeRecentSentences() = intent {
        foodRepository.observeRecentSentences(RECENT_SENTENCES).collect { sentences ->
            reduce { state.copy(recentSentences = sentences) }
        }
    }

    private fun parse(text: String) {
        parseJob = intent {
            val result = mealParseRepository.parse(text)
            postSideEffect(VoiceLogSideEffect.ParseFinished(result))
        }
    }

    /**
     * One batched write, the call `onLogSavedMeal` makes — the whole meal lands in the diary in
     * one emission.
     *
     * The sentence is remembered after the rows are written and not before: what is being recorded
     * is that it produced a meal, so a write that never happened records nothing.
     */
    private fun logMeal(entries: List<FoodEntry>, sentence: String) = intent {
        foodRepository.addEntries(entries)
        foodRepository.recordSentence(sentence)
        postSideEffect(VoiceLogSideEffect.MealLogged)
    }
}
