package ph.mart.healthapp.core.data

import java.util.Calendar
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow

/**
 * Today as a local-midnight epoch day — the key every dated table in this module uses.
 *
 * Public because the diary picks a day to show, and that day is compared against — and defaults
 * to — this one.
 *
 * ponytail: `java.util.Calendar`, not `java.time.LocalDate` — the project has no
 * core-library-desugaring configured, and day arithmetic elsewhere is plain `± 1` on the epoch
 * day. Switch to LocalDate + desugaring if real calendar math ever lands here.
 */
fun todayEpochDay(): Long = epochDayOf(System.currentTimeMillis())

/**
 * The same conversion for an arbitrary instant — an imported workout carries a UTC timestamp and
 * has to land on the local day the user actually trained.
 */
fun epochDayOf(millis: Long): Long {
    val calendar = Calendar.getInstance().apply {
        timeInMillis = millis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    return calendar.timeInMillis / 86_400_000L
}

/**
 * Local midnight of an epoch day, in milliseconds — the inverse of [epochDayOf], and what turns a
 * dated diary row into a timestamp Google Health can place on a timeline.
 *
 * Stepped from today with `Calendar.add` rather than multiplied out, so it stays right across DST
 * boundaries and in the zones whose offset pushes local midnight past a UTC day boundary.
 */
fun epochDayStartMillis(epochDay: Long): Long {
    val calendar = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
        add(Calendar.DAY_OF_YEAR, (epochDay - todayEpochDay()).toInt())
    }
    return calendar.timeInMillis
}

/**
 * Which weekday an epoch day falls on, **Monday = 0** through Sunday = 6 — the bit positions
 * `routine.days` is a mask over.
 *
 * Monday-first rather than `Calendar`'s Sunday-first because a training week is a training week:
 * the plan strip on Home reads Mon…Sun, and one conversion here beats seven off-by-ones at the
 * call sites. Built on [epochDayStartMillis] so it inherits that function's DST correctness.
 */
fun weekdayIndex(epochDay: Long): Int {
    val calendar = Calendar.getInstance().apply { timeInMillis = epochDayStartMillis(epochDay) }
    // Calendar.SUNDAY is 1 and Calendar.MONDAY is 2, so +5 mod 7 lands Monday on 0.
    return (calendar.get(Calendar.DAY_OF_WEEK) + 5) % 7
}

/**
 * Today, re-emitted at each local midnight.
 *
 * Every "today-only" repository overload flatMaps this rather than resolving [todayEpochDay]
 * once when its flow is built. That was a real bug, not a tidiness point: the reads resolved the
 * day at construction while every write resolved it fresh at call time, so an app left open past
 * midnight showed yesterday's water while `setToday()` wrote today's row — a tap that visibly did
 * nothing.
 *
 * The delay is computed off [epochDayStartMillis], so it stays right across DST rather than
 * assuming a day is 86,400,000ms. Doze can defer the wake-up; the day is recomputed whenever it
 * does fire, so a late one self-corrects instead of drifting.
 *
 * The home-screen widget does not use this — Glance holds no live flow, and its
 * `updatePeriodMillis` is what restarts its queries over midnight.
 */
fun todayFlow(): Flow<Long> = flow {
    while (true) {
        val today = todayEpochDay()
        emit(today)
        delay((epochDayStartMillis(today + 1) - System.currentTimeMillis()).coerceAtLeast(1L))
    }
}

/**
 * The wrapper every today-only overload is written in terms of: `observeToday() =
 * forToday(::observeDay)`. Internal because a `:feature:*` module asking "what is today" wants
 * [todayFlow] itself, not a query re-pointer.
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal fun <T> forToday(dated: (Long) -> Flow<T>): Flow<T> = todayFlow().flatMapLatest(dated)
