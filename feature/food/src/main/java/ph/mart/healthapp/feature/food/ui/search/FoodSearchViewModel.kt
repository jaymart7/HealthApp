package ph.mart.healthapp.feature.food.ui.search

import androidx.lifecycle.ViewModel
import org.orbitmvi.orbit.OrbitContainerHost
import org.orbitmvi.orbit.viewmodel.orbitContainer
import ph.mart.healthapp.core.data.food.FoodRepository
import ph.mart.healthapp.core.data.food.searchFoods

/** No side effects: the panel hands the picked product straight to its host. */
sealed interface FoodSearchSideEffect

/**
 * Backs [ph.mart.healthapp.feature.food.ui.search.components.FoodSearchPanel] wherever it appears — the
 * diary's add-entry sheet, the photo flow's manual-search state and the recipe ingredient editor
 * each get their own instance from their own nav entry, so one screen's query never leaks into the
 * other.
 *
 * Its one dependency is the user's own foods, which lead every result: the built-in half
 * ([COMMON_FOODS][ph.mart.healthapp.core.data.food.COMMON_FOODS]) is a list shipped in the APK, so
 * there is still nothing to debounce and no network to check. What it earns its keep for beyond
 * that is the page — that survives a rotation here and would not in the composable.
 */
class FoodSearchViewModel(
    private val foodRepository: FoodRepository,
) : ViewModel(), OrbitContainerHost<FoodSearchUiState, FoodSearchUiState, FoodSearchSideEffect> {

    override val container = orbitContainer<FoodSearchUiState, FoodSearchSideEffect>(FoodSearchUiState()) {
        observeMyFoods()
    }

    fun handleEvent(event: FoodSearchEvent) {
        when (event) {
            is FoodSearchEvent.OnQueryChange -> onQueryChange(event.query)
            FoodSearchEvent.OnNextPage -> movePage(1)
            FoodSearchEvent.OnPrevPage -> movePage(-1)
        }
    }

    /** The page is deliberately left where it is: a food saved in another tab must not move the
     * page out from under someone reading it, and the list only ever grows at the front. */
    private fun observeMyFoods() = intent {
        foodRepository.observeMyFoods().collect { myFoods ->
            reduce { state.copy(myFoods = myFoods, results = searchFoods(state.query, myFoods)) }
        }
    }

    /** A narrowed query re-pages from the top: page 4 of the old results names nothing in the new. */
    private fun onQueryChange(query: String) = intent {
        reduce { state.copy(query = query, results = searchFoods(query, state.myFoods), page = 0) }
    }

    private fun movePage(delta: Int) = intent {
        reduce { state.copy(page = (state.page + delta).coerceIn(0, (state.pageCount - 1).coerceAtLeast(0))) }
    }
}
