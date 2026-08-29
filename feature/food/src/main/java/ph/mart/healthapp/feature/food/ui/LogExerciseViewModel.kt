package ph.mart.healthapp.feature.food.ui

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.combine
import org.orbitmvi.orbit.OrbitContainerHost
import org.orbitmvi.orbit.viewmodel.orbitContainer
import ph.mart.healthapp.core.data.exercise.ExerciseRepository
import ph.mart.healthapp.core.data.profile.ProfileRepository
import ph.mart.healthapp.core.data.progress.ProgressRepository

/**
 * Read-side is one number: the weight the MET estimate multiplies by. It's the latest weigh-in
 * rather than `Profile.weightKg`, which is the onboarding weight and is never updated — same
 * fallback rule `trendVsSevenDaysAgo(fallbackKg)` uses on Home.
 */
class LogExerciseViewModel(
    private val exerciseRepository: ExerciseRepository,
    profileRepository: ProfileRepository,
    progressRepository: ProgressRepository,
) : ViewModel(), OrbitContainerHost<LogExerciseUiState, LogExerciseUiState, LogExerciseSideEffect> {

    override val container = orbitContainer<LogExerciseUiState, LogExerciseSideEffect>(LogExerciseUiState()) {
        observeWeight(profileRepository, progressRepository)
    }

    fun handleEvent(event: LogExerciseEvent) {
        when (event) {
            is LogExerciseEvent.OnSave -> onSave(event.form, event.dateEpochDay)
        }
    }

    private fun observeWeight(
        profileRepository: ProfileRepository,
        progressRepository: ProgressRepository,
    ) = intent {
        combine(
            profileRepository.observeProfile(),
            progressRepository.observeWeightEntries(),
        ) { profile, entries ->
            LogExerciseUiState(
                weightKg = entries.maxByOrNull { it.dateEpochDay }?.weightKg
                    ?: profile?.weightKg
                    ?: LogExerciseUiState().weightKg,
            )
        }.collect { newState -> reduce { newState } }
    }

    private fun onSave(form: LogExerciseForm, dateEpochDay: Long) = intent {
        exerciseRepository.addEntry(form.toExerciseEntry(dateEpochDay))
        postSideEffect(LogExerciseSideEffect.Saved)
    }
}
