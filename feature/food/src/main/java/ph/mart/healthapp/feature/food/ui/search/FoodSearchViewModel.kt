package ph.mart.healthapp.feature.food.ui.search

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.mapLatest
import org.orbitmvi.orbit.OrbitContainerHost
import org.orbitmvi.orbit.viewmodel.orbitContainer
import ph.mart.healthapp.core.data.food.FoodSearchRepository
import ph.mart.healthapp.core.data.network.NetworkMonitor

/** No side effects: the panel hands the picked product straight to its host. */
sealed interface FoodSearchSideEffect

/** Below this a query matches half the database, and every keystroke would spend a request. */
private const val MIN_QUERY_LENGTH = 2

/** FoodData Central's hourly budget is shared by every install; this keeps ordinary typing to
 * one request. */
private const val DEBOUNCE_MS = 400L

/**
 * Backs [ph.mart.healthapp.feature.food.ui.search.components.FoodSearchPanel] wherever it appears — the
 * diary's add-entry sheet and the photo flow's manual-search state each get their own instance
 * from their own nav entry, so one screen's query never leaks into the other.
 */
class FoodSearchViewModel(
    private val foodSearchRepository: FoodSearchRepository,
    private val networkMonitor: NetworkMonitor,
) : ViewModel(), OrbitContainerHost<FoodSearchUiState, FoodSearchUiState, FoodSearchSideEffect> {

    private val queries = MutableStateFlow("")

    override val container = orbitContainer<FoodSearchUiState, FoodSearchSideEffect>(FoodSearchUiState()) {
        observeQueries()
    }

    fun handleEvent(event: FoodSearchEvent) {
        when (event) {
            is FoodSearchEvent.OnQueryChange -> onQueryChange(event.query)
        }
    }

    private fun onQueryChange(query: String) = intent {
        queries.value = query
        val searchable = query.trim().length >= MIN_QUERY_LENGTH
        reduce {
            state.copy(
                query = query,
                // Said up front rather than after the debounce, so the panel doesn't look idle
                // while a request is pending.
                status = if (searchable) SearchStatus.Searching else SearchStatus.Idle,
            )
        }
    }

    /**
     * `mapLatest` drops the superseded lookup, so a fast typist's earlier query can never land on
     * top of a later one. Short queries map to [SearchStatus.Idle] here rather than being filtered
     * out, for the same reason: backspacing has to cancel the in-flight request, not let it win.
     */
    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    private fun observeQueries() = intent {
        queries
            .debounce(DEBOUNCE_MS)
            .distinctUntilChanged()
            .mapLatest { query ->
                val term = query.trim()
                when {
                    term.length < MIN_QUERY_LENGTH -> SearchStatus.Idle
                    !networkMonitor.isOnline() -> SearchStatus.Failed
                    else -> foodSearchRepository.search(term).toStatus()
                }
            }
            .collect { status -> reduce { state.copy(status = status) } }
    }
}
