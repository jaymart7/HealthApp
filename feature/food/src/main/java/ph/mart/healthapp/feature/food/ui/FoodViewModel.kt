package ph.mart.healthapp.feature.food.ui

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.combine
import org.orbitmvi.orbit.OrbitContainerHost
import org.orbitmvi.orbit.viewmodel.orbitContainer
import ph.mart.healthapp.core.data.exercise.ExerciseRepository
import ph.mart.healthapp.core.data.food.FoodRepository
import ph.mart.healthapp.core.data.profile.ProfileRepository
import ph.mart.healthapp.core.data.profile.UnitSystem
import ph.mart.healthapp.core.data.profile.dailyTargets
import ph.mart.healthapp.core.data.water.DEFAULT_WATER_GOAL_GLASSES
import ph.mart.healthapp.core.data.water.WaterRepository

/** No side effects: the add-entry sheet dismisses itself optimistically in [FoodScreen], same
 * pattern [ph.mart.healthapp.ui.QuickActionSheet] already uses — nothing here needs to round-trip
 * through a SideEffect. */
sealed interface FoodSideEffect

class FoodViewModel(
    private val foodRepository: FoodRepository,
    profileRepository: ProfileRepository,
    private val waterRepository: WaterRepository,
    private val exerciseRepository: ExerciseRepository,
) : ViewModel(), OrbitContainerHost<FoodUiState, FoodUiState, FoodSideEffect> {

    override val container = orbitContainer<FoodUiState, FoodSideEffect>(FoodUiState()) {
        observeDiary(foodRepository, profileRepository, waterRepository, exerciseRepository)
    }

    fun handleEvent(event: FoodEvent) {
        when (event) {
            is FoodEvent.OnAddEntry -> onAddEntry(event.form)
            is FoodEvent.OnDeleteEntry -> onDeleteEntry(event.id)
            is FoodEvent.OnToggleFavorite -> onToggleFavorite(event)
            is FoodEvent.OnSetWaterGlasses -> onSetWaterGlasses(event.glasses)
            is FoodEvent.OnDeleteExercise -> onDeleteExercise(event.id)
        }
    }

    private fun observeDiary(
        foodRepository: FoodRepository,
        profileRepository: ProfileRepository,
        waterRepository: WaterRepository,
        exerciseRepository: ExerciseRepository,
    ) = intent {
        combine(
            foodRepository.observeTodayEntries(),
            profileRepository.observeProfile(),
            foodRepository.observeSuggestions(),
            waterRepository.observeToday(),
            exerciseRepository.observeTodayEntries(),
        ) { entries, profile, suggestions, waterGlasses, exercise ->
            FoodUiState(
                entries = entries,
                exercise = exercise,
                addExerciseToBudget = profile?.addExerciseToBudget != false,
                targets = profile?.dailyTargets(),
                suggestions = suggestions,
                waterGlasses = waterGlasses,
                waterGoalGlasses = profile?.waterGoalGlasses ?: DEFAULT_WATER_GOAL_GLASSES,
                unit = profile?.preferredUnit ?: UnitSystem.Metric,
            )
        }.collect { newState -> reduce { newState } }
    }

    private fun onSetWaterGlasses(glasses: Int) = intent {
        waterRepository.setToday(glasses)
    }

    private fun onAddEntry(form: AddEntryForm) = intent {
        foodRepository.addEntry(form.toFoodEntry())
    }

    private fun onDeleteEntry(id: Long) = intent {
        foodRepository.deleteEntry(id)
    }

    private fun onDeleteExercise(id: Long) = intent {
        exerciseRepository.deleteEntry(id)
    }

    private fun onToggleFavorite(event: FoodEvent.OnToggleFavorite) = intent {
        foodRepository.setFavorite(event.suggestion, event.favorite)
    }
}
