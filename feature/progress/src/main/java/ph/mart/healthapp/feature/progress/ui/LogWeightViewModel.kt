package ph.mart.healthapp.feature.progress.ui

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import org.orbitmvi.orbit.OrbitContainerHost
import org.orbitmvi.orbit.viewmodel.orbitContainer
import ph.mart.healthapp.core.data.profile.ProfileRepository
import ph.mart.healthapp.core.data.profile.UnitSystem
import ph.mart.healthapp.core.data.progress.ProgressRepository
import ph.mart.healthapp.core.data.progress.WeightEntry

class LogWeightViewModel(
    private val progressRepository: ProgressRepository,
    private val profileRepository: ProfileRepository,
) : ViewModel(), OrbitContainerHost<LogWeightUiState, LogWeightUiState, LogWeightSideEffect> {

    override val container = orbitContainer<LogWeightUiState, LogWeightSideEffect>(LogWeightUiState()) {
        observeEntries(progressRepository, profileRepository)
        seedLatestWeight(progressRepository)
    }

    fun handleEvent(event: LogWeightEvent) {
        when (event) {
            is LogWeightEvent.OnSave -> onSave(event.form)
        }
    }

    private fun observeEntries(progressRepository: ProgressRepository, profileRepository: ProfileRepository) = intent {
        combine(progressRepository.observeWeightEntries(), profileRepository.observeProfile()) { entries, profile ->
            LogWeightUiState(entries = entries, preferredUnit = profile?.preferredUnit ?: UnitSystem.Metric)
        }.collect { newState -> reduce { newState } }
    }

    /** One-shot: seeds the stepper with the latest logged weight — this is the skill's "loading a
     * record to edit" SideEffect pattern, not a LaunchedEffect in the Composable. */
    private fun seedLatestWeight(repo: ProgressRepository) = intent {
        val latest = repo.observeWeightEntries().first().maxByOrNull { it.dateEpochDay } ?: return@intent
        postSideEffect(LogWeightSideEffect.Loaded(latest.weightKg))
    }

    private fun onSave(form: LogWeightForm) = intent {
        progressRepository.upsertWeightEntry(WeightEntry(dateEpochDay = form.dateEpochDay, weightKg = form.weightKg, note = form.note))
        postSideEffect(LogWeightSideEffect.Saved)
    }
}
