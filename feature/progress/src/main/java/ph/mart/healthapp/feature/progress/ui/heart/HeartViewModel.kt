package ph.mart.healthapp.feature.progress.ui.heart

import androidx.lifecycle.ViewModel
import org.orbitmvi.orbit.OrbitContainerHost
import org.orbitmvi.orbit.viewmodel.orbitContainer
import ph.mart.healthapp.core.data.health.HeartRepository

/** The Heart page's container — `SleepViewModel`'s shape, and import-only for the same reason:
 * FitPulse takes no heart samples of its own. */
class HeartViewModel(
    heartRepository: HeartRepository,
) : ViewModel(), OrbitContainerHost<HeartUiState, HeartUiState, Nothing> {

    override val container = orbitContainer<HeartUiState, Nothing>(HeartUiState()) {
        observeHeart(heartRepository)
    }

    private fun observeHeart(heartRepository: HeartRepository) = intent {
        heartRepository.observeDays().collect { days -> reduce { HeartUiState(days = days) } }
    }
}
