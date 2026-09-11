package ph.mart.healthapp.feature.food.ui.search

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import org.orbitmvi.orbit.OrbitContainerHost
import org.orbitmvi.orbit.viewmodel.orbitContainer
import ph.mart.healthapp.core.data.food.FoodRepository
import ph.mart.healthapp.core.data.food.ProductSearchRepository
import ph.mart.healthapp.core.data.food.ProductSearchResult
import ph.mart.healthapp.core.data.food.ScannedProduct
import ph.mart.healthapp.core.data.food.searchFoods
import ph.mart.healthapp.core.data.network.NetworkMonitor

/** No side effects: the panel hands the picked product straight to its host. */
sealed interface FoodSearchSideEffect

/**
 * Long enough that walking a word in doesn't ask four times, short enough that it lands before you
 * have finished reading what the local list found. Measured, the query itself takes ~0.7s.
 */
private const val ONLINE_DEBOUNCE_MS = 500L

/**
 * Two characters match half the database, and the answer would be a ranking rather than a search.
 * A blank field never asks at all — "list everything" is a local concept.
 */
private const val MIN_ONLINE_QUERY = 3

/**
 * Backs [ph.mart.healthapp.feature.food.ui.search.components.FoodSearchPanel] wherever it appears — the
 * diary's add-entry sheet, the photo flow's manual-search state and the recipe ingredient editor
 * each get their own instance from their own nav entry, so one screen's query never leaks into the
 * other.
 *
 * Two local tiers and one online one. The local halves — the user's own foods and the
 * [COMMON_FOODS][ph.mart.healthapp.core.data.food.COMMON_FOODS] list shipped in the APK — still
 * answer on the keystroke, with nothing to debounce and no network to check; that is what keeps the
 * panel instant and keeps it working offline. Open Food Facts is folded in behind them when it
 * lands, and never blocks, replaces or delays either.
 */
class FoodSearchViewModel(
    private val foodRepository: FoodRepository,
    private val productSearchRepository: ProductSearchRepository,
    private val networkMonitor: NetworkMonitor,
) : ViewModel(), OrbitContainerHost<FoodSearchUiState, FoodSearchUiState, FoodSearchSideEffect> {

    override val container = orbitContainer<FoodSearchUiState, FoodSearchSideEffect>(FoodSearchUiState()) {
        observeMyFoods()
    }

    /** Cancelled on the next keystroke, so walking a word in runs one request rather than one per
     * letter — and a request already in flight for a query nobody is typing any more is dropped. */
    private var onlineJob: Job? = null

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
            reduce { state.copy(myFoods = myFoods, results = searchFoods(state.query, myFoods, state.online)) }
        }
    }

    /**
     * A narrowed query re-pages from the top: page 4 of the old results names nothing in the new.
     * The online tier is cleared with it — hits for "sky" are not hits for "sky flakes", and leaving
     * them up until the next answer lands would show the wrong ones as if they were the right ones.
     */
    private fun onQueryChange(query: String) {
        onlineJob?.cancel()
        intent {
            reduce {
                state.copy(
                    query = query,
                    online = emptyList(),
                    onlineStatus = OnlineSearch.Idle,
                    results = searchFoods(query, state.myFoods),
                    page = 0,
                )
            }
        }
        searchOnline(query.trim())
    }

    private fun searchOnline(term: String) {
        if (term.length < MIN_ONLINE_QUERY || !networkMonitor.isOnline()) return
        onlineJob = intent {
            delay(ONLINE_DEBOUNCE_MS)
            reduce { state.copy(onlineStatus = OnlineSearch.Searching) }
            when (val result = productSearchRepository.search(term)) {
                is ProductSearchResult.Ok -> reduce { state.withOnline(term, result.products) }
                ProductSearchResult.Failed -> reduce {
                    if (state.query.trim() != term) state else state.copy(onlineStatus = OnlineSearch.Failed)
                }
            }
        }
    }

    /** A late answer to a superseded query is dropped by comparing the query back, the same guard
     * `FoodHistoryViewModel`'s suspend one-shot uses. Cancellation covers most of it; this covers
     * the answer already on its way back when the keystroke landed. */
    private fun FoodSearchUiState.withOnline(term: String, products: List<ScannedProduct>) =
        if (query.trim() != term) {
            this
        } else {
            copy(
                online = products,
                onlineStatus = OnlineSearch.Idle,
                results = searchFoods(query, myFoods, products),
            )
        }

    private fun movePage(delta: Int) = intent {
        reduce { state.copy(page = (state.page + delta).coerceIn(0, (state.pageCount - 1).coerceAtLeast(0))) }
    }
}
