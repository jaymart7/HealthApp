package ph.mart.healthapp.feature.progress.ui.sleep

import androidx.lifecycle.ViewModel
import org.orbitmvi.orbit.OrbitContainerHost
import org.orbitmvi.orbit.viewmodel.orbitContainer
import ph.mart.healthapp.core.data.health.SleepRepository

/**
 * The Sleep page's container — the first of thirteen, and the shape the other twelve copy.
 *
 * It writes nothing, and the flow it reads is already streamed by `ProgressViewModel`. That
 * duplication is the price of the page being a route, and it is a **slice**, not a second copy of
 * the tab's thirteen repositories: one flow, alive only while this entry is on the back stack.
 * See `DECISIONS.md` -> **Progress, recap & the energy check-in**.
 */
class SleepViewModel(
    sleepRepository: SleepRepository,
) : ViewModel(), OrbitContainerHost<SleepUiState, SleepUiState, Nothing> {

    override val container = orbitContainer<SleepUiState, Nothing>(SleepUiState()) {
        observeSleep(sleepRepository)
    }

    private fun observeSleep(sleepRepository: SleepRepository) = intent {
        sleepRepository.observeNights().collect { nights -> reduce { SleepUiState(nights = nights) } }
    }
}
