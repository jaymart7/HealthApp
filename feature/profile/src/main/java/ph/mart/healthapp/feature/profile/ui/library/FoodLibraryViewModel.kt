package ph.mart.healthapp.feature.profile.ui.library

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.combine
import org.orbitmvi.orbit.OrbitContainerHost
import org.orbitmvi.orbit.viewmodel.orbitContainer
import ph.mart.healthapp.core.data.food.FoodRepository

/**
 * Reads all three unlimited lists, and that is all: a row opens `:feature:food`'s add-and-edit
 * screen, which is where a rename, an edit or a delete happens now.
 */
class FoodLibraryViewModel(
    private val foodRepository: FoodRepository,
) : ViewModel(), OrbitContainerHost<FoodLibraryUiState, FoodLibraryUiState, Nothing> {

    override val container = orbitContainer<FoodLibraryUiState, Nothing>(FoodLibraryUiState()) {
        observeLibrary()
    }

    private fun observeLibrary() = intent {
        combine(
            foodRepository.observeMyFoods(),
            foodRepository.observeAllSavedMeals(),
            foodRepository.observeAllRecipes(),
        ) { myFoods, savedMeals, recipes -> Triple(myFoods, savedMeals, recipes) }
            .collect { (myFoods, savedMeals, recipes) ->
                reduce { state.copy(myFoods = myFoods, savedMeals = savedMeals, recipes = recipes) }
            }
    }
}
