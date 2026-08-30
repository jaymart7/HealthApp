package ph.mart.healthapp.core.designsystem.component

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Epoch-day conversion and display formatting, shared by [SheetDatePicker], [CalendarPanel] and
 * the food diary's date header.
 */
private const val MILLIS_PER_DAY = 86_400_000L

internal fun epochDayToCalendar(epochDay: Long): Calendar = Calendar.getInstance().apply {
    timeInMillis = epochDay * MILLIS_PER_DAY
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}

internal fun Calendar.toEpochDay(): Long = timeInMillis / MILLIS_PER_DAY

fun todayEpochDay(): Long = epochDayToCalendar(System.currentTimeMillis() / MILLIS_PER_DAY).toEpochDay()

fun epochDayToDate(epochDay: Long): java.util.Date = epochDayToCalendar(epochDay).time

fun formatEpochDay(epochDay: Long): String =
    SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(epochDayToDate(epochDay))

/** "Tuesday" — unambiguous inside a week-long window, where the full date is too long a label. */
fun formatWeekday(epochDay: Long): String =
    SimpleDateFormat("EEEE", Locale.getDefault()).format(epochDayToDate(epochDay))
