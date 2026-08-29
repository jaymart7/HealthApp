package ph.mart.healthapp.feature.home.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import ph.mart.healthapp.core.data.food.DiaryTotals
import ph.mart.healthapp.core.data.profile.DailyTargets
import ph.mart.healthapp.core.data.profile.WeightTrendDisplay
import ph.mart.healthapp.core.data.progress.WeightEntry

private val TARGETS = DailyTargets(calories = 2000, proteinG = 150, carbsG = 200, fatG = 67, floor = 1500)

class HomeDataTest {

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
    fun `an unloaded state is Loading, never day one`() {
        // The regression this guards: HomeUiState's all-zero default is indistinguishable from a
        // genuinely empty day, so a user with months of history would see the day-one empty state
        // fade away on every cold start.
        assertEquals(HomePhase.Loading, homePhase(HomeUiState()))
        assertEquals(
            HomePhase.Loading,
            homePhase(HomeUiState(waterGlasses = 5, foodEntryCount = 3)),
        )
    }

    @Test
    fun `a loaded state is day one only when nothing has been logged`() {
        assertEquals(HomePhase.DayOne, homePhase(HomeUiState(loaded = true)))
        assertEquals(
            HomePhase.Populated,
            homePhase(HomeUiState(loaded = true, waterGlasses = 1)),
        )
        assertEquals(
            HomePhase.Populated,
            homePhase(HomeUiState(loaded = true, foodEntryCount = 1)),
        )
        assertEquals(
            HomePhase.Populated,
            homePhase(
                HomeUiState(
                    loaded = true,
                    weightEntries = listOf(WeightEntry(dateEpochDay = 100, weightKg = 76.0)),
                ),
            ),
        )
        assertEquals(
            HomePhase.Populated,
            homePhase(HomeUiState(loaded = true, burnedKcal = 200)),
        )
        assertEquals(
            HomePhase.Populated,
            homePhase(HomeUiState(loaded = true, lastPhotoEpochDay = 100)),
        )
        // Mood is deliberately not a day-one signal, same as it isn't a streak domain.
        assertEquals(
            HomePhase.DayOne,
            homePhase(HomeUiState(loaded = true, moodLevel = 4, energyLevel = 3)),
        )
    }

    @Test
    fun `greeting matches the prototype copy for each part of the day`() {
        assertEquals("Good morning! Ready for breakfast?", greetingFor(8))
        assertEquals("Good afternoon! How's the day going?", greetingFor(12))
        assertEquals("Good evening! Almost there for today.", greetingFor(18))
    }
}
