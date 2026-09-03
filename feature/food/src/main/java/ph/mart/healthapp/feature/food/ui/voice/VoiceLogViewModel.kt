package ph.mart.healthapp.feature.food.ui.voice

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.Job
import org.orbitmvi.orbit.OrbitContainerHost
import org.orbitmvi.orbit.viewmodel.orbitContainer
import ph.mart.healthapp.core.data.food.FoodEntry
import ph.mart.healthapp.core.data.food.FoodRepository
import ph.mart.healthapp.core.data.food.MealParseRepository
import ph.mart.healthapp.core.data.network.NetworkMonitor

/**
 * [PhotoCaptureViewModel][ph.mart.healthapp.feature.food.ui.photo.PhotoCaptureViewModel]'s three
 * dependencies exactly, and for its reasons: no catalog or reference data to hold, so [Unit] state,
 * with Orbit doing the intent/side-effect coordination around the two repository calls (parse,
 * log). The sentence and the reviewed rows are the screen's, not the container's.
 */
class VoiceLogViewModel(
    private val mealParseRepository: MealParseRepository,
    private val foodRepository: FoodRepository,
    private val networkMonitor: NetworkMonitor,
) : ViewModel(), OrbitContainerHost<Unit, Unit, VoiceLogSideEffect> {

    override val container = orbitContainer<Unit, VoiceLogSideEffect>(Unit)

    /** Lets [VoiceLogEvent.OnCancelParse] cancel just the in-flight call, the way the photo flow's
     * `analysisJob` does — cancellation reaches the Firebase AI SDK cooperatively. */
    private var parseJob: Job? = null

    fun isOnline(): Boolean = networkMonitor.isOnline()

    fun handleEvent(event: VoiceLogEvent) {
        when (event) {
            is VoiceLogEvent.OnParse -> parse(event.text)
            VoiceLogEvent.OnCancelParse -> parseJob?.cancel()
            is VoiceLogEvent.OnLogMeal -> logMeal(event.entries)
        }
    }

    private fun parse(text: String) {
        parseJob = intent {
            val result = mealParseRepository.parse(text)
            postSideEffect(VoiceLogSideEffect.ParseFinished(result))
        }
    }

    /** One batched write, the call `onLogSavedMeal` makes — the whole meal lands in the diary in
     * one emission. */
    private fun logMeal(entries: List<FoodEntry>) = intent {
        foodRepository.addEntries(entries)
        postSideEffect(VoiceLogSideEffect.MealLogged)
    }
}
