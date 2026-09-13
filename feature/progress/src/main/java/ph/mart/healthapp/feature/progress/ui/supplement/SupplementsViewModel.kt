package ph.mart.healthapp.feature.progress.ui.supplement

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.map
import org.orbitmvi.orbit.OrbitContainerHost
import org.orbitmvi.orbit.viewmodel.orbitContainer
import ph.mart.healthapp.core.data.supplement.SupplementRepository

/**
 * The Supplements page's container, and the thinnest of the thirteen: one flow, no `combine`, no
 * profile. The ticking happens on Home and the authoring in Profile, so this reads and nothing
 * else — `SleepViewModel`'s shape with the second flow it does not need taken out.
 */
class SupplementsViewModel(
    repository: SupplementRepository,
) : ViewModel(), OrbitContainerHost<SupplementsUiState, SupplementsUiState, Nothing> {

    override val container = orbitContainer<SupplementsUiState, Nothing>(SupplementsUiState()) {
        observeDays(repository)
    }

    private fun observeDays(repository: SupplementRepository) = intent {
        repository.observeDays()
            .map { days -> SupplementsUiState(days = days) }
            .collect { newState -> reduce { newState } }
    }
}
