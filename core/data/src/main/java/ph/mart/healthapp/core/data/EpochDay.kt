package ph.mart.healthapp.core.data

import java.util.Calendar

/**
 * Today as a local-midnight epoch day — the key every dated table in this module uses.
 *
 * ponytail: `java.util.Calendar`, not `java.time.LocalDate` — the project has no
 * core-library-desugaring configured, and the repositories only ever need "today". Switch to
 * LocalDate + desugaring if arbitrary-date math ever lands here.
 */
internal fun todayEpochDay(): Long {
    val calendar = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    return calendar.timeInMillis / 86_400_000L
}
