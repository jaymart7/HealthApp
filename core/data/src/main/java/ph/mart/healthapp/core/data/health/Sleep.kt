package ph.mart.healthapp.core.data.health

import kotlinx.coroutines.flow.Flow
import ph.mart.healthapp.core.data.progress.ChartRange

/** One night, as Home shows it. [dateEpochDay] is the day the sleep ended. */
data class SleepNight(val dateEpochDay: Long, val minutesAsleep: Int)

/** "7h 12m" — the only shape the UI renders, so the formatting lives with the model. The Int
 * overload is what the Progress stat row uses: it formats an average, not a night. */
fun formatDuration(minutes: Int): String {
    val hours = minutes / 60
    val rest = minutes % 60
    return if (hours == 0) "${rest}m" else "${hours}h ${rest}m"
}

fun SleepNight.formatDuration(): String = formatDuration(minutesAsleep)

/**
 * Anchored to today, like [ph.mart.healthapp.core.data.mood.inRange] and unlike
 * [ph.mart.healthapp.core.data.progress.inRange], which anchors to the latest weigh-in: the
 * sleep series is sparse, so a window headed "1M" must show the last 30 days with their gaps
 * intact rather than the 30 days around whenever the watch last synced.
 */
fun List<SleepNight>.inRange(range: ChartRange, todayEpochDay: Long): List<SleepNight> {
    val days = range.days ?: return this
    return filter { it.dateEpochDay >= todayEpochDay - days }
}

/** Nulls rather than zeros on an empty window, so the stat row renders "—" instead of "0m". */
data class SleepAverages(val averageMinutes: Int?, val longestMinutes: Int?, val nights: Int)

/** A pure fold over the window, like `moodAverages()` — not a Room aggregate. */
fun List<SleepNight>.sleepAverages(): SleepAverages = SleepAverages(
    averageMinutes = takeIf { it.isNotEmpty() }?.let { sumOf { night -> night.minutesAsleep } / it.size },
    longestMinutes = maxOfOrNull { it.minutesAsleep },
    nights = size,
)

/**
 * Read-only by design: every row comes from Google Health, so there is no `upsert` on the public
 * surface and no manual entry path to keep consistent with one.
 *
 * Not a streak domain, for the same reason mood isn't — the streak means "you logged something",
 * and a watch recording sleep while its owner ignores the app is not that.
 */
interface SleepRepository {
    /** The night that ended today. Null when nothing was imported for it — Home hides the card. */
    fun observeLastNight(): Flow<SleepNight?>

    /** Every imported night, oldest first — the Progress tab's Sleep series. */
    fun observeNights(): Flow<List<SleepNight>>
}
