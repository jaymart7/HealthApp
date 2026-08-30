package ph.mart.healthapp.core.designsystem.component

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Epoch-day conversion and display formatting, shared by [SheetDatePicker], [CalendarPanel] and
 * the food diary's date header.
 *
 * An epoch day is **local midnight divided by a day in millis** — the same definition
 * `ph.mart.healthapp.core.data.epochDayOf` uses to key every dated table. That is not "days since
 * 1970-01-01 local": east of UTC the two differ by one, which is exactly the trap this file fell
 * into before. `:core:designsystem` has no dependency on `:core:data`, so the contract is held by
 * `DateFormatTest` rather than by a shared function.
 */
private const val MILLIS_PER_DAY = 86_400_000L

private fun midnightToday(): Calendar = Calendar.getInstance().apply {
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}

/**
 * Stepped from today with [Calendar.add] rather than multiplied out, so it stays right across DST
 * boundaries and in the zones whose offset pushes local midnight past a UTC day boundary — the
 * same reason `core.data.epochDayStartMillis` steps instead of multiplying.
 */
internal fun epochDayToCalendar(epochDay: Long): Calendar =
    midnightToday().apply { add(Calendar.DAY_OF_YEAR, (epochDay - todayEpochDay()).toInt()) }

/** Normalises to local midnight first, so any calendar — not just one already at midnight — maps
 * to the day the user was looking at. */
internal fun Calendar.toEpochDay(): Long = (clone() as Calendar).apply {
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}.timeInMillis / MILLIS_PER_DAY

/**
 * Internal on purpose: `core.data.todayEpochDay` is the one every caller outside this module uses,
 * and two public copies of this is what produced an off-by-one day east of UTC. This one exists
 * only so [epochDayToCalendar] has an origin to step from.
 */
internal fun todayEpochDay(): Long = midnightToday().timeInMillis / MILLIS_PER_DAY

fun epochDayToDate(epochDay: Long): java.util.Date = epochDayToCalendar(epochDay).time

fun formatEpochDay(epochDay: Long): String =
    SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(epochDayToDate(epochDay))

/** "Tuesday" — unambiguous inside a week-long window, where the full date is too long a label. */
fun formatWeekday(epochDay: Long): String =
    SimpleDateFormat("EEEE", Locale.getDefault()).format(epochDayToDate(epochDay))
