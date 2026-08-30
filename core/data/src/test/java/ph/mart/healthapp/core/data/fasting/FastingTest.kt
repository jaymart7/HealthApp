package ph.mart.healthapp.core.data.fasting

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import ph.mart.healthapp.core.data.epochDayOf
import ph.mart.healthapp.core.data.epochDayStartMillis
import ph.mart.healthapp.core.data.progress.ChartRange
import ph.mart.healthapp.core.data.todayEpochDay

private const val HOUR = 3_600_000L

class FastingTest {

    private fun fast(startMillis: Long, hours: Int? = null, goalHours: Int = 16) = FastSession(
        startMillis = startMillis,
        endMillis = hours?.let { startMillis + it * HOUR },
        goalHours = goalHours,
    )

    @Test
    fun `a running fast measures against now, a finished one against its end`() {
        val start = 1_700_000_000_000L
        val running = fast(start)
        assertTrue(running.isActive)
        assertEquals(14 * 60, running.durationMinutes(nowMillis = start + 14 * HOUR))

        val finished = fast(start, hours = 16)
        assertFalse(finished.isActive)
        // `now` has moved on by a day; a finished fast must not have grown with it.
        assertEquals(16 * 60, finished.durationMinutes(nowMillis = start + 40 * HOUR))
    }

    @Test
    fun `goal is reached at start plus the snapshotted hours, not the profile's current one`() {
        val start = 1_700_000_000_000L
        val session = fast(start, goalHours = 18)
        assertEquals(start + 18 * HOUR, session.goalReachedMillis)
        assertFalse(session.reachedGoal(nowMillis = start + 17 * HOUR))
        assertTrue(session.reachedGoal(nowMillis = start + 18 * HOUR))
    }

    /** The reason a fast is a session rather than a day: it starts one evening and ends the next. */
    @Test
    fun `a fast that crosses midnight is dated by the day it ended`() {
        val today = todayEpochDay()
        // 20:00 yesterday to 12:00 today.
        val start = epochDayStartMillis(today - 1) + 20 * HOUR
        val session = fast(start, hours = 16)
        assertEquals(today, session.dateEpochDay)
        assertEquals(today - 1, epochDayOf(start))
    }

    /** A running fast has no end, so it is dated by its start — otherwise it would have no slot
     * on any chart at all. */
    @Test
    fun `a running fast is dated by its start`() {
        val today = todayEpochDay()
        assertEquals(today, fast(epochDayStartMillis(today) + 9 * HOUR).dateEpochDay)
    }

    @Test
    fun `range is anchored to today, not to the newest fast`() {
        val today = todayEpochDay()
        // Nothing logged for four months; a 1M window comes back empty rather than re-centring
        // itself on those fasts the way the weight chart's inRange does.
        val stale = listOf(
            fast(epochDayStartMillis(today - 120), hours = 16),
            fast(epochDayStartMillis(today - 100), hours = 15),
        )
        assertEquals(emptyList<FastSession>(), stale.inRange(ChartRange.OneMonth, today))
    }

    @Test
    fun `range boundary is inclusive and 1Y keeps everything`() {
        val today = todayEpochDay()
        val sessions = listOf(31, 30, 0).map { back ->
            fast(epochDayStartMillis(today - back) + 2 * HOUR, hours = 4)
        }
        assertEquals(2, sessions.inRange(ChartRange.OneMonth, today).size)
        assertEquals(3, sessions.inRange(ChartRange.OneYear, today).size)
    }

    @Test
    fun `averages report the window's mean, longest and goals hit`() {
        val start = 1_700_000_000_000L
        val sessions = listOf(
            fast(start, hours = 15, goalHours = 16),
            fast(start, hours = 17, goalHours = 16),
            fast(start, hours = 19, goalHours = 16),
        )
        val averages = sessions.fastingAverages()
        assertEquals(17 * 60, averages.averageMinutes)
        assertEquals(19 * 60, averages.longestMinutes)
        assertEquals(2, averages.goalsHit)
        assertEquals(3, averages.count)
    }

    /** Each fast is judged against the goal it was started under, so raising the target later
     * cannot un-hit one already recorded. */
    @Test
    fun `goals hit uses each fast's own snapshotted target`() {
        val start = 1_700_000_000_000L
        val sessions = listOf(
            fast(start, hours = 14, goalHours = 12),
            fast(start, hours = 14, goalHours = 20),
        )
        assertEquals(1, sessions.fastingAverages().goalsHit)
    }

    @Test
    fun `an empty window averages to nothing rather than to zero`() {
        val averages = emptyList<FastSession>().fastingAverages()
        assertNull(averages.averageMinutes)
        assertNull(averages.longestMinutes)
        assertEquals(0, averages.goalsHit)
        assertEquals(0, averages.count)
    }

    @Test
    fun `elapsed formats without a leading zero hour`() {
        assertEquals("14h 20m", formatElapsed(14 * HOUR + 20 * 60_000L))
        assertEquals("45m", formatElapsed(45 * 60_000L))
    }
}
