package ph.mart.healthapp.feature.progress.ui.heart

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.combine
import org.orbitmvi.orbit.OrbitContainerHost
import org.orbitmvi.orbit.viewmodel.orbitContainer
import ph.mart.healthapp.core.data.health.HeartRepository
import ph.mart.healthapp.core.data.profile.ProfileRepository

/** The Heart page's container — `SleepViewModel`'s shape, and import-only for the same reason:
 * FitPulse takes no heart samples of its own. */
class HeartViewModel(
    heartRepository: HeartRepository,
    profileRepository: ProfileRepository,
) : ViewModel(), OrbitContainerHost<HeartUiState, HeartUiState, Nothing> {

    override val container = orbitContainer<HeartUiState, Nothing>(HeartUiState()) {
        observeHeart(heartRepository, profileRepository)
    }

    private fun observeHeart(
        heartRepository: HeartRepository,
        profileRepository: ProfileRepository,
    ) = intent {
        combine(
            heartRepository.observeDays(),
            profileRepository.observeProfile(),
        ) { days, profile ->
            HeartUiState(days = days, cycleTrackingOn = profile?.cycleTrackingOn == true)
        }.collect { newState -> reduce { newState } }
    }
}
