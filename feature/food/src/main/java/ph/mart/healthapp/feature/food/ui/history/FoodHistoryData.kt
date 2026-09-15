package ph.mart.healthapp.feature.food.ui.history

import ph.mart.healthapp.core.data.food.FoodEntry
import ph.mart.healthapp.core.data.food.MealType
import ph.mart.healthapp.core.designsystem.component.formatMonthYear
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
    /** The meal slot the list is narrowed to; `null` is every slot, which is what it opens on. */
    val mealFilter: MealType? = null,
    /** Queries that have found something before, newest first — offered under a blank field. */
    val recentQueries: List<String> = emptyList(),
    /**
     * What each day in [results] came to in full, keyed by epoch day.
     *
     * Read separately from the rows, because a day header reports the *day*: summing the matched
     * rows would have "chicken" decide what Tuesday was worth. Absent while a search is in flight,
     * which is why the header treats a missing key as nothing to draw rather than as zero.
     */
    val dayTotals: Map<Long, Int> = emptyMap(),
) {
    /** How many days the matches are spread across — the second figure in the count line. */
    val dayCount: Int get() = results.distinctBy { it.dateEpochDay }.size
}

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

/** One day of the history list: the date, and what matched on it. */
internal data class HistoryDay(val dateEpochDay: Long, val entries: List<FoodEntry>)

/** A run of days under one heading — "This week", "Earlier this month", "August". */
internal data class HistoryBand(val label: String, val days: List<HistoryDay>)

/**
 * The days folded again, into the bands the list is scanned by.
 *
 * Adjacent-only, like [groupedByDay] and for the same reason: the rows arrive newest-first from
 * Room and nothing here re-sorts them. A band therefore *cannot* reach back and collect a day it
 * has already passed, which is what keeps the list's order and its headings the same statement.
 */
internal fun List<FoodEntry>.bandedGroups(today: Long): List<HistoryBand> =
    groupedByDay().fold(mutableListOf<Pair<String, MutableList<HistoryDay>>>()) { bands, (day, entries) ->
        val label = ageBandFor(day, today)
        val open = bands.lastOrNull()
        val row = HistoryDay(day, entries)
        if (open != null && open.first == label) open.second += row else bands += label to mutableListOf(row)
        bands
    }.map { (label, days) -> HistoryBand(label, days) }

/**
 * Which heading a day sits under.
 *
 * Three tiers, coarsening as the list goes back: the last seven days are one band because that is
 * the window a person still thinks of as "now", the rest of the current month is the next, and
 * everything older is simply its month. A day and today are in the same month exactly when they
 * carry the same month heading, which is what [formatMonthYear] already decides — no second
 * calendar comparison, and no second place for the year rule to be got wrong.
 */
// Stays in Kotlin under the pure-function-with-a-test rule: FoodHistoryTest asserts this wording.
internal fun ageBandFor(epochDay: Long, today: Long): String = when {
    today - epochDay < DAYS_IN_WEEK -> "This week"
    formatMonthYear(epochDay) == formatMonthYear(today) -> "Earlier this month"
    else -> formatMonthYear(epochDay)
}

/**
 * How old a day is, in the unit that day deserves — the chip beside the absolute date.
 *
 * The absolute date is what the list is *scanned* by; this is what it is *understood* by, and the
 * two are drawn together because neither does the other's job. The unit coarsens with distance for
 * the reason the bands do: "63 days ago" is a number to work out, "9 weeks ago" is a fact.
 */
// Stays in Kotlin under the pure-function-with-a-test rule: FoodHistoryTest asserts this wording.
internal fun relativeAgeLabel(epochDay: Long, today: Long): String {
    val days = today - epochDay
    return when {
        days <= 0L -> "Today"
        days == 1L -> "Yesterday"
        days < 2 * DAYS_IN_WEEK -> "$days days ago"
        days < 2 * DAYS_IN_MONTH -> "${days / DAYS_IN_WEEK} weeks ago"
        else -> "${days / DAYS_IN_MONTH} months ago"
    }
}

private const val DAYS_IN_WEEK = 7L

/** Thirty, not 28 or 31: the label is an approximation already, and a month that has to be exact
 * is the absolute date sitting right beside it.
 *
 * Each tier begins at *two* of its unit, which is what keeps the singular out of a string that has
 * no plural form to switch on — a week is still "7 days ago" and a month is still "4 weeks ago". */
private const val DAYS_IN_MONTH = 30L

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

    /** The meal filter. `null` is "All" — the read runs again either way. */
    data class OnMealFilterChange(val mealType: MealType?) : FoodHistoryEvent

    /**
     * A result was opened for review, which is the moment the query is worth remembering: recorded
     * on a keystroke instead, the recent list fills with "c", "ch", "chi".
     */
    data object OnQueryUsed : FoodHistoryEvent

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
