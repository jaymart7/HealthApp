package ph.mart.healthapp.core.data.health

import kotlinx.coroutines.flow.Flow

/** One night, as Home shows it. [dateEpochDay] is the day the sleep ended. */
data class SleepNight(val dateEpochDay: Long, val minutesAsleep: Int)

/** "7h 12m" — the only shape Home renders, so the formatting lives with the model. */
fun SleepNight.formatDuration(): String {
    val hours = minutesAsleep / 60
    val minutes = minutesAsleep % 60
    return if (hours == 0) "${minutes}m" else "${hours}h ${minutes}m"
}

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
}
