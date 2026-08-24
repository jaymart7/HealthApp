package ph.mart.healthapp.feature.food.ui

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.Job
import org.orbitmvi.orbit.OrbitContainerHost
import org.orbitmvi.orbit.viewmodel.orbitContainer
import ph.mart.healthapp.core.data.food.FoodEntry
import ph.mart.healthapp.core.data.food.FoodRecognitionRepository
import ph.mart.healthapp.core.data.food.FoodRepository
import ph.mart.healthapp.core.data.network.NetworkMonitor

/** No catalog/reference data to hold — [Unit] state, Orbit used purely for the intent/side-effect
 * coordination around the two repository calls (recognize, log). */
class PhotoCaptureViewModel(
    private val recognitionRepository: FoodRecognitionRepository,
    private val foodRepository: FoodRepository,
    private val networkMonitor: NetworkMonitor,
) : ViewModel(), OrbitContainerHost<Unit, Unit, PhotoCaptureSideEffect> {

    override val container = orbitContainer<Unit, PhotoCaptureSideEffect>(Unit)

    /** Lets [PhotoCaptureEvent.OnCancelAnalysis] cancel just the in-flight recognition call —
     * cancellation reaches the Firebase AI SDK call cooperatively via structured concurrency. */
    private var analysisJob: Job? = null

    fun isOnline(): Boolean = networkMonitor.isOnline()

    fun handleEvent(event: PhotoCaptureEvent) {
        when (event) {
            is PhotoCaptureEvent.OnCapture -> analyze(event.photo)
            PhotoCaptureEvent.OnCancelAnalysis -> analysisJob?.cancel()
            is PhotoCaptureEvent.OnLogMeal -> logMeal(event.entry)
        }
    }

    private fun analyze(photo: Bitmap) {
        analysisJob = intent {
            val result = recognitionRepository.recognize(photo)
            postSideEffect(PhotoCaptureSideEffect.RecognitionFinished(result))
        }
    }

    private fun logMeal(entry: FoodEntry) = intent {
        foodRepository.addEntry(entry)
        postSideEffect(PhotoCaptureSideEffect.MealLogged)
    }
}
