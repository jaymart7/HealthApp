package ph.mart.healthapp.feature.food.ui.ideas

import androidx.lifecycle.ViewModel
import org.orbitmvi.orbit.OrbitContainerHost
import org.orbitmvi.orbit.viewmodel.orbitContainer
import ph.mart.healthapp.core.data.food.MealIdeaRepository
import ph.mart.healthapp.core.data.food.MealIdeaRequest
import ph.mart.healthapp.core.data.food.MealIdeaResult
import ph.mart.healthapp.core.data.network.NetworkMonitor

/**
 * Two dependencies, and deliberately not one more.
 *
 * The day's numbers, the recents and the recipes all reach the screen from [FoodUiState][ph.mart.healthapp.feature.food.ui.diary.FoodUiState],
 * which the diary underneath has already combined — injecting `FoodRepository`, `ProfileRepository`,
 * `ExerciseRepository` and `StepsRepository` here to rebuild them would be a second copy of the
 * diary's whole observer to render a screen that writes nothing. That is the call the Progress
 * recap made when it chose an overlay over a route.
 *
 * So this holds the model call and nothing else. No side effects: picking an idea seeds the
 * add-entry sheet, which is the diary's own state.
 */
class MealIdeasViewModel(
    private val mealIdeaRepository: MealIdeaRepository,
    private val networkMonitor: NetworkMonitor,
) : ViewModel(), OrbitContainerHost<MealIdeasUiState, MealIdeasUiState, MealIdeasSideEffect> {

    override val container = orbitContainer<MealIdeasUiState, MealIdeasSideEffect>(MealIdeasUiState.Idle)

    fun handleEvent(event: MealIdeasEvent) {
        when (event) {
            is MealIdeasEvent.OnRequest -> request(event.request)
        }
    }

    /** The offline check is made *before* the call rather than inferred from its failure: the
     * screen says something different for "you're offline" than for "that didn't work", and a
     * `FirebaseAIException` cannot tell the two apart. */
    private fun request(request: MealIdeaRequest) = intent {
        if (!networkMonitor.isOnline()) {
            reduce { MealIdeasUiState.Failed(offline = true) }
            return@intent
        }
        reduce { MealIdeasUiState.Loading }
        val newState = when (val result = mealIdeaRepository.ideas(request)) {
            is MealIdeaResult.Success -> MealIdeasUiState.Ideas(result.ideas)
            MealIdeaResult.Failed -> MealIdeasUiState.Failed(offline = false)
        }
        reduce { newState }
    }
}

/** None — the screen hands the picked idea straight to the diary, the shape
 * [FoodSearchSideEffect][ph.mart.healthapp.feature.food.ui.search.FoodSearchSideEffect] has. */
sealed interface MealIdeasSideEffect
