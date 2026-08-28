package ph.mart.healthapp.reminder

import java.util.Calendar
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private val ZONE: TimeZone = TimeZone.getTimeZone("Asia/Manila")

private fun at(year: Int, month: Int, day: Int, hour: Int, minute: Int = 0): Long =
    Calendar.getInstance(ZONE).apply {
        clear()
        set(year, month, day, hour, minute, 0)
    }.timeInMillis

private fun describe(millis: Long): String =
    Calendar.getInstance(ZONE).apply { timeInMillis = millis }.let {
        "%04d-%02d-%02d %02d:%02d".format(
            it.get(Calendar.YEAR),
            it.get(Calendar.MONTH) + 1,
            it.get(Calendar.DAY_OF_MONTH),
            it.get(Calendar.HOUR_OF_DAY),
            it.get(Calendar.MINUTE),
        )
    }

class ReminderScheduleTest {

    // 2026-08-24 is a Monday; 2026-08-25 a Tuesday.

    @Test
    fun `before the hour today runs today`() {
        val now = at(2026, Calendar.AUGUST, 25, 7)
        assertEquals("2026-08-25 08:00", describe(nextRunMillis(hour = 8, dayOfWeek = null, nowMillis = now, zone = ZONE)))
    }

    @Test
    fun `after the hour today rolls to tomorrow`() {
        val now = at(2026, Calendar.AUGUST, 25, 9)
        val next = nextRunMillis(hour = 8, dayOfWeek = null, nowMillis = now, zone = ZONE)
        assertEquals("2026-08-26 08:00", describe(next))
        assertTrue("delay must be positive", next > now)
    }

    @Test
    fun `exactly on the hour rolls to tomorrow rather than firing twice`() {
        val now = at(2026, Calendar.AUGUST, 25, 8)
        assertEquals("2026-08-26 08:00", describe(nextRunMillis(hour = 8, dayOfWeek = null, nowMillis = now, zone = ZONE)))
    }

    @Test
    fun `weekly from a Tuesday waits for the next Monday`() {
        val now = at(2026, Calendar.AUGUST, 25, 9)
        assertEquals("2026-08-31 08:00", describe(nextRunMillis(hour = 8, dayOfWeek = Calendar.MONDAY, nowMillis = now, zone = ZONE)))
    }

    @Test
    fun `weekly early on the Monday itself runs that same morning`() {
        val now = at(2026, Calendar.AUGUST, 24, 7)
        assertEquals("2026-08-24 08:00", describe(nextRunMillis(hour = 8, dayOfWeek = Calendar.MONDAY, nowMillis = now, zone = ZONE)))
    }
}
