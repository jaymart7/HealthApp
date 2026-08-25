package ph.mart.healthapp.feature.home.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import ph.mart.healthapp.core.data.food.DiaryTotals
import ph.mart.healthapp.core.data.profile.DailyTargets
import ph.mart.healthapp.core.data.progress.WeightEntry

private val TARGETS = DailyTargets(calories = 2000, proteinG = 150, carbsG = 200, fatG = 67, floor = 1500)

class HomeDataTest {

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

    @Test
    fun `days since photo is null with no photos and never negative`() {
        assertNull(daysSincePhoto(null, todayEpochDay = 100))
        assertEquals(12L, daysSincePhoto(88, todayEpochDay = 100))
        assertEquals(0L, daysSincePhoto(105, todayEpochDay = 100))
    }

    @Test
    fun `insight prefers the calorie overage, then protein, then weight`() {
        val flatTrend = WeightTrendDisplay(currentKg = 76.0, deltaKg = 0.0, hasPrior = false)
        assertEquals(
            "You're 200 kcal over today's target.",
            insightFor(DiaryTotals(2200, 150, 200, 67), TARGETS, flatTrend),
        )
        assertEquals(
            "You're 70g short on protein today.",
            insightFor(DiaryTotals(1000, 80, 100, 30), TARGETS, flatTrend),
        )
        // Exactly 60% of the protein goal is not "short" — the rule is strictly below.
        assertNull(insightFor(DiaryTotals(1000, 90, 100, 30), TARGETS, flatTrend))
        assertEquals(
            "-0.6 kg over the last week — keep it steady.",
            insightFor(
                DiaryTotals(1000, 140, 100, 30),
                TARGETS,
                WeightTrendDisplay(currentKg = 76.0, deltaKg = -0.6, hasPrior = true),
            ),
        )
    }

    @Test
    fun `insight is null when nothing is notable`() {
        assertNull(
            insightFor(
                DiaryTotals(1000, 140, 100, 30),
                TARGETS,
                WeightTrendDisplay(currentKg = 76.0, deltaKg = -0.1, hasPrior = true),
            ),
        )
    }

    @Test
    fun `empty diary does not trigger the protein insight`() {
        val trend = WeightTrendDisplay(currentKg = 76.0, deltaKg = 0.0, hasPrior = false)
        assertNull(insightFor(DiaryTotals(0, 0, 0, 0), TARGETS, trend))
    }

    @Test
    fun `greeting matches the prototype copy for each part of the day`() {
        assertEquals("Good morning! Ready for breakfast?", greetingFor(8))
        assertEquals("Good afternoon! How's the day going?", greetingFor(12))
        assertEquals("Good evening! Almost there for today.", greetingFor(18))
    }
}
