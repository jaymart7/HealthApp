package ph.mart.healthapp.feature.progress.ui.energy

import androidx.lifecycle.ViewModel
import org.orbitmvi.orbit.OrbitContainerHost
import org.orbitmvi.orbit.viewmodel.orbitContainer
import ph.mart.healthapp.core.data.profile.ProfileRepository

/**
 * The energy flow's only container, and the one place the Progress tab writes a calorie target.
 *
 * It exists for the same reason `BloodPressureViewModel` does: `ProgressViewModel` is the
 * read-only container its KDoc says it is, and the check-in's Apply button has to write. The card
 * and the overlay sit under one `ViewModelStoreOwner`, so `koinViewModel()` hands them the same
 * instance.
 */
class EnergyCheckInViewModel(
    private val repository: ProfileRepository,
) : ViewModel(), OrbitContainerHost<EnergyCheckInUiState, EnergyCheckInUiState, Nothing> {

    override val container = orbitContainer<EnergyCheckInUiState, Nothing>(EnergyCheckInUiState()) {
        observeProfile()
    }

    fun handleEvent(event: EnergyCheckInEvent) {
        when (event) {
            is EnergyCheckInEvent.OnApply -> onApply(event.kcal)
        }
    }

    private fun observeProfile() = intent {
        repository.observeProfile().collect { profile -> reduce { EnergyCheckInUiState(profile) } }
    }

    /**
     * The measured target is written to `calorieOverrideKcal` — the column a manual target already
     * uses, so there is no second kind of pinned target to reconcile and Profile → Goals' "Reset to
     * calculated" already undoes it. Pinning is the honest reading: a measurement is not a formula
     * that should keep moving with the next weigh-in.
     *
     * The profile is read from state rather than the repository so an Apply can only ever land on
     * the row the screen was showing.
     */
    private fun onApply(kcal: Int) = intent {
        val profile = state.profile ?: return@intent
        repository.saveProfile(profile.copy(calorieOverrideKcal = kcal))
    }
}
