package ph.mart.healthapp.feature.progress.ui.sleep

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.combine
import org.orbitmvi.orbit.OrbitContainerHost
import org.orbitmvi.orbit.viewmodel.orbitContainer
import ph.mart.healthapp.core.data.health.SleepRepository
import ph.mart.healthapp.core.data.profile.ProfileRepository

/**
 * The Sleep page's container — the first of thirteen, and the shape the other twelve copy.
 *
 * It writes nothing, and both flows it reads are already streamed by `ProgressViewModel`. That
 * duplication is the price of the page being a route, and it is a **slice**, not a second copy of
 * the tab's thirteen repositories: two flows, alive only while this entry is on the back stack.
 * See `DECISIONS.md` -> **Progress, recap & the energy check-in**.
 *
 * The profile is read for one boolean, `cycleTrackingOn` — see [SleepUiState].
 */
class SleepViewModel(
    sleepRepository: SleepRepository,
    profileRepository: ProfileRepository,
) : ViewModel(), OrbitContainerHost<SleepUiState, SleepUiState, Nothing> {

    override val container = orbitContainer<SleepUiState, Nothing>(SleepUiState()) {
        observeSleep(sleepRepository, profileRepository)
    }

    private fun observeSleep(
        sleepRepository: SleepRepository,
        profileRepository: ProfileRepository,
    ) = intent {
        combine(
            sleepRepository.observeNights(),
            profileRepository.observeProfile(),
        ) { nights, profile ->
            SleepUiState(nights = nights, cycleTrackingOn = profile?.cycleTrackingOn == true)
        }.collect { newState -> reduce { newState } }
    }
}
