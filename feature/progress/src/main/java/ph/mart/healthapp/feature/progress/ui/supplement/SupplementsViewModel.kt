package ph.mart.healthapp.feature.progress.ui.supplement

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.combine
import org.orbitmvi.orbit.OrbitContainerHost
import org.orbitmvi.orbit.viewmodel.orbitContainer
import ph.mart.healthapp.core.data.supplement.SupplementRepository

/**
 * The Supplements page's container: the whole log and the active list, joined so the catch-up
 * checklist can price a past day off that day's own rows and still name a supplement that has
 * never been ticked.
 *
 * The one Progress subject page that writes. Authoring is still Profile's list and today is still
 * Home's card — this only corrects a day that has already gone by, which neither of those can
 * reach.
 */
class SupplementsViewModel(
    private val repository: SupplementRepository,
) : ViewModel(), OrbitContainerHost<SupplementsUiState, SupplementsUiState, Nothing> {

    override val container = orbitContainer<SupplementsUiState, Nothing>(SupplementsUiState()) {
        observeSupplements()
    }

    private fun observeSupplements() = intent {
        combine(repository.observeDays(), repository.observeSupplements()) { days, supplements ->
            SupplementsUiState(days = days, supplements = supplements)
        }.collect { newState -> reduce { newState } }
    }

    /** Clamped and seeded in the repository, and a future date refused there — the screen's own
     * bound on the stepper is the affordance, not the guard. */
    fun setTaken(date: Long, supplementId: Long, taken: Int) = intent {
        repository.setTakenOn(date, supplementId, taken)
    }
}
