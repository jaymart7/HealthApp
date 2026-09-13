package ph.mart.healthapp.feature.progress.ui.activity

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.combine
import org.orbitmvi.orbit.OrbitContainerHost
import org.orbitmvi.orbit.viewmodel.orbitContainer
import ph.mart.healthapp.core.data.exercise.ExerciseRepository
import ph.mart.healthapp.core.data.health.DEFAULT_STEP_GOAL
import ph.mart.healthapp.core.data.health.StepsRepository
import ph.mart.healthapp.core.data.profile.ProfileRepository

/**
 * The Activity page's container, and the first of the thirteen to read three flows: the burn
 * series is imported steps *and* logged workouts folded together, and the goal line is the
 * profile's. Still a slice — `ProgressViewModel` reads thirteen.
 */
class ActivityViewModel(
    stepsRepository: StepsRepository,
    exerciseRepository: ExerciseRepository,
    profileRepository: ProfileRepository,
) : ViewModel(), OrbitContainerHost<ActivityUiState, ActivityUiState, Nothing> {

    override val container = orbitContainer<ActivityUiState, Nothing>(ActivityUiState()) {
        observeActivity(stepsRepository, exerciseRepository, profileRepository)
    }

    private fun observeActivity(
        stepsRepository: StepsRepository,
        exerciseRepository: ExerciseRepository,
        profileRepository: ProfileRepository,
    ) = intent {
        combine(
            stepsRepository.observeDays(),
            exerciseRepository.observeRecentEntries(),
            profileRepository.observeProfile(),
        ) { stepDays, entries, profile ->
            ActivityUiState(
                stepDays = stepDays,
                exerciseEntries = entries,
                stepGoal = profile?.stepGoal ?: DEFAULT_STEP_GOAL,
            )
        }.collect { newState -> reduce { newState } }
    }
}
