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
fun todayEpochDay(): Long {
    val calendar = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    return calendar.timeInMillis / 86_400_000L
}
