package ph.mart.healthapp.core.data

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * [todayFlow] is a timer, so there is one thing worth guarding: the wait it computes. A zero or
 * negative delay turns the loop into a busy-spin that emits forever and pins a core — and it is
 * computed by subtracting `now` from a `Calendar` walked forward a day, which is exactly the sort
 * of arithmetic that lands on the wrong side of the boundary.
 */
class EpochDayTest {

    @Test
    fun `the wait until the next local midnight is always in the future`() {
        val wait = epochDayStartMillis(todayEpochDay() + 1) - System.currentTimeMillis()
        assertTrue("expected a positive wait, was $wait", wait > 0)
        // A day, plus room for the DST transition that makes one 25 hours long.
        assertTrue("expected under 26h, was $wait", wait <= 26 * 60 * 60 * 1000L)
    }

    @Test
    fun `todayFlow emits today before it waits for anything`() = runBlocking {
        assertEquals(todayEpochDay(), todayFlow().first())
    }

    @Test
    fun `an epoch day round-trips through local midnight`() {
        val today = todayEpochDay()
        assertEquals(today, epochDayOf(epochDayStartMillis(today)))
        assertEquals(today + 1, epochDayOf(epochDayStartMillis(today + 1)))
    }
}
