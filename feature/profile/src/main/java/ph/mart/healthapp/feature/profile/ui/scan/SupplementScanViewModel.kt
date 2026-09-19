package ph.mart.healthapp.feature.profile.ui.scan

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.Job
import org.orbitmvi.orbit.OrbitContainerHost
import org.orbitmvi.orbit.viewmodel.orbitContainer
import ph.mart.healthapp.core.data.network.NetworkMonitor
import ph.mart.healthapp.core.data.supplement.SUPPLEMENT_TIMES_PER_DAY
import ph.mart.healthapp.core.data.supplement.Supplement
import ph.mart.healthapp.core.data.supplement.SupplementRepository
import ph.mart.healthapp.core.data.supplement.SupplementScanRepository

/**
 * [Unit] state, Orbit used only to coordinate the two repository calls — read the panel, keep what
 * came back. `LabelScanViewModel`'s shape, one domain over.
 */
class SupplementScanViewModel(
    private val supplementScanRepository: SupplementScanRepository,
    private val supplementRepository: SupplementRepository,
    private val networkMonitor: NetworkMonitor,
) : ViewModel(), OrbitContainerHost<Unit, Unit, SupplementScanSideEffect> {

    override val container = orbitContainer<Unit, SupplementScanSideEffect>(Unit)

    /** Lets back-from-Reading abandon the in-flight request, as every other AI flow here does. */
    private var readJob: Job? = null

    fun isOnline(): Boolean = networkMonitor.isOnline()

    fun handleEvent(event: SupplementScanEvent) {
        when (event) {
            is SupplementScanEvent.OnPhotoCaptured -> read(event.photo)
            SupplementScanEvent.OnCancelRead -> readJob?.cancel()
            is SupplementScanEvent.OnSave -> save(event.supplement)
        }
    }

    private fun read(photo: Bitmap) {
        readJob = intent {
            val result = supplementScanRepository.read(photo)
            postSideEffect(SupplementScanSideEffect.ReadFinished(result))
        }
    }

    /**
     * Always an add: this flow has no supplement to edit, so `id` stays 0 and Room generates one.
     * The trim and the clamp are `SupplementsViewModel.onSave`'s, repeated rather than shared for
     * the reason the two screens are separate — a blank name is refused at the sheet's Save button
     * either way, and this is the guard behind it.
     */
    private fun save(supplement: Supplement) = intent {
        val cleaned = supplement.copy(
            id = 0,
            name = supplement.name.trim(),
            dose = supplement.dose.trim(),
            timesPerDay = supplement.timesPerDay.coerceIn(SUPPLEMENT_TIMES_PER_DAY),
        )
        if (cleaned.name.isBlank()) return@intent
        supplementRepository.addSupplement(cleaned)
        postSideEffect(SupplementScanSideEffect.Saved)
    }
}
