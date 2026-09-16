package ph.mart.healthapp.core.data.food

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import ph.mart.healthapp.core.data.exercise.weekStart
import ph.mart.healthapp.core.data.health.BurnDay
import ph.mart.healthapp.core.data.profile.DailyTargets
import ph.mart.healthapp.core.data.todayEpochDay

private val TARGETS = DailyTargets(calories = 2000, proteinG = 150, carbsG = 200, fatG = 67, floor = 1500)

/** The Monday of the week the machine is in — everything below is offset from it, for
 * `TrainingPlanTest`'s reason: which weekday a fixed epoch day falls on depends on the time zone,
 * but "four days after this week's Monday is a Friday" holds everywhere. */
private val MONDAY = weekStart(todayEpochDay())

private fun day(epochDay: Long, calories: Int) =
    DayNutrition(dateEpochDay = epochDay, calories = calories, proteinG = 0, carbsG = 0, fatG = 0)

private fun burn(epochDay: Long, kcal: Int) =
    BurnDay(dateEpochDay = epochDay, burnedKcal = kcal, workouts = 1, minutes = 40)

private fun budget(
    nutrition: List<DayNutrition>,
    burn: List<BurnDay> = emptyList(),
    addExercise: Boolean = true,
    today: Long,
) = weekBudget(nutrition, burn, TARGETS, addExercise, today)

class WeekBudgetTest {

    @Test
    fun `a day nobody logged is skipped rather than banked`() {
        val week = budget(
            nutrition = listOf(day(MONDAY, 1500), day(MONDAY + 1, 0), day(MONDAY + 2, 2000)),
            today = MONDAY + 3,
        )
        // Tuesday contributes nothing at all — not 2,000 of credit for a day that never happened.
        assertEquals(500, week.bankedKcal)
        assertEquals(2, week.daysCounted)
        assertEquals(3, week.daysClosed)
    }

    @Test
    fun `today is not in the bank however much it holds`() {
        val closed = listOf(day(MONDAY, 1500))
        val withToday = budget(nutrition = closed + day(MONDAY + 3, 3000), today = MONDAY + 3)
        assertEquals(budget(nutrition = closed, today = MONDAY + 3), withToday)
        assertEquals(500, withToday.bankedKcal)
    }

    @Test
    fun `burn raises a past day's budget only with the switch on`() {
        val nutrition = listOf(day(MONDAY, 1500))
        val burn = listOf(burn(MONDAY, 300))
        assertEquals(800, budget(nutrition, burn, addExercise = true, today = MONDAY + 3).bankedKcal)
        assertEquals(500, budget(nutrition, burn, addExercise = false, today = MONDAY + 3).bankedKcal)
    }

    @Test
    fun `a fresh Monday has nothing behind it and spends the plain target`() {
        val week = budget(nutrition = listOf(day(MONDAY, 800)), today = MONDAY)
        assertEquals(0, week.bankedKcal)
        assertEquals(0, week.daysClosed)
        assertEquals(7, week.daysLeft)
        assertEquals(TARGETS.calories, week.perDayKcal)
        assertTrue(week.isEmpty)
        assertFalse(week.belowFloor)
    }

    @Test
    fun `an over-eaten week banks a negative`() {
        val week = budget(nutrition = listOf(day(MONDAY, 3000), day(MONDAY + 1, 3000)), today = MONDAY + 2)
        assertEquals(-2000, week.bankedKcal)
        assertEquals(5, week.daysLeft)
        assertEquals(1600, week.perDayKcal)
    }

    @Test
    fun `the even figure stops at the floor and says so`() {
        // Sunday: one day left to repay 1,000 kcal, which the arithmetic puts under the floor.
        val week = budget(nutrition = listOf(day(MONDAY, 3000)), today = MONDAY + 6)
        assertEquals(-1000, week.bankedKcal)
        assertEquals(1, week.daysLeft)
        assertEquals(TARGETS.floor, week.perDayKcal)
        assertTrue(week.belowFloor)
    }
}
