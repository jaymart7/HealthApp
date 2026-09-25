package ph.mart.healthapp.feature.food.ui.recipe

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.Job
import org.orbitmvi.orbit.OrbitContainerHost
import org.orbitmvi.orbit.viewmodel.orbitContainer
import ph.mart.healthapp.core.data.food.FoodRepository
import ph.mart.healthapp.core.data.food.RecipeParseRepository
import ph.mart.healthapp.core.data.food.RecipeParseResult
import ph.mart.healthapp.core.data.food.SavedMealItem
import ph.mart.healthapp.core.data.network.NetworkMonitor
import ph.mart.healthapp.feature.food.R

/** The builder still reads nothing — a recipe is authored, not loaded — so the one thing the
 * container holds is whether a fill is in flight, which the send button and back both read. */
data class RecipeBuilderUiState(val filling: Boolean = false)

sealed interface RecipeBuilderSideEffect {
    data object Saved : RecipeBuilderSideEffect
    data class Filled(val name: String, val servings: Int, val items: List<SavedMealItem>) : RecipeBuilderSideEffect
    data class FillFailed(@StringRes val message: Int) : RecipeBuilderSideEffect
}

sealed interface RecipeBuilderEvent {
    data class OnSave(val name: String, val servings: Int, val items: List<SavedMealItem>) : RecipeBuilderEvent
    data class OnFill(val text: String) : RecipeBuilderEvent
    data object OnCancelFill : RecipeBuilderEvent
}

class RecipeBuilderViewModel(
    private val foodRepository: FoodRepository,
    private val recipeParseRepository: RecipeParseRepository,
    private val networkMonitor: NetworkMonitor,
) : ViewModel(), OrbitContainerHost<RecipeBuilderUiState, RecipeBuilderUiState, RecipeBuilderSideEffect> {

    override val container = orbitContainer<RecipeBuilderUiState, RecipeBuilderSideEffect>(RecipeBuilderUiState())

    /** Cancelled by back and by the stop button — `VoiceLogViewModel.parseJob`'s reason. */
    private var fillJob: Job? = null

    fun handleEvent(event: RecipeBuilderEvent) {
        when (event) {
            is RecipeBuilderEvent.OnSave -> onSave(event)
            is RecipeBuilderEvent.OnFill -> onFill(event.text)
            RecipeBuilderEvent.OnCancelFill -> onCancelFill()
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

    /** `SupplementsViewModel.onLookUp`'s shape: the online check is asked at the moment a request
     * is about to be spent, and each way it can fail is a sentence under the field. */
    private fun onFill(text: String) {
        if (text.isBlank() || fillJob?.isActive == true) return
        fillJob = intent {
            if (!networkMonitor.isOnline()) {
                postSideEffect(RecipeBuilderSideEffect.FillFailed(R.string.food_recipe_fill_offline))
                return@intent
            }
            reduce { state.copy(filling = true) }
            val result = recipeParseRepository.parse(text.trim())
            reduce { state.copy(filling = false) }
            postSideEffect(
                when (result) {
                    is RecipeParseResult.Parsed ->
                        RecipeBuilderSideEffect.Filled(result.name, result.servings, result.items)

                    RecipeParseResult.NothingFound ->
                        RecipeBuilderSideEffect.FillFailed(R.string.food_recipe_fill_nothing)

                    RecipeParseResult.Failed ->
                        RecipeBuilderSideEffect.FillFailed(R.string.food_recipe_fill_failed)
                },
            )
        }
    }

    /** A cancelled intent never reaches its own `filling = false`, so the reset is its own. */
    private fun onCancelFill() {
        fillJob?.cancel()
        intent { reduce { state.copy(filling = false) } }
    }
}
