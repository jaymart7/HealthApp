package ph.mart.healthapp.core.data.bloodpressure

import kotlinx.coroutines.flow.Flow
import ph.mart.healthapp.core.data.epochDayOf
import ph.mart.healthapp.core.data.progress.ChartRange

/**
 * One cuff reading, at the moment it was taken.
 *
 * Stored per *reading* rather than per day, unlike [ph.mart.healthapp.core.data.mood.MoodDay] and
 * [ph.mart.healthapp.core.data.health.HeartDay]: morning and evening readings are the whole point
 * of tracking blood pressure, and a day-keyed table where the second reading overwrites the first
 * throws away the thing being measured.
 *
 * There is deliberately **no stored date column**. The timestamp is the datum and [dateEpochDay]
 * derives the local day from it, so the two can never disagree about when a reading was taken —
 * the same reason `fast_session` carries no status flag beside its null `endMillis`.
 *
 * [pulseBpm] is `0` when the user didn't type the figure the cuff also showed, the reading `0`
 * already has for a food entry's sodium and for an untapped mood. It never reaches `heart_day`:
 * that table is the watch's, and folding a cuff reading into it would claim a measurement the
 * watch never took.
 */
data class BloodPressureReading(
    val id: Long = 0,
    val takenAtMillis: Long,
    val systolic: Int,
    val diastolic: Int,
    val pulseBpm: Int = 0,
) {
    val dateEpochDay: Long get() = epochDayOf(takenAtMillis)
}

/** Clamped on write, and what the sheet's steppers bound. Wide enough to admit a reading a cuff
 * can actually produce, narrow enough that a typo of 1280 doesn't reach the chart's axis. */
val SYSTOLIC_RANGE = 60..250
val DIASTOLIC_RANGE = 30..160
val PULSE_RANGE = 30..220

/**
 * The standard AHA bands, shown as a plain label and nothing more — no advice copy, no "see a
 * doctor". 128/82 means nothing to most people without it, and grading it is not this app's job.
 *
 * [severe] is true for [Crisis] alone, and is the only thing that colours a reading. That is the
 * trend-arrow rule applied once: `error` is for genuinely off-track, not for a five-step scale.
 */
enum class BloodPressureCategory(val label: String, val severe: Boolean = false) {
    Normal("Normal"),
    Elevated("Elevated"),
    Stage1("High (stage 1)"),
    Stage2("High (stage 2)"),
    Crisis("Crisis", severe = true),
}

/**
 * **Worst-first, and the order is load-bearing.** A reading is categorised by whichever number is
 * further along, so 185/70 is a crisis; a normal-first chain would read its diastolic and call the
 * same reading Elevated.
 */
fun categoryOf(systolic: Int, diastolic: Int): BloodPressureCategory = when {
    systolic > 180 || diastolic > 120 -> BloodPressureCategory.Crisis
    systolic >= 140 || diastolic >= 90 -> BloodPressureCategory.Stage2
    systolic >= 130 || diastolic >= 80 -> BloodPressureCategory.Stage1
    systolic >= 120 -> BloodPressureCategory.Elevated
    else -> BloodPressureCategory.Normal
}

val BloodPressureReading.category: BloodPressureCategory get() = categoryOf(systolic, diastolic)

/** "128/82" — the only shape the UI renders, so the formatting lives with the model, like
 * [ph.mart.healthapp.core.data.health.formatBpm]. */
fun formatBloodPressure(systolic: Int, diastolic: Int): String = "$systolic/$diastolic"

/** One day's readings folded to their means, oldest first. Days with no reading are absent rather
 * than zero — the series is sparse, and the chart draws the gap. */
data class BloodPressureDay(
    val dateEpochDay: Long,
    val systolic: Int,
    val diastolic: Int,
    val readings: Int,
)

fun List<BloodPressureReading>.byDay(): List<BloodPressureDay> = groupBy { it.dateEpochDay }
    .toSortedMap()
    .map { (date, readings) ->
        BloodPressureDay(
            dateEpochDay = date,
            systolic = readings.sumOf { it.systolic } / readings.size,
            diastolic = readings.sumOf { it.diastolic } / readings.size,
            readings = readings.size,
        )
    }

/** Nulls rather than zeros on an empty window, so the stat row renders "—" instead of "0/0". */
data class BloodPressureAverages(
    val systolic: Int?,
    val diastolic: Int?,
    val pulseBpm: Int?,
    val readings: Int,
)

/**
 * A mean of the **days**, folded over [byDay] — for the reason
 * [ph.mart.healthapp.core.data.health.heartAverages] states: a morning someone measured four times
 * is not four mornings.
 *
 * The pulse keeps its own denominator and skips zeros, the rule
 * [ph.mart.healthapp.core.data.mood.moodAverages] follows: a month of readings taken off a cuff
 * that shows no pulse reports a blank pulse, not a quietly halved one.
 */
fun List<BloodPressureReading>.averages(): BloodPressureAverages {
    val days = byDay()
    val pulses = filter { it.pulseBpm > 0 }
    return BloodPressureAverages(
        systolic = days.takeIf { it.isNotEmpty() }?.let { d -> d.sumOf { it.systolic } / d.size },
        diastolic = days.takeIf { it.isNotEmpty() }?.let { d -> d.sumOf { it.diastolic } / d.size },
        pulseBpm = pulses.takeIf { it.isNotEmpty() }?.let { p -> p.sumOf { it.pulseBpm } / p.size },
        readings = size,
    )
}

/**
 * Anchored to today, like the mood, sleep, heart and supplement overloads and unlike
 * [ph.mart.healthapp.core.data.progress.inRange], which anchors to the latest weigh-in: nobody
 * takes their blood pressure daily, so a window headed "1M" must show the last 30 days with their
 * gaps intact rather than the 30 days around whenever the last reading happened to land.
 */
fun List<BloodPressureReading>.inRange(range: ChartRange, todayEpochDay: Long): List<BloodPressureReading> {
    val days = range.days ?: return this
    return filter { it.dateEpochDay >= todayEpochDay - days }
}

/**
 * Manual entry only. Blood pressure has a Google Health scope, and it is deliberately not
 * requested: the four the app already asks for cap it at 100 users pending OAuth verification and
 * a CASA assessment, and a fifth would need its own justification on that form.
 *
 * Deliberately **not** a streak domain, for the reason mood, sleep, fasting and supplements
 * aren't: the streak's four domains are fixed, and adding a fifth now would change what a past run
 * meant. That is why there is no `observeLoggedDays()` here.
 */
interface BloodPressureRepository {
    /** The most recent reading, or null before the first one. What Home's card renders. */
    fun observeLatest(): Flow<BloodPressureReading?>

    /** Every reading, oldest first — the Progress tab's series and its list. */
    fun observeReadings(): Flow<List<BloodPressureReading>>

    /** Values outside the three ranges are clamped, never rejected. */
    suspend fun addReading(reading: BloodPressureReading)

    /** Soft delete, like a diary row — a mistyped reading is removed, never erased. */
    suspend fun deleteReading(id: Long)

    /** Every reading, oldest first — for data export. */
    suspend fun allReadings(): List<BloodPressureReading>

    /** Soft-deletes everything, for import's replace-in-full semantics. */
    suspend fun clearAllReadings()
}
