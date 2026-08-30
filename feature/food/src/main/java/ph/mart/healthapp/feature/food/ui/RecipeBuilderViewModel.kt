package ph.mart.healthapp.feature.food.ui

import androidx.lifecycle.ViewModel
import org.orbitmvi.orbit.OrbitContainerHost
import org.orbitmvi.orbit.viewmodel.orbitContainer
import ph.mart.healthapp.core.data.food.FoodRepository
import ph.mart.healthapp.core.data.food.SavedMealItem

/** The builder reads nothing — a recipe is authored, not loaded — so the container's state is
 * empty and the only thing crossing it is "the write landed, leave the screen". */
data object RecipeBuilderUiState

sealed interface RecipeBuilderSideEffect {
    data object Saved : RecipeBuilderSideEffect
}

sealed interface RecipeBuilderEvent {
    data class OnSave(val name: String, val servings: Int, val items: List<SavedMealItem>) : RecipeBuilderEvent
}

class RecipeBuilderViewModel(
    private val foodRepository: FoodRepository,
) : ViewModel(), OrbitContainerHost<RecipeBuilderUiState, RecipeBuilderUiState, RecipeBuilderSideEffect> {

    override val container = orbitContainer<RecipeBuilderUiState, RecipeBuilderSideEffect>(RecipeBuilderUiState)

    fun handleEvent(event: RecipeBuilderEvent) {
        when (event) {
            is RecipeBuilderEvent.OnSave -> onSave(event)
        }
    }

    private fun onSave(event: RecipeBuilderEvent.OnSave) = intent {
        foodRepository.saveRecipe(
            name = event.name.trim(),
            servings = event.servings.coerceAtLeast(1),
            items = event.items,
        )
        postSideEffect(RecipeBuilderSideEffect.Saved)
    }
}
