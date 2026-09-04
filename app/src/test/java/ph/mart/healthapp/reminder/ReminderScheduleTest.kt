package ph.mart.healthapp.reminder

import java.util.Calendar
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import ph.mart.healthapp.core.data.fasting.FastSession

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

    @Test
    fun `weekly from a Tuesday waits for the coming Sunday`() {
        val now = at(2026, Calendar.AUGUST, 25, 9)
        assertEquals("2026-08-30 19:00", describe(nextRunMillis(hour = 19, dayOfWeek = Calendar.SUNDAY, nowMillis = now, zone = ZONE)))
    }

    @Test
    fun `weekly early on the Sunday itself runs that same evening`() {
        val now = at(2026, Calendar.AUGUST, 30, 9)
        assertEquals("2026-08-30 19:00", describe(nextRunMillis(hour = 19, dayOfWeek = Calendar.SUNDAY, nowMillis = now, zone = ZONE)))
    }

    // The recap's quiet-week guard: the same predicate `recap()` returns null on, so the
    // notification can never open an overlay with nothing in it.

    @Test
    fun `one logged day anywhere in the week is enough`() {
        assertTrue(hasRecapToShow(setOf(20_000L), todayEpochDay = 20_006))
        assertTrue(hasRecapToShow(setOf(20_006L), todayEpochDay = 20_006))
        assertTrue(hasRecapToShow(setOf(20_003L), todayEpochDay = 20_006))
    }

    @Test
    fun `an empty week stays quiet`() {
        assertFalse(hasRecapToShow(emptySet(), todayEpochDay = 20_006))
    }

    /** The window is seven days ending today — a day on either side of it does not count. */
    @Test
    fun `a day outside the seven does not count`() {
        assertFalse(hasRecapToShow(setOf(19_999L), todayEpochDay = 20_006))
        assertFalse(hasRecapToShow(setOf(20_007L), todayEpochDay = 20_006))
    }

    // The fasting goal is the one one-shot: its target comes from when the user stopped eating,
    // not from a clock, so it has no `nextRunMillis` to answer to.

    @Test
    fun `the fasting target is the fast's own snapshotted goal`() {
        val start = at(2026, Calendar.AUGUST, 24, 20)
        val target = fastingGoalTargetMillis(FastSession(startMillis = start, goalHours = 16), true)
        assertEquals("2026-08-25 12:00", describe(target!!))
    }

    @Test
    fun `no fast and a switched-off reminder both schedule nothing`() {
        val start = at(2026, Calendar.AUGUST, 24, 20)
        assertNull(fastingGoalTargetMillis(null, true))
        assertNull(fastingGoalTargetMillis(FastSession(startMillis = start, goalHours = 16), false))
    }

    @Test
    fun `the delay is measured to the target and stays positive`() {
        val now = at(2026, Calendar.AUGUST, 24, 20)
        val target = at(2026, Calendar.AUGUST, 25, 12)
        assertEquals(16 * 3_600_000L, fastingGoalDelayMillis(target, now))
    }

    /** A target already past means the work enqueued when the fast started has fired; scheduling
     * again on the next profile emission would be a duplicate. */
    @Test
    fun `a target already in the past schedules nothing`() {
        val target = at(2026, Calendar.AUGUST, 25, 12)
        assertNull(fastingGoalDelayMillis(target, at(2026, Calendar.AUGUST, 25, 13)))
        assertNull(fastingGoalDelayMillis(target, target))
        assertNull(fastingGoalDelayMillis(null, target))
    }
}
