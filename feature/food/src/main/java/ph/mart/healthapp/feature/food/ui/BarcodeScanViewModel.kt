package ph.mart.healthapp.feature.food.ui

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.Job
import org.orbitmvi.orbit.OrbitContainerHost
import org.orbitmvi.orbit.viewmodel.orbitContainer
import ph.mart.healthapp.core.data.food.BarcodeLookupRepository
import ph.mart.healthapp.core.data.food.FoodEntry
import ph.mart.healthapp.core.data.food.FoodRepository
import ph.mart.healthapp.core.data.network.NetworkMonitor

/** Same shape as [PhotoCaptureViewModel]: [Unit] state, Orbit used only to coordinate the two
 * repository calls (look up, log). */
class BarcodeScanViewModel(
    private val barcodeLookupRepository: BarcodeLookupRepository,
    private val foodRepository: FoodRepository,
    private val networkMonitor: NetworkMonitor,
) : ViewModel(), OrbitContainerHost<Unit, Unit, BarcodeScanSideEffect> {

    override val container = orbitContainer<Unit, BarcodeScanSideEffect>(Unit)

    /** Lets back-from-LookingUp abandon the in-flight request, as [PhotoCaptureViewModel] does for
     * the analysis call. */
    private var lookupJob: Job? = null

    fun isOnline(): Boolean = networkMonitor.isOnline()

    fun handleEvent(event: BarcodeScanEvent) {
        when (event) {
            is BarcodeScanEvent.OnBarcodeScanned -> lookUp(event.barcode)
            BarcodeScanEvent.OnCancelLookup -> lookupJob?.cancel()
            is BarcodeScanEvent.OnLogEntry -> logEntry(event.entry)
        }
    }

    private fun lookUp(barcode: String) {
        lookupJob = intent {
            val result = barcodeLookupRepository.lookup(barcode)
            postSideEffect(BarcodeScanSideEffect.LookupFinished(result))
        }
    }

    private fun logEntry(entry: FoodEntry) = intent {
        foodRepository.addEntry(entry)
        postSideEffect(BarcodeScanSideEffect.EntryLogged)
    }
}
