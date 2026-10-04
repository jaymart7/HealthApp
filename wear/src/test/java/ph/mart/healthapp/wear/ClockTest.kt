package ph.mart.healthapp.wear

import java.util.Calendar
import java.util.TimeZone
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import ph.mart.healthapp.wear.ui.epochDayOf

/**
 * The watch's copy of the epoch-day key, held to `EpochDayTest`'s property — one local day, one
 * key, and the next day the next key — plus the one figure that ties it to the phone. It is the
 * key `isStale` compares against the phone's `TodaySnapshot.dateEpochDay`, so a copy that drifts
 * marks every snapshot stale.
 */
class ClockTest {

    private val original: TimeZone = TimeZone.getDefault()

    @After
    fun restoreZone() {
        TimeZone.setDefault(original)
    }

    private fun noonOn(year: Int, month: Int, day: Int): Calendar =
        Calendar.getInstance().apply {
            clear()
            set(year, month, day, 12, 0)
        }

    @Test
    fun `a local day is one key, and the next day is the next key`() {
        listOf("UTC", "Asia/Manila", "Europe/London").forEach { id ->
            TimeZone.setDefault(TimeZone.getTimeZone(id))
            val day = noonOn(2026, Calendar.JANUARY, 1)
            var previous = epochDayOf(day.timeInMillis)
            repeat(365) {
                day.add(Calendar.DAY_OF_YEAR, 1)
                val key = epochDayOf(day.timeInMillis)
                assertEquals("$id day ${day.get(Calendar.DAY_OF_YEAR)}", previous + 1, key)
                previous = key
            }
        }
    }

    /** The summer day the plain divide got wrong — 20635 is what `core.data.epochDayOf` gives it. */
    @Test
    fun `a London summer day keys where the phone keys it`() {
        TimeZone.setDefault(TimeZone.getTimeZone("Europe/London"))
        assertEquals(20635L, epochDayOf(noonOn(2026, Calendar.JULY, 1).timeInMillis))
    }
}
