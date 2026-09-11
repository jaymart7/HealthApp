package ph.mart.healthapp.feature.training.ui.training

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.combine
import org.orbitmvi.orbit.OrbitContainerHost
import org.orbitmvi.orbit.viewmodel.orbitContainer
import ph.mart.healthapp.core.data.exercise.ExerciseRepository
import ph.mart.healthapp.core.data.exercise.RoutineRepository
import ph.mart.healthapp.core.data.exercise.trainingWeek
import ph.mart.healthapp.core.data.profile.ProfileRepository
import ph.mart.healthapp.core.data.profile.UnitSystem
import ph.mart.healthapp.core.data.todayEpochDay

/**
 * Read-only: four flows in, one state out, no events. Every door this tab draws is a callback the
 * screen hands upward — `AppScaffold` owns the sheet and the workout route, exactly as it does for
 * the FAB and for Home's plan card.
 *
 * [ph.mart.healthapp.core.data.exercise.trainingWeek] is re-scored on every emission rather than at
 * flow-construction time, for the reason `HomeViewModel` does the same: a tab left open past
 * midnight must not keep scoring yesterday's week.
 */
class TrainingViewModel(
    exerciseRepository: ExerciseRepository,
    routineRepository: RoutineRepository,
    profileRepository: ProfileRepository,
) : ViewModel(), OrbitContainerHost<TrainingUiState, TrainingUiState, Nothing> {

    override val container = orbitContainer<TrainingUiState, Nothing>(TrainingUiState()) {
        observeTraining(exerciseRepository, routineRepository, profileRepository)
    }

    private fun observeTraining(
        exerciseRepository: ExerciseRepository,
        routineRepository: RoutineRepository,
        profileRepository: ProfileRepository,
    ) = intent {
        combine(
            exerciseRepository.observeTodayEntries(),
            exerciseRepository.observeRecentEntries(),
            routineRepository.observeRoutines(),
            profileRepository.observeProfile(),
        ) { today, recent, routines, profile ->
            TrainingUiState(
                loaded = true,
                today = today,
                recent = recent,
                routines = routines,
                trainingWeek = trainingWeek(routines, recent, todayEpochDay()),
                unit = profile?.preferredUnit ?: UnitSystem.Metric,
            )
        }.collect { newState -> reduce { newState } }
    }
}
