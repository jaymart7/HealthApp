package ph.mart.healthapp.wear.ui

import androidx.lifecycle.ViewModel
import org.orbitmvi.orbit.OrbitContainer
import org.orbitmvi.orbit.OrbitContainerHost
import org.orbitmvi.orbit.viewmodel.orbitContainer
import ph.mart.healthapp.wear.data.WearSnapshotRepository

/**
 * One repository, because the watch keeps nothing: everything drawn arrives from the phone and
 * both taps go back to it. The phone's own ViewModels take repository interfaces the same way —
 * the difference is that theirs read Room and this one reads a data item.
 */
class WearTodayViewModel(
    private val repository: WearSnapshotRepository,
) : ViewModel(), OrbitContainerHost<WearTodayUiState, WearTodayUiState, WearTodaySideEffect> {

    override val container: OrbitContainer<WearTodayUiState, WearTodayUiState, WearTodaySideEffect> =
        orbitContainer<WearTodayUiState, WearTodaySideEffect>(WearTodayUiState()) {
            observeSnapshots()
        }

    fun handleEvent(event: WearTodayEvent) {
        when (event) {
            WearTodayEvent.OnAddGlass -> send(repository::addGlass)
            WearTodayEvent.OnToggleFast -> send(repository::toggleFast)
        }
    }

    private fun observeSnapshots() = intent {
        repository.snapshots.collect { snapshot ->
            reduce { state.copy(loaded = true, snapshot = snapshot) }
        }
    }

    /**
     * The reply is never applied to the state: what the user sees comes back as a fresh push from
     * the phone, which is the only device that knows what was actually written. A failure raises
     * the side effect instead — see [WearTodaySideEffect.PhoneUnreachable].
     */
    private fun send(action: suspend () -> Boolean) = intent {
        if (state.sending) return@intent
        reduce { state.copy(sending = true) }
        val delivered = action()
        reduce { state.copy(sending = false) }
        if (!delivered) postSideEffect(WearTodaySideEffect.PhoneUnreachable)
    }
}
