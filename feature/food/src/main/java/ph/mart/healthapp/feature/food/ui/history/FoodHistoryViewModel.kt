package ph.mart.healthapp.feature.food.ui.history

import androidx.lifecycle.ViewModel
import org.orbitmvi.orbit.OrbitContainerHost
import org.orbitmvi.orbit.viewmodel.orbitContainer
import ph.mart.healthapp.core.data.food.FoodRepository

/**
 * Backs [FoodHistoryScreen] — the one screen in this app that reads the diary across days.
 *
 * It holds no subscription at all, which is what makes it unlike every other ViewModel here: the
 * results are a one-shot answer to a typed question, not a table the screen mirrors. A row logged
 * elsewhere while this is open therefore doesn't appear until the next keystroke, and that is the
 * right behaviour for a search — a list that reorders itself under a reading finger is worse than
 * a slightly stale one.
 *
 * The query is seeded by the screen rather than injected, because it arrives on the route: see
 * [FoodHistoryScreen].
 */
class FoodHistoryViewModel(
    private val foodRepository: FoodRepository,
) : ViewModel(), OrbitContainerHost<FoodHistoryUiState, FoodHistoryUiState, FoodHistorySideEffect> {

    override val container = orbitContainer<FoodHistoryUiState, FoodHistorySideEffect>(FoodHistoryUiState())

    fun handleEvent(event: FoodHistoryEvent) {
        when (event) {
            is FoodHistoryEvent.OnQueryChange -> search(event.query)
            is FoodHistoryEvent.OnLogAgain -> logAgain(event)
        }
    }

    /**
     * The query is reduced before the read and the results after it, so the field never lags the
     * keystroke that filled it.
     *
     * A late answer to a query the user has already typed past is dropped: Orbit serialises
     * intents, so [state] here is whatever the newest keystroke reduced, and comparing against it
     * is what stops a slower earlier read overwriting a faster later one.
     */
    private fun search(query: String) = intent {
        reduce { state.copy(query = query, searching = true) }
        val results = foodRepository.searchEntries(query.trim())
        reduce {
            if (state.query != query) state else {
                state.copy(results = results, searching = false, searched = true)
            }
        }
    }

    /**
     * A copy, never the row itself. Two things are dropped deliberately:
     *
     * - `id`, so this is a new row rather than an edit of a past day's.
     * - [ph.mart.healthapp.core.data.food.FoodEntry.photoPath], because `addEntry` keeps a path it
     *   is handed and two rows pointing at one file would break the meal-photo prune, which counts
     *   paths and would reclaim the file out from under the row that earned it.
     *
     * The meal slot is the source row's, not the clock's: unlike the photo and barcode flows that
     * `defaultMealTypeForNow()` exists for, this one already knows where the food belongs.
     */
    private fun logAgain(event: FoodHistoryEvent.OnLogAgain) = intent {
        foodRepository.addEntry(
            event.entry.copy(id = 0, dateEpochDay = event.dateEpochDay, photoPath = null),
        )
    }
}
