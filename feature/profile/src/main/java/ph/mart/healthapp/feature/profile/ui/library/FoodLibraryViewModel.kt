package ph.mart.healthapp.feature.profile.ui.library

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.combine
import org.orbitmvi.orbit.OrbitContainerHost
import org.orbitmvi.orbit.viewmodel.orbitContainer
import ph.mart.healthapp.core.data.food.FoodRepository

/**
 * Reads both unlimited lists and writes the two things this screen can do to them. No side
 * effects: a rename and a delete are both writes the flows report back on their own.
 */
class FoodLibraryViewModel(
    private val foodRepository: FoodRepository,
) : ViewModel(), OrbitContainerHost<FoodLibraryUiState, FoodLibraryUiState, Nothing> {

    override val container = orbitContainer<FoodLibraryUiState, Nothing>(FoodLibraryUiState()) {
        observeLibrary()
    }

    fun handleEvent(event: FoodLibraryEvent) {
        when (event) {
            is FoodLibraryEvent.OnDeleteSavedMeal -> onDeleteSavedMeal(event.id)
            is FoodLibraryEvent.OnDeleteRecipe -> onDeleteRecipe(event.id)
            is FoodLibraryEvent.OnRenameSavedMeal -> onRenameSavedMeal(event.id, event.name)
            is FoodLibraryEvent.OnRenameRecipe -> onRenameRecipe(event.id, event.name)
        }
    }

    private fun observeLibrary() = intent {
        combine(
            foodRepository.observeAllSavedMeals(),
            foodRepository.observeAllRecipes(),
        ) { savedMeals, recipes -> savedMeals to recipes }
            .collect { (savedMeals, recipes) ->
                reduce { state.copy(savedMeals = savedMeals, recipes = recipes) }
            }
    }

    private fun onDeleteSavedMeal(id: Long) = intent {
        foodRepository.deleteSavedMeal(id)
    }

    private fun onDeleteRecipe(id: Long) = intent {
        foodRepository.deleteRecipe(id)
    }

    /** Blank is rejected at the sheet's Save button; trimming here is what stops a stray space
     * becoming a name nothing else in the app would have accepted. */
    private fun onRenameSavedMeal(id: Long, name: String) = intent {
        foodRepository.renameSavedMeal(id, name.trim())
    }

    private fun onRenameRecipe(id: Long, name: String) = intent {
        foodRepository.renameRecipe(id, name.trim())
    }
}
