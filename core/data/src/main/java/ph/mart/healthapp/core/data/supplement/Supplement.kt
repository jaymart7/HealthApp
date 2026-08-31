package ph.mart.healthapp.core.data.supplement

import kotlinx.coroutines.flow.Flow
import ph.mart.healthapp.core.data.progress.ChartRange

/**
 * One thing the user takes. [dose] is a label the app never does math on — "2000 IU", "5 g",
 * "one scoop" — for the same reason fiber, sugar and sodium are reported and never graded: there
 * is nothing on the profile to derive a supplement target from. [timesPerDay] *is* a number,
 * because "2x daily" turns the day's tick into a count out of N.
 *
 * [deleted] is a soft delete: past [SupplementDay] rows keep a name to render, so removing a
 * supplement today can't rewrite the chart's history.
 */
data class Supplement(
    val id: Long = 0,
    val name: String,
    val dose: String = "",
    val timesPerDay: Int = 1,
    val deleted: Boolean = false,
    /** Ordering only — the list reads oldest-first, so a new row lands at the bottom. */
    val createdAt: Long = 0,
)

/**
 * One supplement on one day.
 *
 * **[dueTimes] is a snapshot, never re-read from [Supplement.timesPerDay].** Dropping "2x daily"
 * to once next month must not turn a past day that read "2 of 2" into "2 of 1" — the same rule
 * `fast_session.goalHours` and `step_day.burnedKcal` follow.
 *
 * A [taken] of 0 is a real row, not an absence: un-ticking is an update, which is what keeps this
 * domain inside the project's soft-delete-only rule without a deleted flag on the day table. A
 * fully-zeroed day is simply not exported.
 */
data class SupplementDay(
    val dateEpochDay: Long,
    val supplementId: Long,
    val taken: Int,
    val dueTimes: Int,
)

/** A supplement paired with today's count — what the Home card renders. */
data class SupplementToday(val supplement: Supplement, val taken: Int) {
    val isComplete: Boolean get() = taken >= supplement.timesPerDay
}

/** Once a day is the common case; past six a checklist stops being one. */
val SUPPLEMENT_TIMES_PER_DAY = 1..6

/** Doses ride a text field, so this is the only bound on one. */
const val SUPPLEMENT_NAME_MAX = 40
const val SUPPLEMENT_DOSE_MAX = 24

/** The card's header, e.g. "2 of 3" — supplements *completed*, not doses taken. */
val List<SupplementToday>.completedCount: Int get() = count { it.isComplete }

/**
 * The next count a tap should write: one more, wrapping back to zero at the top. One gesture
 * covers both shapes — a once-daily row behaves as a checkbox, a twice-daily one steps 0-1-2-0.
 */
fun nextTaken(taken: Int, timesPerDay: Int): Int = if (taken >= timesPerDay) 0 else taken + 1

/**
 * One fraction per day that has rows, keyed by day and oldest first. Days with no rows are absent
 * rather than zero — the series is sparse, exactly like mood's, and a day before the user's first
 * supplement is a gap, not a miss.
 *
 * The denominator is that day's own summed [SupplementDay.dueTimes], which is what makes a day
 * whose targets have since changed still report the share it actually hit.
 */
fun List<SupplementDay>.adherenceByDay(): List<Pair<Long, Float>> = groupBy { it.dateEpochDay }
    .toSortedMap()
    .mapNotNull { (date, rows) ->
        val due = rows.sumOf { it.dueTimes }
        if (due <= 0) return@mapNotNull null
        date to (rows.sumOf { it.taken }.toFloat() / due).coerceAtMost(1f)
    }

/** Mean of [adherenceByDay]'s fractions — a mean of the *days*, so a day with six supplements
 * isn't six days. Null when nothing has been logged in the window. */
fun List<SupplementDay>.averageAdherence(): Float? =
    adherenceByDay().takeIf { it.isNotEmpty() }?.let { days -> days.sumOf { it.second.toDouble() }.toFloat() / days.size }

/**
 * Anchored to today, like [ph.mart.healthapp.core.data.mood.inRange] and unlike the weight
 * series: a chart headed "1M" must show the last 30 days with their gaps intact, not the 30 days
 * around whenever the user last ticked something.
 */
fun List<SupplementDay>.inRange(range: ChartRange, todayEpochDay: Long): List<SupplementDay> {
    val days = range.days ?: return this
    return filter { it.dateEpochDay >= todayEpochDay - days }
}

/**
 * The user's own list, plus one row per supplement per day it was due.
 *
 * Deliberately **not** part of the logging streak, for the same reason mood, sleep and fasting
 * aren't: the streak's four domains (food, water, weigh-in, exercise) are things the user *did*
 * that day, and adding a fifth now would change what a past run meant. That is why there is no
 * `observeLoggedDays()` here.
 */
interface SupplementRepository {
    /** Active supplements, oldest first. Soft-deleted rows never appear. */
    fun observeSupplements(): Flow<List<Supplement>>

    /** The list paired with today's counts — one flow, because both Home and the widget-shaped
     * callers combine at the arity the typed `combine` overloads stop at. */
    fun observeToday(): Flow<List<SupplementToday>>

    /** Every day with a row, oldest first — the Progress tab. Sparse, unlike daily nutrition. */
    fun observeDays(): Flow<List<SupplementDay>>

    suspend fun addSupplement(supplement: Supplement)

    /** Name, dose and times-per-day in one write — the edit sheet saves all three at once. */
    suspend fun updateSupplement(supplement: Supplement)

    /** Soft delete. Past days keep pointing at the row so the chart can still name it. */
    suspend fun deleteSupplement(id: Long)

    /** [taken] is clamped to the supplement's own [Supplement.timesPerDay]. Writes a zero row for
     * every other active supplement on that day too — see the impl. */
    suspend fun setTakenToday(supplementId: Long, taken: Int)

    /** Every supplement including soft-deleted ones — for data export, which must keep the ids a
     * [SupplementDay] points at. */
    suspend fun allSupplements(): List<Supplement>

    /** Every day with something actually taken, oldest first — for data export. */
    suspend fun allDays(): List<SupplementDay>

    /** Dated writes with the id intact — an import or the debug seed. */
    suspend fun upsertSupplement(supplement: Supplement)
    suspend fun upsertDay(day: SupplementDay)

    /** Drops both tables, for import's replace-in-full semantics. A hard delete is right here for
     * the same reason it is on an active fast: the history being replaced is not the user undoing
     * one row, it is the whole log being swapped for another device's. */
    suspend fun clearAll()
}
