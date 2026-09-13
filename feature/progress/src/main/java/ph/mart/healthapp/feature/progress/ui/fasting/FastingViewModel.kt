package ph.mart.healthapp.feature.progress.ui.fasting

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.combine
import org.orbitmvi.orbit.OrbitContainerHost
import org.orbitmvi.orbit.viewmodel.orbitContainer
import ph.mart.healthapp.core.data.fasting.DEFAULT_FAST_GOAL_HOURS
import ph.mart.healthapp.core.data.fasting.FastingRepository
import ph.mart.healthapp.core.data.profile.ProfileRepository

/** The Fasting page's container — `SleepViewModel`'s shape, reading the profile for the goal line
 * rather than for the switcher. Starting and ending a fast is Home's card, under its own. */
class FastingViewModel(
    fastingRepository: FastingRepository,
    profileRepository: ProfileRepository,
) : ViewModel(), OrbitContainerHost<FastingUiState, FastingUiState, Nothing> {

    override val container = orbitContainer<FastingUiState, Nothing>(FastingUiState()) {
        observeFasting(fastingRepository, profileRepository)
    }

    private fun observeFasting(
        fastingRepository: FastingRepository,
        profileRepository: ProfileRepository,
    ) = intent {
        combine(
            fastingRepository.observeSessions(),
            profileRepository.observeProfile(),
        ) { sessions, profile ->
            FastingUiState(
                sessions = sessions,
                goalHours = profile?.fastingGoalHours ?: DEFAULT_FAST_GOAL_HOURS,
            )
        }.collect { newState -> reduce { newState } }
    }
}
