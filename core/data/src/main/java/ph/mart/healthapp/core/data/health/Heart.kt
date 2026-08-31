package ph.mart.healthapp.core.data.health

import kotlinx.coroutines.flow.Flow
import ph.mart.healthapp.core.data.progress.ChartRange

/**
 * One day of heart rate, as Home and the Progress tab show it.
 *
 * [minBpm] is the day's lowest reading, never a resting heart rate: FitPulse aggregates whatever
 * samples the watch happened to take, and calling a minimum "resting" would claim a measurement
 * nobody made.
 */
data class HeartDay(val dateEpochDay: Long, val averageBpm: Int, val minBpm: Int)

/** "68 bpm" — the only shape the UI renders, so the formatting lives with the model. */
fun formatBpm(bpm: Int): String = "$bpm bpm"

/**
 * Anchored to today, like the [SleepNight] and [ph.mart.healthapp.core.data.mood.MoodDay]
 * overloads and unlike [ph.mart.healthapp.core.data.progress.inRange], which anchors to the latest
 * weigh-in: the series is sparse, so a window headed "1M" must show the last 30 days with their
 * gaps intact rather than the 30 days around whenever the watch last synced.
 */
fun List<HeartDay>.inRange(range: ChartRange, todayEpochDay: Long): List<HeartDay> {
    val days = range.days ?: return this
    return filter { it.dateEpochDay >= todayEpochDay - days }
}

/** Nulls rather than zeros on an empty window, so the stat row renders "—" instead of "0 bpm". */
data class HeartAverages(val averageBpm: Int?, val lowestBpm: Int?, val days: Int)

/**
 * A pure fold over the window, like [sleepAverages] — not a Room aggregate. The mean is a mean of
 * the *days*, not of the samples: the samples are long gone by the time this runs, and a day the
 * watch took twice as many readings on is not twice the day.
 */
fun List<HeartDay>.heartAverages(): HeartAverages = HeartAverages(
    averageBpm = takeIf { it.isNotEmpty() }?.let { days -> days.sumOf { it.averageBpm } / days.size },
    lowestBpm = minOfOrNull { it.minBpm },
    days = size,
)

/**
 * Read-only by design, exactly like [SleepRepository] and [StepsRepository]: every row comes from
 * Google Health, so there is no `upsert` on the public surface and no manual entry path to keep
 * consistent with one.
 *
 * Not a streak domain — a watch counting beats while its owner ignores the app is not "you logged
 * something", which is why there is no `observeLoggedDays()` here.
 */
interface HeartRepository {
    /** Today. Null when nothing has been imported for it — the card is hidden, not zeroed. */
    fun observeToday(): Flow<HeartDay?>

    /** Every imported day, oldest first — the Progress tab's Heart series. */
    fun observeDays(): Flow<List<HeartDay>>
}
