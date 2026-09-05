package ph.mart.healthapp.core.data.cycle

import androidx.annotation.StringRes
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.Flow
import ph.mart.healthapp.core.data.R
import ph.mart.healthapp.core.data.progress.ChartRange

/**
 * One logged day of the menstrual cycle: how heavy the flow was, and what was felt.
 *
 * **[flow] `0` means "not logged", never a flow of zero** — the reading `mood_day`'s zero has, and
 * what makes a symptom-only day a first-class row instead of one demanding a flow tap it has no
 * answer for.
 */
data class CycleDay(
    val dateEpochDay: Long,
    val flow: Int,
    val symptoms: Set<CycleSymptom> = emptySet(),
) {
    val logged: Boolean get() = flow > 0 || symptoms.isNotEmpty()
}

val FLOW_SCALE = 1..4

/**
 * How heavy a logged day was.
 *
 * [Unstated] exists for the importer and is never offered as a tap: Health Connect's
 * `MenstruationPeriodRecord` hands back a span of days with no intensity on it, and writing
 * "Medium" for one would invent a figure the source never reported — the rule an imported blood
 * pressure reading's `pulseBpm = 0` already follows.
 */
enum class FlowLevel(val value: Int, @StringRes val label: Int) {
    Unstated(1, R.string.data_flow_level_unstated),
    Light(2, R.string.data_flow_level_light),
    Medium(3, R.string.data_flow_level_medium),
    Heavy(4, R.string.data_flow_level_heavy),
}

/** The three levels the UI offers. [FlowLevel.Unstated] is rendered, never tapped. */
val TAPPABLE_FLOW: List<FlowLevel> = listOf(FlowLevel.Light, FlowLevel.Medium, FlowLevel.Heavy)

fun flowLevelOf(value: Int): FlowLevel? = FlowLevel.entries.firstOrNull { it.value == value }

/** What a day can be tagged with. Reported, never graded — there is no field on the profile a
 * symptom target could be derived from, the rule fiber, sugar and sodium already follow. */
enum class CycleSymptom(@StringRes val label: Int) {
    Cramps(R.string.data_cycle_symptom_cramps),
    Headache(R.string.data_cycle_symptom_headache),
    Bloating(R.string.data_cycle_symptom_bloating),
    Fatigue(R.string.data_cycle_symptom_fatigue),
    MoodSwings(R.string.data_cycle_symptom_mood_swings),
    Tender(R.string.data_cycle_symptom_tender),
    Acne(R.string.data_cycle_symptom_acne),
    Nausea(R.string.data_cycle_symptom_nausea),
    BackPain(R.string.data_cycle_symptom_back_pain),
    Cravings(R.string.data_cycle_symptom_cravings),
}

/**
 * Parses the stored symptom string. A name this build doesn't know is **dropped** — the rule
 * `homeCardLayout()` keeps for exactly the same reason: a tag retired in a later version must not
 * leave a row unreadable.
 */
fun cycleSymptoms(stored: String): Set<CycleSymptom> =
    stored.split(',').mapNotNullTo(LinkedHashSet()) { token ->
        val name = token.trim()
        CycleSymptom.entries.firstOrNull { it.name == name }
    }

/** The inverse, in declaration order so the stored string is canonical whatever order they were
 * tapped in. */
fun encodeCycleSymptoms(symptoms: Set<CycleSymptom>): String =
    CycleSymptom.entries.filter { it in symptoms }.joinToString(",") { it.name }

/** A run of days with flow on them — derived, never stored. */
data class CyclePeriod(val startEpochDay: Long, val endEpochDay: Long) {
    val lengthDays: Int get() = (endEpochDay - startEpochDay + 1).toInt()

    operator fun contains(epochDay: Long): Boolean = epochDay in startEpochDay..endEpochDay
}

/**
 * How many missed days a period survives. Real logging skips a day, and a period split in two
 * corrupts every cycle length after it — one bad tap would move the prediction by a fortnight.
 *
 * ponytail: fixed at one day. A user-set tolerance is the upgrade path if it ever reads wrong.
 */
const val PERIOD_GAP_DAYS = 1

/**
 * The periods in a set of logged days, oldest first. Days with symptoms but no flow are *not*
 * period days: a cramp three days early is not bleeding, and treating it as one would move the
 * cycle length it anchors.
 */
fun List<CycleDay>.periods(): List<CyclePeriod> {
    val flowDays = filter { it.flow > 0 }.map { it.dateEpochDay }.distinct().sorted()
    if (flowDays.isEmpty()) return emptyList()
    val periods = mutableListOf<CyclePeriod>()
    var start = flowDays.first()
    var last = start
    for (day in flowDays.drop(1)) {
        if (day - last > PERIOD_GAP_DAYS + 1) {
            periods += CyclePeriod(start, last)
            start = day
        }
        last = day
    }
    return periods + CyclePeriod(start, last)
}

/** Start-to-start, in days — one figure per *completed* cycle, so N periods yield N-1 lengths. */
fun List<CyclePeriod>.cycleLengths(): List<Int> =
    zipWithNext { previous, next -> (next.startEpochDay - previous.startEpochDay).toInt() }

/** How many cycles the average is fitted over. Six months is enough to absorb one odd month
 * without averaging in a year-old cycle the body has moved on from. */
const val PREDICTION_WINDOW_CYCLES = 6

/**
 * When the next period is expected, and what that guess is made of.
 *
 * [daysAway] is negative once the date has passed — the card says "expected 2 days ago" rather
 * than hiding, because a late period is the thing someone opens this feature to check.
 */
data class CyclePrediction(
    val nextStartEpochDay: Long,
    val averageCycleDays: Int,
    val basedOnCycles: Int,
) {
    fun daysAway(todayEpochDay: Long): Int = (nextStartEpochDay - todayEpochDay).toInt()
}

/**
 * Null until there are **two** period starts: one start is not a cycle, the same refusal
 * `Recap.weightArcKg` makes for a window holding a single weigh-in. FitPulse reports figures it
 * measured and never invents one.
 *
 * There is deliberately no fertile window and no ovulation date here. This app names things and
 * reports numbers; it does not advise — and a fertile window derived from a mean cycle length is
 * a contraception claim it cannot stand behind.
 */
fun List<CyclePeriod>.cyclePrediction(): CyclePrediction? {
    val lengths = cycleLengths().takeLast(PREDICTION_WINDOW_CYCLES)
    if (lengths.isEmpty()) return null
    val average = lengths.average().roundToInt()
    return CyclePrediction(
        nextStartEpochDay = last().startEpochDay + average,
        averageCycleDays = average,
        basedOnCycles = lengths.size,
    )
}

/** Which day of the current cycle today is — 1 on the day the last period started. Null before
 * the first period is logged, and while the only period on record starts in the future. */
fun List<CyclePeriod>.cycleDayNumber(todayEpochDay: Long): Int? {
    val start = lastOrNull { it.startEpochDay <= todayEpochDay }?.startEpochDay ?: return null
    return (todayEpochDay - start + 1).toInt()
}

/** [daysLogged] counts days with a flow *or* a symptom, so it can exceed either denominator —
 * `MoodAverages`' rule, and for the same reason. */
data class CycleAverages(val cycleDays: Double?, val periodDays: Double?, val daysLogged: Int)

/**
 * Each series keeps its own denominator, and a period that is **still running** is left out of the
 * period-length average: it is measured short by however many days are left in it, and a figure
 * that dips every month and recovers is a figure nobody can read.
 */
fun List<CycleDay>.cycleAverages(todayEpochDay: Long): CycleAverages {
    val periods = periods()
    val lengths = periods.cycleLengths()
    val finished = periods.filterNot { todayEpochDay in it }
    return CycleAverages(
        cycleDays = lengths.takeIf { it.isNotEmpty() }?.average(),
        periodDays = finished.takeIf { it.isNotEmpty() }?.map { it.lengthDays }?.average(),
        daysLogged = count { it.logged },
    )
}

/**
 * Anchored to today, like `MoodDay.inRange` and unlike `WeightEntry.inRange`: a sparse series
 * headed "1M" has to show the last 30 days with their gaps intact, not the 30 days around
 * whenever the last one was logged.
 */
fun List<CycleDay>.inRange(range: ChartRange, todayEpochDay: Long): List<CycleDay> {
    val days = range.days ?: return this
    return filter { it.dateEpochDay >= todayEpochDay - days }
}

/**
 * One row per day holding a flow and a set of tags — `MoodRepository`'s shape, and its reasoning:
 * correcting a tap is an update rather than a delete, which keeps this domain inside the
 * soft-delete-only rule without a deleted flag.
 *
 * Deliberately **not** a streak domain. The streak's four domains (food, water, weigh-in,
 * exercise) are things the user *did*; a body noticing itself is not one, and folding a fifth in
 * now would rewrite what every past run meant. That is why there is no `observeLoggedDays()` here.
 *
 * Nothing in this domain ever reaches an AI payload, the widget, the watch or the weekly recap.
 */
interface CycleRepository {
    /** Zero-filled, never null — the Home card never has to special-case a missing row. */
    fun observeToday(): Flow<CycleDay>

    /** A [FlowLevel] value, or 0 to clear. Leaves that day's symptoms untouched. */
    suspend fun setTodayFlow(flow: Int)

    /** Logged days only, oldest first — the Home card's prediction and the Progress page. */
    fun observeDays(): Flow<List<CycleDay>>

    /** Dated whole-row write — the logging sheet, an import or the debug seed. */
    suspend fun upsertDay(day: CycleDay)

    /**
     * Health Connect's imported period days. A day that already carries a flow is **skipped** —
     * the typed value wins, exactly as an imported weigh-in skips a day already weighed by hand.
     * Returns how many rows actually landed, for the sync's own item count.
     */
    suspend fun importDays(days: List<CycleDay>): Int

    /** Every logged day, oldest first — for data export. */
    suspend fun allDays(): List<CycleDay>

    /** Zeroes every day, for import's replace-in-full semantics. */
    suspend fun clearAllDays()
}
