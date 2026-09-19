package ph.mart.healthapp.core.data.supplement

import kotlinx.coroutines.flow.Flow
import ph.mart.healthapp.core.data.food.Nutrients
import ph.mart.healthapp.core.data.food.isEmpty
import ph.mart.healthapp.core.data.food.plus
import ph.mart.healthapp.core.data.food.times
import ph.mart.healthapp.core.data.hasWeekday
import ph.mart.healthapp.core.data.progress.ChartRange
import ph.mart.healthapp.core.data.weekdayIndex
import ph.mart.healthapp.core.data.weekdayLabel

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
    /**
     * What one *dose* carries, of the seven this app grades — the label's own per-serving column,
     * because a tick is a serving. Empty for every supplement typed by hand, which is the ordinary
     * case and not a gap: `0` is unknown-or-none here exactly as it is everywhere else
     * [Nutrients] travels.
     *
     * Snapshotted at scan time and never re-read, the rule [SupplementDay.dueTimes] already
     * follows one field over: a reformulated bottle rescanned next year must not restate what last
     * year's ticks contributed.
     */
    val nutrients: Nutrients = Nutrients(),
    /**
     * The Supplement Facts panel as printed, one line per declaration
     * ("Vitamin C 90 mg\nZinc 11 mg"). Shown, never parsed.
     *
     * It exists because [nutrients] cannot hold most of a multivitamin: vitamin A, C, E, B12, zinc
     * and magnesium have no field in this app and no target on the profile to grade one against,
     * and a bottle that declares twenty lines should not appear to declare four. Third-party
     * product text, never authored here, which is why it is a String and not a resource.
     */
    val panel: String = "",
    /**
     * Which weekdays this is due on — the Monday-first mask `Weekday.kt` defines, the same one
     * [ph.mart.healthapp.core.data.exercise.Routine.days] is written in.
     *
     * **[EVERY_DAY] rather than 0 is the empty state.** A routine's 0 means "not on the plan yet",
     * which is a real thing for a routine and nothing at all for a supplement: something due on no
     * day cannot be taken, ticked or charted. So 0 is unreachable — the edit sheet refuses the
     * toggle that would empty the mask and the repository normalises it on write — and every row
     * that predates this field reads as due daily, which is exactly what it was.
     *
     * It is deliberately **not** snapshotted onto [SupplementDay] the way [SupplementDay.dueTimes]
     * is. It doesn't need to be: a day this isn't due on gets no row at all, and an absent row is
     * already what the chart reads as "not tracked". Narrowing a schedule next month leaves every
     * past day exactly as it was for the same reason a changed `timesPerDay` does.
     */
    val days: Int = EVERY_DAY,
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

/** All seven bits — what every supplement is until the user narrows it. */
const val EVERY_DAY = 0b1111111

/** Whether this supplement is due on [epochDay]. The one question the mask is asked. */
fun Supplement.isDueOn(epochDay: Long): Boolean = days.hasWeekday(weekdayIndex(epochDay))

/** "Mon · Wed · Fri", and empty on a supplement due daily — a caller that wants to say "every
 * day" has better words for it than seven abbreviations in a row. */
fun Supplement.dayLabel(): String = if (days == EVERY_DAY) "" else days.weekdayLabel()

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
 * What each day's ticks contributed, keyed by day — the diary's panel and the Nutrition page's
 * range average read the same map.
 *
 * A dose is the unit: [SupplementDay.taken] doses of a supplement carry [Supplement.nutrients]
 * that many times over, which is the one place [Nutrients.times] is handed a count rather than a
 * portion factor. A day whose ticks carry nothing is **absent rather than zero**, the sparseness
 * [adherenceByDay] already keeps: a day of supplements nobody scanned is a day with no figures,
 * not a day of zeros, and the panel above it must not grade it as a shortfall.
 *
 * [supplements] must include soft-deleted rows. A past day points at a supplement by id and
 * dropping the row it names would silently drop what that day carried — the same reason the export
 * carries them.
 */
fun supplementNutrientsByDay(
    days: List<SupplementDay>,
    supplements: List<Supplement>,
): Map<Long, Nutrients> {
    val byId = supplements.associateBy { it.id }
    return days.groupBy { it.dateEpochDay }.mapNotNull { (date, rows) ->
        val total = rows.fold(Nutrients()) { acc, row ->
            val supplement = byId[row.supplementId] ?: return@fold acc
            if (row.taken <= 0) acc else acc + supplement.nutrients * row.taken.toDouble()
        }
        if (total.isEmpty) null else date to total
    }.toMap()
}

/**
 * Anchored to today, like [ph.mart.healthapp.core.data.mood.inRange] and unlike the weight
 * series: a chart headed "1M" must show the last 30 days with their gaps intact, not the 30 days
 * around whenever the user last ticked something.
 */
fun List<SupplementDay>.inRange(range: ChartRange, todayEpochDay: Long): List<SupplementDay> {
    val days = range.days
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
     * callers combine at the arity the typed `combine` overloads stop at.
     *
     * **Only what is due today.** A checklist is a list of things to do, so a Monday-only
     * supplement is absent on a Tuesday rather than present and unticked — which is also what
     * keeps the reminder quiet and the coach honest, both of which read this. */
    fun observeToday(): Flow<List<SupplementToday>>

    /** Every day with a row, oldest first — the Progress tab. Sparse, unlike daily nutrition. */
    fun observeDays(): Flow<List<SupplementDay>>

    /** What each day's ticks carried — [supplementNutrientsByDay] over the whole log, including
     * supplements since deleted. One flow for both callers: the diary takes its own date out of
     * the map, the Nutrition page sums a range of it. */
    fun observeNutrientsByDay(): Flow<Map<Long, Nutrients>>

    suspend fun addSupplement(supplement: Supplement)

    /** Name, dose and times-per-day in one write — the edit sheet saves all three at once. */
    suspend fun updateSupplement(supplement: Supplement)

    /** Soft delete. Past days keep pointing at the row so the chart can still name it. */
    suspend fun deleteSupplement(id: Long)

    /** [taken] is clamped to the supplement's own [Supplement.timesPerDay]. Writes a zero row for
     * every other supplement *due today* as well — see the impl. */
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
