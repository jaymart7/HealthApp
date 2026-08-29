package ph.mart.healthapp.core.data.streak

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import ph.mart.healthapp.core.data.food.DayNutrition
import ph.mart.healthapp.core.data.profile.Goal
import ph.mart.healthapp.core.data.progress.WeightEntry

private const val TODAY = 1000L

private fun ate(day: Long) = DayNutrition(dateEpochDay = day, calories = 500, proteinG = 30, carbsG = 50, fatG = 15)
private fun blank(day: Long) = DayNutrition(dateEpochDay = day, calories = 0, proteinG = 0, carbsG = 0, fatG = 0)

class LoggingStreakTest {

    @Test
    fun `a day with only water counts the same as a day with only food`() {
        val days = loggedDays(
            nutrition = listOf(ate(TODAY)),
            waterDays = setOf(TODAY - 1),
            weightEntries = emptyList(),
            exerciseDays = emptySet(),
        )
        assertEquals(setOf(TODAY, TODAY - 1), days)
        assertEquals(2, days.streakStats(TODAY).current)
    }

    @Test
    fun `a weigh-in alone keeps the streak alive`() {
        val days = loggedDays(
            nutrition = listOf(ate(TODAY), blank(TODAY - 1)),
            waterDays = emptySet(),
            weightEntries = listOf(WeightEntry(dateEpochDay = TODAY - 1, weightKg = 80.0)),
            exerciseDays = emptySet(),
        )
        assertEquals(2, days.streakStats(TODAY).current)
    }

    @Test
    fun `a gym-only day keeps the streak alive`() {
        val days = loggedDays(
            nutrition = listOf(ate(TODAY), blank(TODAY - 1)),
            waterDays = emptySet(),
            weightEntries = emptyList(),
            exerciseDays = setOf(TODAY - 1),
        )
        assertEquals(2, days.streakStats(TODAY).current)
    }

    @Test
    fun `a zero-filled nutrition day does not count as logged`() {
        val days = loggedDays(
            nutrition = listOf(ate(TODAY), blank(TODAY - 1), ate(TODAY - 2)),
            waterDays = emptySet(),
            weightEntries = emptyList(),
            exerciseDays = emptySet(),
        )
        assertEquals(setOf(TODAY, TODAY - 2), days)
        assertEquals(1, days.streakStats(TODAY).current)
    }

    @Test
    fun `today still empty counts back from yesterday rather than resetting to zero`() {
        val days = setOf(TODAY - 1, TODAY - 2, TODAY - 3)
        val stats = days.streakStats(TODAY)
        assertEquals(3, stats.current)
        assertEquals(3, stats.best)
    }

    @Test
    fun `two empty days in a row do break the streak`() {
        val stats = setOf(TODAY - 2, TODAY - 3, TODAY - 4).streakStats(TODAY)
        assertEquals(0, stats.current)
        assertEquals(3, stats.best)
    }

    @Test
    fun `best survives a broken streak so badges stay earned`() {
        // A 9-day run long ago, a 2-day run now.
        val days = ((TODAY - 30)..(TODAY - 22)).toSet() + setOf(TODAY, TODAY - 1)
        val stats = days.streakStats(TODAY)
        assertEquals(2, stats.current)
        assertEquals(9, stats.best)
        assertEquals(11, stats.totalDaysLogged)
        assertEquals(setOf(StreakBadge.Three, StreakBadge.Week), stats.earnedBadges())
        // The next badge is measured against the *current* run, not the best one.
        assertEquals(StreakBadge.Three, stats.nextBadge())
    }

    @Test
    fun `nothing logged is a zero streak, not a crash`() {
        assertEquals(StreakStats(0, 0, 0), emptySet<Long>().streakStats(TODAY))
        assertTrue(emptySet<Long>().streakStats(TODAY).earnedBadges().isEmpty())
    }

    @Test
    fun `nextBadge is null once the current run has passed them all`() {
        assertNull(StreakStats(current = 120, best = 120, totalDaysLogged = 120).nextBadge())
    }

    @Test
    fun `weight progress inverts with the goal and is null for maintain`() {
        val entries = listOf(
            WeightEntry(dateEpochDay = TODAY - 60, weightKg = 85.0),
            WeightEntry(dateEpochDay = TODAY, weightKg = 80.0),
        )
        assertEquals(5.0, weightProgressKg(entries, Goal.Lose, onboardingWeightKg = 85.0)!!, 0.001)
        assertEquals(-5.0, weightProgressKg(entries, Goal.Build, onboardingWeightKg = 85.0)!!, 0.001)
        assertNull(weightProgressKg(entries, Goal.Maintain, onboardingWeightKg = 85.0))
    }

    @Test
    fun `the start is the earliest entry by date, not by insertion order`() {
        val chronological = listOf(
            WeightEntry(dateEpochDay = TODAY - 60, weightKg = 85.0),
            WeightEntry(dateEpochDay = TODAY, weightKg = 80.0),
        )
        val backdated = listOf(
            WeightEntry(dateEpochDay = TODAY, weightKg = 80.0),
            WeightEntry(dateEpochDay = TODAY - 60, weightKg = 85.0),
        )
        assertEquals(
            weightProgressKg(chronological, Goal.Lose, 85.0),
            weightProgressKg(backdated, Goal.Lose, 85.0),
        )
    }

    @Test
    fun `a lone entry measures against the onboarding weight, not itself`() {
        val one = listOf(WeightEntry(dateEpochDay = TODAY, weightKg = 80.0))
        assertEquals(2.4, weightProgressKg(one, Goal.Lose, onboardingWeightKg = 82.4)!!, 0.001)
        assertNull(weightProgressKg(emptyList(), Goal.Lose, onboardingWeightKg = 82.4))
    }

    @Test
    fun `the earned weight badge is the highest threshold reached`() {
        assertNull(earnedWeightBadge(1.9))
        assertEquals(WeightBadge.Two, earnedWeightBadge(2.0))
        assertEquals(WeightBadge.Five, earnedWeightBadge(5.2))
        assertEquals(WeightBadge.Ten, earnedWeightBadge(14.0))
        assertNull(earnedWeightBadge(-3.0))
    }
}
