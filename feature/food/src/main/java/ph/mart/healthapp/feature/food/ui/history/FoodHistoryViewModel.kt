package ph.mart.healthapp.feature.food.ui.history

import androidx.lifecycle.ViewModel
import org.orbitmvi.orbit.OrbitContainerHost
import org.orbitmvi.orbit.viewmodel.orbitContainer
import ph.mart.healthapp.core.data.food.FoodRepository
import ph.mart.healthapp.core.data.food.HISTORY_PAGE_SIZE
import ph.mart.healthapp.core.data.food.MealType

/** Three chips is what fits on one line beside the "Recent" label without an overflow row. */
private const val RECENT_QUERIES = 3

/**
 * Backs [FoodHistoryScreen] — the one screen in this app that reads the diary across days.
 *
 * It holds no subscription at all, which is what makes it unlike every other ViewModel here: the
 * results are a one-shot answer to a typed question, not a table the screen mirrors. A row logged
 * elsewhere while this is open therefore doesn't appear until the next keystroke, and that is the
 * right behaviour for a search — a list that reorders itself under a reading finger is worse than
 * a slightly stale one.
 *
 * The answer arrives a page at a time: [search] reads the first, [loadMore] appends the rest as
 * the list is scrolled. The same one-shot rule holds across them — a page is the table as it was
 * when it was asked for, and the rows above it are not re-read.
 *
 * The query is seeded by the screen rather than injected, because it arrives on the route: see
 * [FoodHistoryScreen].
 */
class FoodHistoryViewModel(
    private val foodRepository: FoodRepository,
) : ViewModel(), OrbitContainerHost<FoodHistoryUiState, FoodHistoryUiState, FoodHistorySideEffect> {

    override val container = orbitContainer<FoodHistoryUiState, FoodHistorySideEffect>(FoodHistoryUiState()) {
        observeRecentQueries()
    }

    fun handleEvent(event: FoodHistoryEvent) {
        when (event) {
            is FoodHistoryEvent.OnQueryChange -> search(event.query)
            is FoodHistoryEvent.OnMealFilterChange -> filter(event.mealType)
            FoodHistoryEvent.OnLoadMore -> loadMore()
            FoodHistoryEvent.OnQueryUsed -> recordQuery()
            is FoodHistoryEvent.OnLog -> log(event)
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
        val mealFilter = state.mealFilter
        val results = foodRepository.searchEntries(query.trim(), mealFilter)
        // The day headers' figures, read after the rows because the rows are what name the days.
        val totals = foodRepository.dayTotals(results.map { it.dateEpochDay }.distinct())
        // What the count line says. A third read rather than a count over `results`, which is one
        // page and not an answer — the reason `dayTotals` is its own read, one level up.
        val counts = foodRepository.searchCount(query.trim(), mealFilter)
        reduce {
            if (state.query != query || state.mealFilter != mealFilter) state else {
                state.copy(
                    results = results,
                    dayTotals = totals,
                    matchCount = counts.matches,
                    dayCount = counts.days,
                    searching = false,
                    searched = true,
                    // A fresh question, so the list starts at the top of its answer again.
                    appending = false,
                    endReached = results.size < HISTORY_PAGE_SIZE,
                )
            }
        }
    }

    /**
     * The next page, appended.
     *
     * The guard is the whole of the paging: Orbit serialises intents, so two pages can't be in
     * flight at once, and [FoodHistoryUiState.endReached] is what stops the list asking Room for
     * page after empty page once it has run out. [FoodHistoryUiState.searched] is in there because
     * a list that hasn't had its first answer yet has no offset to ask from.
     *
     * Only the days this page *introduced* are re-totalled — the ones already on screen have their
     * figures, and re-reading them would be a growing query on every flick.
     */
    private fun loadMore() = intent {
        if (state.appending || state.endReached || state.searching || !state.searched) return@intent
        reduce { state.copy(appending = true) }
        val query = state.query
        val mealFilter = state.mealFilter
        val page = foodRepository.searchEntries(query.trim(), mealFilter, offset = state.results.size)
        val newDates = page.map { it.dateEpochDay }.distinct().filterNot { it in state.dayTotals }
        val totals = if (newDates.isEmpty()) emptyMap() else foodRepository.dayTotals(newDates)
        reduce {
            // Dropped if the question changed underneath it — [search]'s rule. The rows go, the
            // flag still has to be cleared, or an abandoned page leaves the list unable to ask for
            // another one.
            if (state.query != query || state.mealFilter != mealFilter) {
                state.copy(appending = false)
            } else {
                state.withPage(page, totals)
            }
        }
    }

    /**
     * The filter re-runs the query it narrows. Reduced before the read, like the query itself, so
     * the chip moves on the tap rather than when the answer lands — and [search] compares against
     * the reduced filter as well as the reduced query, so switching twice quickly cannot leave the
     * slower first answer on screen.
     */
    private fun filter(mealType: MealType?) = intent {
        reduce { state.copy(mealFilter = mealType) }
        search(state.query)
    }

    /** See [FoodHistoryEvent.OnQueryUsed] for why this is a row tap and not a keystroke. */
    private fun recordQuery() = intent {
        foodRepository.recordQuery(state.query)
    }

    private fun observeRecentQueries() = intent {
        foodRepository.observeRecentQueries(RECENT_QUERIES).collect { queries ->
            reduce { state.copy(recentQueries = queries) }
        }
    }

    /**
     * The write, and nothing else: the row arrives finished from the review screen, and what makes
     * it a copy rather than an edit of a past day's row is stated at [toReviewForm], where the form
     * is seeded.
     *
     * No side effect and nothing reduced — the screen raises its own confirmation, the shape the
     * diary's own rows use for a delete.
     */
    private fun log(event: FoodHistoryEvent.OnLog) = intent {
        foodRepository.addEntry(event.entry)
    }
}
