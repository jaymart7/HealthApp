package ph.mart.healthapp.wear.ui

import java.text.DateFormat
import java.util.Calendar
import java.util.Date

/**
 * The watch's own copies of two one-liners `:core:data` also has. Duplicated on purpose: sharing
 * them would mean putting `:core:data` — and Room with it — on the wrist, which is the one thing
 * this module is arranged to avoid. Both are stdlib calls with no rule inside them to drift.
 */
internal fun formatClockTime(millis: Long): String =
    DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(millis))

internal fun todayEpochDay(): Long {
    val calendar = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    return calendar.timeInMillis / 86_400_000L
}
