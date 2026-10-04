package ph.mart.healthapp.wear.ui

import java.text.DateFormat
import java.util.Calendar
import java.util.Date

/**
 * The watch's own copies of two one-liners `:core:data` also has. Duplicated on purpose: sharing
 * them would mean putting `:core:data` — and Room with it — on the wrist, which is the one thing
 * this module is arranged to avoid.
 *
 * [epochDayOf] is not rule-free, and that is what makes it the copy to watch: it must give the
 * same key as `core.data.epochDayOf`, because the phone stamps `TodaySnapshot.dateEpochDay` with
 * that one and [isStale] compares the two. It missed the DST fix once — see [epochDayOf].
 */
internal fun formatClockTime(millis: Long): String =
    DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(millis))

internal fun todayEpochDay(): Long = epochDayOf(System.currentTimeMillis())

/**
 * Local midnight with [Calendar.DST_OFFSET] added before the divide — `core.data.epochDayOf`'s
 * rule, for its reason: in a zone whose standard offset is UTC+0 and which observes DST (London,
 * Dublin, Lisbon), the plain quotient lands a day behind the phone all summer, and the watch then
 * called every snapshot stale.
 */
internal fun epochDayOf(millis: Long): Long {
    val calendar = Calendar.getInstance().apply {
        timeInMillis = millis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    return (calendar.timeInMillis + calendar.get(Calendar.DST_OFFSET)) / 86_400_000L
}
