package ph.mart.healthapp.feature.progress.ui.cycle

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.map
import org.orbitmvi.orbit.OrbitContainerHost
import org.orbitmvi.orbit.viewmodel.orbitContainer
import ph.mart.healthapp.core.data.cycle.CycleRepository

/**
 * The Cycle page's container — read-only, one flow, `SupplementsViewModel`'s shape. The writing
 * stays with [LogCycleViewModel], which the sheet this page opens brings with it: the page and the
 * sheet sit under one `ViewModelStoreOwner` on this route, so neither has to know about the other.
 */
class CycleViewModel(
    repository: CycleRepository,
) : ViewModel(), OrbitContainerHost<CycleUiState, CycleUiState, Nothing> {

    override val container = orbitContainer<CycleUiState, Nothing>(CycleUiState()) {
        observeDays(repository)
    }

    private fun observeDays(repository: CycleRepository) = intent {
        repository.observeDays()
            .map { days -> CycleUiState(days = days) }
            .collect { newState -> reduce { newState } }
    }
}
