package ph.mart.healthapp.core.data

import java.util.Calendar

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
