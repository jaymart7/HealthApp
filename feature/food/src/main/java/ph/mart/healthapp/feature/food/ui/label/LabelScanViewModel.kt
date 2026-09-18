package ph.mart.healthapp.feature.food.ui.label

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.Job
import org.orbitmvi.orbit.OrbitContainerHost
import org.orbitmvi.orbit.viewmodel.orbitContainer
import ph.mart.healthapp.core.data.food.FoodEntry
import ph.mart.healthapp.core.data.food.FoodSuggestion
import ph.mart.healthapp.core.data.food.LabelScanRepository
import ph.mart.healthapp.core.data.food.FoodRepository
import ph.mart.healthapp.core.data.network.NetworkMonitor
import ph.mart.healthapp.feature.food.ui.barcode.BarcodeScanViewModel

/** Same shape as [BarcodeScanViewModel]: [Unit] state, Orbit used only to coordinate the two
 * repository calls (read the panel, log what came back). */
class LabelScanViewModel(
    private val labelScanRepository: LabelScanRepository,
    private val foodRepository: FoodRepository,
    private val networkMonitor: NetworkMonitor,
) : ViewModel(), OrbitContainerHost<Unit, Unit, LabelScanSideEffect> {

    override val container = orbitContainer<Unit, LabelScanSideEffect>(Unit)

    /** Lets back-from-Reading abandon the in-flight request, as the barcode flow does for its
     * lookup and the photo flow for its analysis. */
    private var readJob: Job? = null

    fun isOnline(): Boolean = networkMonitor.isOnline()

    fun handleEvent(event: LabelScanEvent) {
        when (event) {
            is LabelScanEvent.OnPhotoCaptured -> read(event.photo)
            LabelScanEvent.OnCancelRead -> readJob?.cancel()
            is LabelScanEvent.OnLogEntry -> logEntry(event.entry, event.keepAsFood)
        }
    }

    private fun read(photo: Bitmap) {
        readJob = intent {
            val result = labelScanRepository.read(photo)
            postSideEffect(LabelScanSideEffect.ReadFinished(result))
        }
    }

    /**
     * The pair `FoodViewModel.onSaveMyFood` already makes, in one intent so the food is kept before
     * the screen is told to leave. A food the user authored and a food they starred are the same
     * `favorite_food` row, which is why keeping one needs no second write path.
     *
     * No photo is attached. `FoodRepositoryImpl` takes one, but a picture of a nutrition panel is
     * not a picture of the meal, and the camera flow is the app's only path that attaches an image.
     */
    private fun logEntry(entry: FoodEntry, keepAsFood: FoodSuggestion?) = intent {
        if (keepAsFood != null) foodRepository.setFavorite(keepAsFood, favorite = true)
        foodRepository.addEntry(entry)
        postSideEffect(LabelScanSideEffect.EntryLogged)
    }
}
