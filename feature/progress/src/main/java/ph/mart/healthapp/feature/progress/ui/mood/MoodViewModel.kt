package ph.mart.healthapp.feature.progress.ui.mood

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.combine
import org.orbitmvi.orbit.OrbitContainerHost
import org.orbitmvi.orbit.viewmodel.orbitContainer
import ph.mart.healthapp.core.data.mood.MoodRepository
import ph.mart.healthapp.core.data.profile.ProfileRepository

/**
 * The Mood page's container — `SleepViewModel`'s shape and its reasoning. It writes nothing; the
 * two-tap reflection that fills this series is Home's card, under its own container.
 */
class MoodViewModel(
    moodRepository: MoodRepository,
    profileRepository: ProfileRepository,
) : ViewModel(), OrbitContainerHost<MoodUiState, MoodUiState, Nothing> {

    override val container = orbitContainer<MoodUiState, Nothing>(MoodUiState()) {
        observeMood(moodRepository, profileRepository)
    }

    private fun observeMood(
        moodRepository: MoodRepository,
        profileRepository: ProfileRepository,
    ) = intent {
        combine(
            moodRepository.observeDays(),
            profileRepository.observeProfile(),
        ) { days, profile ->
            MoodUiState(days = days, cycleTrackingOn = profile?.cycleTrackingOn == true)
        }.collect { newState -> reduce { newState } }
    }
}
