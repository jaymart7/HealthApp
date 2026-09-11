package ph.mart.healthapp.feature.food.ui.history

import ph.mart.healthapp.core.data.food.FoodEntry

/**
 * The diary's history search: one query, and whatever it matched.
 *
 * [results] arrive from Room already ordered newest-first and capped, so there is nothing to sort
 * and nothing to page here. No status type beyond [searching]: the query is a local one that
 * cannot be offline and cannot fail, and the only other answer the screen has to draw is "nothing
 * matched" — the shape `FoodSearchUiState` settled on for the same reasons.
 *
 * [searched] is what separates "nothing matched" from "the first query hasn't come back yet", so
 * the empty page never flashes over a list that is about to arrive.
 */
data class FoodHistoryUiState(
    val query: String = "",
    val results: List<FoodEntry> = emptyList(),
    val searching: Boolean = false,
    val searched: Boolean = false,
)

/**
 * The results cut into one group per day, newest day first — which is the order they arrive in, so
 * this only ever folds adjacent rows together and never re-sorts.
 *
 * A list of pairs rather than a `Map`: the order *is* the meaning here, and a map hands that to
 * whatever its iteration order happens to be. Pure, so `FoodHistoryTest` can hold it to both.
 */
internal fun List<FoodEntry>.groupedByDay(): List<Pair<Long, List<FoodEntry>>> =
    fold(mutableListOf<Pair<Long, MutableList<FoodEntry>>>()) { groups, entry ->
        val open = groups.lastOrNull()
        if (open != null && open.first == entry.dateEpochDay) {
            open.second += entry
        } else {
            groups += entry.dateEpochDay to mutableListOf(entry)
        }
        groups
    }

sealed interface FoodHistoryEvent {
    data class OnQueryChange(val query: String) : FoodHistoryEvent

    /**
     * Logs a copy of a past row onto [dateEpochDay] — the day the diary was showing when this
     * screen was opened, which is the rule its microphone, barcode and camera doors already
     * follow.
     */
    data class OnLogAgain(val entry: FoodEntry, val dateEpochDay: Long) : FoodHistoryEvent
}

/** No side effects: the confirmation is raised by the screen, the shape the diary's own rows use
 * for a delete — nothing here has to come back from the write. */
sealed interface FoodHistorySideEffect
