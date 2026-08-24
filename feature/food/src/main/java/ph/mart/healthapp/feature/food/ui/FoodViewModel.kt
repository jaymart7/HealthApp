package ph.mart.healthapp.feature.food.ui

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.combine
import org.orbitmvi.orbit.OrbitContainerHost
import org.orbitmvi.orbit.viewmodel.orbitContainer
import ph.mart.healthapp.core.data.food.FoodRepository
import ph.mart.healthapp.core.data.profile.ProfileRepository
import ph.mart.healthapp.core.data.profile.dailyTargets

/** No side effects: the add-entry sheet dismisses itself optimistically in [FoodScreen], same
 * pattern [ph.mart.healthapp.ui.QuickActionSheet] already uses — nothing here needs to round-trip
 * through a SideEffect. */
sealed interface FoodSideEffect

class FoodViewModel(
    private val foodRepository: FoodRepository,
    profileRepository: ProfileRepository,
) : ViewModel(), OrbitContainerHost<FoodUiState, FoodUiState, FoodSideEffect> {

    override val container = orbitContainer<FoodUiState, FoodSideEffect>(FoodUiState()) {
        observeDiary(foodRepository, profileRepository)
    }

    fun handleEvent(event: FoodEvent) {
        when (event) {
            is FoodEvent.OnAddEntry -> onAddEntry(event.form)
            is FoodEvent.OnDeleteEntry -> onDeleteEntry(event.id)
        }
    }

    private fun observeDiary(foodRepository: FoodRepository, profileRepository: ProfileRepository) = intent {
        combine(foodRepository.observeTodayEntries(), profileRepository.observeProfile()) { entries, profile ->
            FoodUiState(entries = entries, targets = profile?.dailyTargets())
        }.collect { newState -> reduce { newState } }
    }

    private fun onAddEntry(form: AddEntryForm) = intent {
        foodRepository.addEntry(form.toFoodEntry())
    }

    private fun onDeleteEntry(id: Long) = intent {
        foodRepository.deleteEntry(id)
    }
}
