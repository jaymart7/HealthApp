package ph.mart.healthapp.core.data.health

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import ph.mart.healthapp.core.data.progress.ChartRange

class SleepTest {

    @Test
    fun `averages report the window's mean and longest night`() {
        val nights = listOf(SleepNight(1, 420), SleepNight(2, 480), SleepNight(3, 390))
        val averages = nights.sleepAverages()
        assertEquals(430, averages.averageMinutes)
        assertEquals(480, averages.longestMinutes)
        assertEquals(3, averages.nights)
    }

    @Test
    fun `an empty window averages to nothing rather than to zero`() {
        val averages = emptyList<SleepNight>().sleepAverages()
        assertNull(averages.averageMinutes)
        assertNull(averages.longestMinutes)
        assertEquals(0, averages.nights)
    }

    @Test
    fun `range is anchored to today, not to the newest night`() {
        val today = 20_000L
        // The watch last synced 100 days ago; a 1M window must come back empty rather than
        // re-centring itself on that night the way the weight chart's inRange does.
        val stale = listOf(SleepNight(today - 120, 400), SleepNight(today - 100, 430))
        assertEquals(emptyList<SleepNight>(), stale.inRange(ChartRange.OneMonth, today))
    }

    @Test
    fun `range boundary is inclusive and 1Y keeps everything`() {
        val today = 20_000L
        val nights = listOf(SleepNight(today - 31, 300), SleepNight(today - 30, 400), SleepNight(today, 500))
        assertEquals(listOf(400, 500), nights.inRange(ChartRange.OneMonth, today).map { it.minutesAsleep })
        assertEquals(3, nights.inRange(ChartRange.OneYear, today).size)
    }

    @Test
    fun `duration formats without a leading zero hour`() {
        assertEquals("7h 12m", formatDuration(432))
        assertEquals("45m", formatDuration(45))
        assertEquals(SleepNight(1, 432).formatDuration(), formatDuration(432))
    }
}
