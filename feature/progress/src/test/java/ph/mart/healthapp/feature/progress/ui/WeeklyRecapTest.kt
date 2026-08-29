package ph.mart.healthapp.feature.progress.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import ph.mart.healthapp.core.data.food.DayNutrition
import ph.mart.healthapp.core.data.mood.MoodDay
import ph.mart.healthapp.core.data.profile.DailyTargets
import ph.mart.healthapp.core.data.progress.WeightEntry

private const val TODAY = 20_000L

private val TARGETS = DailyTargets(calories = 2000, proteinG = 150, carbsG = 200, fatG = 67, floor = 1500)

/** Dense week ending today, zero-filled where nothing was eaten — the shape the repository emits. */
private fun week(vararg calories: Int): List<DayNutrition> =
    calories.mapIndexed { index, kcal ->
        val day = TODAY - (calories.size - 1 - index)
        DayNutrition(day, kcal, kcal / 16, kcal / 10, kcal / 30)
    }

private fun recap(
    nutrition: List<DayNutrition> = week(0, 0, 0, 0, 0, 0, 0),
    activeDays: Set<Long> = emptySet(),
    weightEntries: List<WeightEntry> = emptyList(),
    moodDays: List<MoodDay> = emptyList(),
    targets: DailyTargets? = TARGETS,
) = weeklyRecap(nutrition, activeDays, weightEntries, moodDays, targets, todayEpochDay = TODAY)

class WeeklyRecapTest {

    @Test
    fun `nothing logged in the window means no card at all`() {
        // Logged, but a fortnight ago — outside the window.
        assertNull(recap(activeDays = setOf(TODAY - 20, TODAY - 14)))
    }

    @Test
    fun `days logged counts every domain, calories average only food days`() {
        val result = recap(
            nutrition = week(0, 1800, 0, 0, 2200, 0, 0),
            // Six active days: two with food, four water- or exercise-only.
            activeDays = (TODAY - 5..TODAY).toSet(),
        )
        assertNotNull(result)
        assertEquals(6, result!!.daysLogged)
        assertEquals(2, result.averages.daysLogged)
        // The four zero-filled gaps must not drag the mean down.
        assertEquals(2000, result.averages.calories)
    }

    @Test
    fun `activity outside the window is not counted`() {
        val result = recap(activeDays = setOf(TODAY - 7, TODAY - 6, TODAY))
        assertEquals(2, result!!.daysLogged)
    }

    @Test
    fun `best day is the closest to target, ties breaking to the more recent day`() {
        val result = recap(
            nutrition = week(0, 1900, 0, 0, 2100, 0, 1500),
            activeDays = (TODAY - 5..TODAY).toSet(),
        )
        // 1900 and 2100 are both 100 off; the later day wins.
        assertEquals(TODAY - 2, result!!.bestDay?.dateEpochDay)
        assertEquals(2100, result.bestDay?.calories)
    }

    @Test
    fun `no targets means no best day`() {
        val result = recap(
            nutrition = week(0, 1900, 0, 0, 0, 0, 0),
            activeDays = setOf(TODAY),
            targets = null,
        )
        assertNull(result!!.bestDay)
    }

    @Test
    fun `a weigh-in inside the window reports the delta`() {
        val result = recap(
            activeDays = setOf(TODAY),
            weightEntries = listOf(
                WeightEntry(dateEpochDay = TODAY - 10, weightKg = 78.0),
                WeightEntry(dateEpochDay = TODAY - 1, weightKg = 76.8),
            ),
        )
        assertEquals(true, result!!.weightTrend?.hasPrior)
        assertEquals(-1.2, result.weightTrend!!.deltaKg, 0.001)
    }

    @Test
    fun `a stale weigh-in reports nothing rather than an old delta`() {
        val result = recap(
            activeDays = setOf(TODAY),
            weightEntries = listOf(
                WeightEntry(dateEpochDay = TODAY - 60, weightKg = 82.0),
                WeightEntry(dateEpochDay = TODAY - 30, weightKg = 79.0),
            ),
        )
        assertNull(result!!.weightTrend)
    }

    @Test
    fun `mood from before the window is not averaged into it`() {
        val result = recap(
            activeDays = (TODAY - 2..TODAY).toSet(),
            moodDays = listOf(MoodDay(TODAY - 30, mood = 1, energy = 1)),
        )
        assertNull(result!!.moodAverages)
    }

    @Test
    fun `mood inside the window is averaged, energy keeping its own denominator`() {
        val result = recap(
            activeDays = (TODAY - 2..TODAY).toSet(),
            moodDays = listOf(
                MoodDay(TODAY - 30, mood = 1, energy = 1),
                MoodDay(TODAY - 2, mood = 4, energy = 0),
                MoodDay(TODAY, mood = 2, energy = 3),
            ),
        )
        val averages = result!!.moodAverages!!
        assertEquals(3.0, averages.mood!!, 0.001)
        assertEquals(3.0, averages.energy!!, 0.001)
        assertEquals(2, averages.daysLogged)
    }
}
