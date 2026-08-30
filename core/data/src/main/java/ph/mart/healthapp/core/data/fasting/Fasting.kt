package ph.mart.healthapp.core.data.fasting

import java.text.DateFormat
import java.util.Date
import kotlinx.coroutines.flow.Flow
import ph.mart.healthapp.core.data.epochDayOf
import ph.mart.healthapp.core.data.health.formatDuration
import ph.mart.healthapp.core.data.progress.ChartRange

/**
 * One fast. [endMillis] null means it is still running — see
 * [ph.mart.healthapp.core.data.fasting.local.FastSessionEntity] for why that is the marker and why
 * [goalHours] is carried on the row rather than read back off the profile.
 */
data class FastSession(
    val id: Long = 0,
    val startMillis: Long,
    val endMillis: Long? = null,
    val goalHours: Int = DEFAULT_FAST_GOAL_HOURS,
)

const val DEFAULT_FAST_GOAL_HOURS = 16

/** Below 12 it stops being a fast and starts being the gap between dinner and breakfast; above 24
 * it needs supervision this app can't give. */
val FAST_GOAL_HOURS = 12..24

private const val HOUR_MILLIS = 3_600_000L
private const val MINUTE_MILLIS = 60_000L

val FastSession.isActive: Boolean get() = endMillis == null

/** Wall-clock length so far. [nowMillis] is only read while the fast is running — a finished one
 * has both ends and can't drift. */
fun FastSession.durationMillis(nowMillis: Long): Long =
    ((endMillis ?: nowMillis) - startMillis).coerceAtLeast(0)

fun FastSession.durationMinutes(nowMillis: Long): Int = (durationMillis(nowMillis) / MINUTE_MILLIS).toInt()

/** When this fast hits its target — the instant the goal notification is scheduled for, and what
 * the widget prints instead of an elapsed time it cannot keep current. */
val FastSession.goalReachedMillis: Long get() = startMillis + goalHours * HOUR_MILLIS

fun FastSession.reachedGoal(nowMillis: Long): Boolean = (endMillis ?: nowMillis) >= goalReachedMillis

/**
 * The day a fast belongs to is the day it **ended**, like [ph.mart.healthapp.core.data.health.SleepNight]:
 * a 16-hour fast started at 20:00 is yesterday evening's discipline paying off this lunchtime, and
 * charting it on the start day would put every bar a day early.
 */
val FastSession.dateEpochDay: Long get() = epochDayOf(endMillis ?: startMillis)

/** "14h 20m" — the same formatter the sleep card uses, so two durations in this app never render
 * two different ways. */
fun formatElapsed(millis: Long): String = formatDuration((millis / MINUTE_MILLIS).toInt())

/**
 * "8:00 PM" / "20:00" — locale's short time, so the 12/24-hour choice follows the device without
 * needing a Context. Lives here beside [formatElapsed] because Home and the home-screen widget
 * both print it and neither should invent its own.
 */
fun formatClockTime(millis: Long): String =
    DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(millis))

/**
 * Anchored to today, like [ph.mart.healthapp.core.data.mood.inRange] and
 * [ph.mart.healthapp.core.data.health.inRange], and unlike
 * [ph.mart.healthapp.core.data.progress.inRange], which anchors to the latest weigh-in: the series
 * is sparse, so a window headed "1M" has to show the last 30 days with their gaps intact.
 */
fun List<FastSession>.inRange(range: ChartRange, todayEpochDay: Long): List<FastSession> {
    val days = range.days ?: return this
    return filter { it.dateEpochDay >= todayEpochDay - days }
}

/** Nulls rather than zeros on an empty window, so the stat row renders "—" instead of "0m" —
 * [ph.mart.healthapp.core.data.health.SleepAverages]' rule. */
data class FastingAverages(
    val averageMinutes: Int?,
    val longestMinutes: Int?,
    val goalsHit: Int,
    val count: Int,
)

/**
 * A pure fold over the window, like `sleepAverages()` — not a Room aggregate. Only completed fasts
 * reach here (see [FastingRepository.observeSessions]), so `nowMillis` never matters and the
 * arithmetic can't move under a running clock.
 */
fun List<FastSession>.fastingAverages(): FastingAverages {
    val minutes = map { it.durationMinutes(nowMillis = 0) }
    return FastingAverages(
        averageMinutes = minutes.takeIf { it.isNotEmpty() }?.let { it.sum() / it.size },
        longestMinutes = minutes.maxOrNull(),
        goalsHit = count { it.reachedGoal(nowMillis = 0) },
        count = size,
    )
}

/**
 * Stored one row per fast, not per day — a fast crosses midnight, so nothing about it is a date.
 *
 * Deliberately **not** part of the logging streak, for the same reason mood and sleep aren't: the
 * streak means "you logged something you did", and a timer left running while its owner ignores
 * the app is not that. That is why there is no `observeLoggedDays()` here.
 */
interface FastingRepository {
    /** The fast currently running, or null. Home's card and the goal notification both read this,
     * so neither can believe a fast is open that the other has ended. */
    fun observeActive(): Flow<FastSession?>

    /** Completed fasts only, oldest first — the Progress tab's series. A running fast is not yet
     * history and would keep growing under the chart. */
    fun observeSessions(): Flow<List<FastSession>>

    /** No-op while a fast is already open: `endMillis IS NULL` is the marker, so two open rows
     * would make "the active fast" ambiguous. */
    suspend fun start(goalHours: Int)

    /** Stamps the open fast's end. No-op when nothing is running. */
    suspend fun stop()

    /** Throws away a fast that never finished — the mis-tap undo. Completed sessions are never
     * deleted, which is what keeps this domain inside the soft-delete-only rule. */
    suspend fun discardActive()

    /** Dated write — an import or the debug seed. */
    suspend fun upsertSession(session: FastSession)

    /** Every completed fast, oldest first — for data export. */
    suspend fun allSessions(): List<FastSession>

    /** For import's replace-in-full semantics. */
    suspend fun clearAllSessions()
}
