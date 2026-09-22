package ph.mart.healthapp.core.designsystem.component

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Epoch-day conversion and display formatting, shared by [SheetDatePicker], [CalendarPanel] and
 * the food diary's date header.
 *
 * An epoch day is **local midnight, shifted by the DST offset, divided by a day in millis** — the
 * same definition `ph.mart.healthapp.core.data.epochDayOf` uses to key every dated table. That is
 * not "days since 1970-01-01 local": east of UTC the two differ by one, which is exactly the trap
 * this file fell into before. `:core:designsystem` has no dependency on `:core:data`, so the
 * contract is held by `DateFormatTest` rather than by a shared function — [epochDay] is this
 * file's whole half of it.
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

/**
 * A calendar already at local midnight, as a day key.
 *
 * **[Calendar.DST_OFFSET] is added before the divide, and that is load-bearing.** Local midnight
 * is not a fixed distance from a UTC day boundary: it moves an hour at a transition, and in a zone
 * whose *standard* offset is UTC+0 and which observes DST — Europe/London, Dublin, Lisbon, the
 * Canaries, Casablanca — that hour crosses the boundary. Without it, 2026-03-29 and 2026-03-30 in
 * London both answered 20541 and October skipped 20751. Adding the offset puts every local
 * midnight on its *standard-time* UTC instant, so the key advances by exactly one per calendar day
 * in every zone; wherever the offset is zero it changes nothing. `core.data.epochDayOf` carries
 * the same line for the same reason.
 */
private fun Calendar.epochDay(): Long = (timeInMillis + get(Calendar.DST_OFFSET)) / MILLIS_PER_DAY

/** Normalises to local midnight first, so any calendar — not just one already at midnight — maps
 * to the day the user was looking at. */
internal fun Calendar.toEpochDay(): Long = (clone() as Calendar).apply {
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}.epochDay()

/**
 * Internal on purpose: `core.data.todayEpochDay` is the one every caller outside this module uses,
 * and two public copies of this is what produced an off-by-one day east of UTC. This one exists
 * only so [epochDayToCalendar] has an origin to step from.
 */
internal fun todayEpochDay(): Long = midnightToday().epochDay()

fun epochDayToDate(epochDay: Long): java.util.Date = epochDayToCalendar(epochDay).time

fun formatEpochDay(epochDay: Long): String =
    SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(epochDayToDate(epochDay))

/** "Sep 12" — a date inside a set whose year the set itself already establishes: a grid of months,
 * a comparison pair, a strip. The year would be the same word repeated on every frame. */
fun formatDayMonth(epochDay: Long): String =
    SimpleDateFormat("MMM d", Locale.getDefault()).format(epochDayToDate(epochDay))

/** "Tuesday" — unambiguous inside a week-long window, where the full date is too long a label. */
fun formatWeekday(epochDay: Long): String =
    SimpleDateFormat("EEEE", Locale.getDefault()).format(epochDayToDate(epochDay))

/** "Sep" — the x-axis label a chart spanning months can fit. Short by necessity: four of them
 * share the width of a phone. */
fun formatMonth(epochDay: Long): String =
    SimpleDateFormat("MMM", Locale.getDefault()).format(epochDayToDate(epochDay))

/**
 * "8:42" — a time of day, for the one place the app prints one: the add-entry sheet's subtitle
 * while correcting a logged row, which has to say *which* row.
 *
 * Takes epoch millis rather than an epoch day, because this is the only figure in the app that is
 * not a date. `java.text` gives the locale's own short form, so 24-hour locales get 08:42.
 *
 * ponytail: the locale's convention, not the device's 12/24-hour *setting* —
 * `android.text.format.DateFormat.getTimeFormat` reads that, and needs a Context this file
 * deliberately has none of. Swap it in at the call site if someone notices.
 */
fun formatTimeOfDay(epochMillis: Long): String =
    java.text.DateFormat.getTimeInstance(java.text.DateFormat.SHORT, Locale.getDefault())
        .format(java.util.Date(epochMillis))

/**
 * The same clock face for a **minute past local midnight** — `0..1439`, the shape the weigh-in,
 * measurement, cycle and progress-photo rows store their time of day in (`core.data.nowMinuteOfDay`).
 *
 * The fields are *set* on today's midnight rather than added to it in millis, because a stored
 * minute is a wall-clock reading and not an elapsed duration: `midnight + 390 * 60_000` prints
 * 5:30 or 7:30 on the two mornings a year a DST zone shifts, where a 6:30 weigh-in is 6:30 on
 * every one of them. The one hour a spring-forward skips has no wall clock to print and
 * normalises forward; nothing is stored in it, because it never occurred.
 */
fun formatMinuteOfDay(minuteOfDay: Int): String = formatTimeOfDay(
    midnightToday().apply {
        set(Calendar.HOUR_OF_DAY, minuteOfDay / 60)
        set(Calendar.MINUTE, minuteOfDay % 60)
    }.timeInMillis,
)

/**
 * "August", or "August 2024" when the day is not in the current year — the heading over a run of
 * days in a list that scrolls back through months.
 *
 * Long-form where [formatMonth] is short, because this one has a whole row to itself rather than
 * four of it sharing an axis. The year appears only when it has something to say: in a list that
 * mostly spans weeks, "August 2025" on every band is a year repeated for nothing, and in one that
 * spans years, two bands both saying "August" is the ambiguity this exists to close.
 */
fun formatMonthYear(epochDay: Long): String {
    val pattern = if (epochDayToCalendar(epochDay).get(Calendar.YEAR) == midnightToday().get(Calendar.YEAR)) {
        "MMMM"
    } else {
        "MMMM yyyy"
    }
    return SimpleDateFormat(pattern, Locale.getDefault()).format(epochDayToDate(epochDay))
}
