package ph.mart.healthapp.core.data.health

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import ph.mart.healthapp.core.data.progress.ChartRange

class HeartTest {

    @Test
    fun `averages report the window's mean and its lowest reading`() {
        val days = listOf(HeartDay(1, 68, 52), HeartDay(2, 71, 55), HeartDay(3, 66, 49))
        val averages = days.heartAverages()
        assertEquals(68, averages.averageBpm)
        assertEquals(49, averages.lowestBpm)
        assertEquals(3, averages.days)
    }

    @Test
    fun `the mean is a mean of the days, not of the samples`() {
        // Whatever the watch did, a day is a day: one reading of 90 weighs exactly as much as a
        // day of a hundred readings averaging 60. The samples are long gone by the time this runs.
        val days = listOf(HeartDay(1, 60, 60), HeartDay(2, 90, 90))
        assertEquals(75, days.heartAverages().averageBpm)
    }

    @Test
    fun `an empty window averages to nothing rather than to zero`() {
        val averages = emptyList<HeartDay>().heartAverages()
        assertNull(averages.averageBpm)
        assertNull(averages.lowestBpm)
        assertEquals(0, averages.days)
    }

    @Test
    fun `range is anchored to today, not to the newest reading`() {
        val today = 20_000L
        // The watch last synced 100 days ago; a 1M window must come back empty rather than
        // re-centring itself on that day the way the weight chart's inRange does.
        val stale = listOf(HeartDay(today - 120, 70, 54), HeartDay(today - 100, 68, 52))
        assertEquals(emptyList<HeartDay>(), stale.inRange(ChartRange.OneMonth, today))
    }

    @Test
    fun `range boundary is inclusive and 1Y keeps everything`() {
        val today = 20_000L
        val days = listOf(HeartDay(today - 31, 60, 50), HeartDay(today - 30, 70, 55), HeartDay(today, 80, 60))
        assertEquals(listOf(70, 80), days.inRange(ChartRange.OneMonth, today).map { it.averageBpm })
        assertEquals(3, days.inRange(ChartRange.OneYear, today).size)
    }

    @Test
    fun `bpm formats the one way the UI renders it`() {
        assertEquals("68 bpm", formatBpm(68))
    }
}
