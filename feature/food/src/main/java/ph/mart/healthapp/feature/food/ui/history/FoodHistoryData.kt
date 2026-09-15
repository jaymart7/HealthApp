package ph.mart.healthapp.feature.food.ui.history

import ph.mart.healthapp.core.data.food.FoodEntry
import ph.mart.healthapp.feature.food.ui.shared.AddEntryForm
import ph.mart.healthapp.feature.food.ui.shared.toAddEntryForm

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

/**
 * A past row seeded into the review form the card's tap opens — a copy, never the row itself.
 *
 * Two things are dropped deliberately:
 *
 * - `id`, so what is eventually written is a new row rather than an edit of a past day's.
 *   [AddEntryForm] carries none, so this is simply what the form cannot say.
 * - [FoodEntry.photoPath], because `addEntry` keeps a path it is handed and two rows pointing at one
 *   file would break the meal-photo prune, which counts paths and would reclaim the file out from
 *   under the row that earned it.
 *
 * The meal slot is the source row's, not the clock's: unlike the photo and barcode flows that
 * `defaultMealTypeForNow()` exists for, this one already knows where the food belongs — and unlike
 * those, the chips on the review screen are there to say otherwise.
 */
internal fun FoodEntry.toReviewForm(): AddEntryForm = toAddEntryForm().copy(photoPath = null)

sealed interface FoodHistoryEvent {
    data class OnQueryChange(val query: String) : FoodHistoryEvent

    /**
     * Writes the reviewed row. The screen hands over the finished [FoodEntry] — `form.toFoodEntry(
     * dateEpochDay)`, stamped with the day the diary was showing when this screen was opened, which
     * is the rule its microphone, barcode and camera doors already follow — so there is nothing left
     * here to decide. The shape `BarcodeScanEvent.OnLogEntry` uses, for the same reason.
     */
    data class OnLog(val entry: FoodEntry) : FoodHistoryEvent
}

/** No side effects: the confirmation is raised by the screen, the shape the diary's own rows use
 * for a delete — nothing here has to come back from the write. */
sealed interface FoodHistorySideEffect
