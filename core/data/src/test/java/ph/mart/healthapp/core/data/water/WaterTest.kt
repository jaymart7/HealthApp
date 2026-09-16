package ph.mart.healthapp.core.data.water

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import ph.mart.healthapp.core.data.profile.UnitSystem
import ph.mart.healthapp.core.data.progress.ChartRange

private const val TODAY = 20_000L

class WaterTest {

    @Test
    fun `metric stays in ml below a litre`() {
        assertEquals("0 ml", waterVolumeLabel(0, UnitSystem.Metric))
        assertEquals("750 ml", waterVolumeLabel(3, UnitSystem.Metric))
    }

    @Test
    fun `metric switches to litres at a litre`() {
        assertEquals("1.0 L", waterVolumeLabel(4, UnitSystem.Metric))
        assertEquals("2.0 L", waterVolumeLabel(8, UnitSystem.Metric))
    }

    @Test
    fun `imperial counts fluid ounces`() {
        assertEquals("0 fl oz", waterVolumeLabel(0, UnitSystem.Imperial))
        assertEquals("64 fl oz", waterVolumeLabel(8, UnitSystem.Imperial))
    }

    /** Null, never zero — the stat row draws an em dash rather than claiming a dry week. */
    @Test
    fun `an empty window reports nothing rather than zero`() {
        val averages = emptyList<WaterDay>().waterAverages(goalGlasses = 8)
        assertNull(averages.averageGlasses)
        assertNull(averages.bestGlasses)
        assertEquals(0, averages.daysHitGoal)
        assertEquals(0, averages.daysLogged)
    }

    /**
     * **The denominator is days with a row**, never calendar days in the window. Three logged days
     * across a fortnight average over three, not fourteen: a day nobody logged is a gap, not a day
     * they drank nothing.
     */
    @Test
    fun `the average divides by days logged, not days elapsed`() {
        val days = listOf(
            WaterDay(TODAY - 13, 9),
            WaterDay(TODAY - 6, 6),
            WaterDay(TODAY, 3),
        )
        val averages = days.waterAverages(goalGlasses = 8)
        assertEquals(6.0, averages.averageGlasses!!, 0.001)
        assertEquals(3, averages.daysLogged)
        assertEquals(9, averages.bestGlasses)
        assertEquals(1, averages.daysHitGoal)
    }

    /** A day exactly on the goal counts as hit, not missed. */
    @Test
    fun `hitting the goal on the nose counts`() {
        assertEquals(1, listOf(WaterDay(TODAY, 8)).waterAverages(goalGlasses = 8).daysHitGoal)
    }

    /** Anchored to today, not to the newest row — a window headed "1M" is the last 30 days even
     * when the last glass was logged in the spring. */
    @Test
    fun `the window is anchored to today`() {
        val days = listOf(
            WaterDay(TODAY - 200, 8),
            WaterDay(TODAY - 29, 7),
            WaterDay(TODAY, 6),
        )
        assertEquals(listOf(7, 6), days.inRange(ChartRange.OneMonth, TODAY).map { it.glasses })
        assertEquals(3, days.inRange(ChartRange.OneYear, TODAY).size)
    }
}
