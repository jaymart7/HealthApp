package ph.mart.healthapp.feature.progress.ui.weight

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import org.orbitmvi.orbit.OrbitContainerHost
import org.orbitmvi.orbit.viewmodel.orbitContainer
import ph.mart.healthapp.core.data.profile.ProfileRepository
import ph.mart.healthapp.core.data.profile.UnitSystem
import ph.mart.healthapp.core.data.progress.ProgressRepository
import ph.mart.healthapp.core.data.progress.WeightEntry

/** Wide enough to hold every real body weight, narrow enough that a slipped finger cannot write a
 * 3kg weigh-in that then owns the chart's whole y-axis. */
private val WEIGHT_KG = 20.0..400.0

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
            is LogWeightEvent.OnDelete -> onDelete(event.dateEpochDay)
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

    /** The typed field is deliberately unclamped so a figure can be retyped digit by digit, so the
     * clamp lands here — the same band and the same clamp-at-the-edges-rather-than-validate-after
     * the profile's weight setters use. */
    private fun onSave(form: LogWeightForm) = intent {
        val weightKg = form.weightKg.coerceIn(WEIGHT_KG)
        progressRepository.upsertWeightEntry(WeightEntry(dateEpochDay = form.dateEpochDay, weightKg = weightKg, note = form.note))
        postSideEffect(LogWeightSideEffect.Saved)
    }

    /** A hard delete, and the one in this app that is: the row is keyed by its date and nothing
     * points at it, so there is no referent a soft delete would be keeping alive. */
    private fun onDelete(dateEpochDay: Long) = intent {
        progressRepository.deleteWeightEntry(dateEpochDay)
        postSideEffect(LogWeightSideEffect.Saved)
    }
}
