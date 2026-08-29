package ph.mart.healthapp.core.data.profile

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import ph.mart.healthapp.core.data.progress.WeightEntry

class WeightTrendTest {

    @Test
    fun `prior weight is the newest entry at least 7 days back, not the previous entry`() {
        val entries = listOf(
            WeightEntry(dateEpochDay = 0, weightKg = 80.0),
            WeightEntry(dateEpochDay = 3, weightKg = 79.0),
            WeightEntry(dateEpochDay = 9, weightKg = 78.0),
        )
        val trend = entries.trendVsSevenDaysAgo(fallbackKg = 0.0)
        assertTrue(trend.hasPrior)
        assertEquals(78.0, trend.currentKg, 0.001)
        // day 9 - 7 = day 2; the newest entry at or before that is day 0 (80.0), not day 3.
        assertEquals(-2.0, trend.deltaKg, 0.001)
    }

    @Test
    fun `backdated insert does not change the trend`() {
        val chronological = listOf(
            WeightEntry(dateEpochDay = 0, weightKg = 80.0),
            WeightEntry(dateEpochDay = 9, weightKg = 78.0),
        )
        val backdated = listOf(
            WeightEntry(dateEpochDay = 9, weightKg = 78.0),
            WeightEntry(dateEpochDay = 0, weightKg = 80.0),
        )
        assertEquals(
            chronological.trendVsSevenDaysAgo(fallbackKg = 0.0),
            backdated.trendVsSevenDaysAgo(fallbackKg = 0.0),
        )
    }

    @Test
    fun `no entry 7 days back reports no prior data rather than a zero delta`() {
        val entries = listOf(
            WeightEntry(dateEpochDay = 5, weightKg = 79.0),
            WeightEntry(dateEpochDay = 6, weightKg = 78.5),
        )
        val trend = entries.trendVsSevenDaysAgo(fallbackKg = 0.0)
        assertEquals(false, trend.hasPrior)
        assertEquals(78.5, trend.currentKg, 0.001)
    }

    @Test
    fun `empty series falls back to the profile weight`() {
        val trend = emptyList<WeightEntry>().trendVsSevenDaysAgo(fallbackKg = 76.5)
        assertEquals(76.5, trend.currentKg, 0.001)
        assertEquals(false, trend.hasPrior)
        assertEquals(0.0, trend.deltaKg, 0.001)
    }
}
