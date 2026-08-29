package ph.mart.healthapp.widget

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import ph.mart.healthapp.core.data.food.DiaryTotals
import ph.mart.healthapp.core.data.profile.ActivityLevel
import ph.mart.healthapp.core.data.profile.Goal
import ph.mart.healthapp.core.data.profile.Profile
import ph.mart.healthapp.core.data.profile.Sex
import ph.mart.healthapp.core.data.profile.dailyTargets

/**
 * The widget's arithmetic, which is the only part of it that can be checked off-device — a Glance
 * composable needs a real app-widget host.
 */
class TodayWidgetStateTest {

    private val profile = Profile(
        sex = Sex.Male,
        age = 30,
        heightCm = 180.0,
        weightKg = 80.0,
        activityLevel = ActivityLevel.Moderate,
        goal = Goal.Lose,
        // Fixed so the assertions below don't ride on the Mifflin–St Jeor result changing.
        calorieOverrideKcal = 2000,
        waterGoalGlasses = 8,
    )

    private fun stateFor(
        consumed: Int = 0,
        glasses: Int = 0,
        burned: Int = 0,
        addExercise: Boolean = true,
    ) = todayWidgetState(
        profile = profile.copy(addExerciseToBudget = addExercise),
        totals = DiaryTotals(consumed, 0, 0, 0),
        glasses = glasses,
        burnedKcal = burned,
        streakDays = 3,
    )

    @Test
    fun `exercise raises the budget when the credit is on`() {
        assertEquals(2320, stateFor(burned = 320).budgetKcal)
    }

    @Test
    fun `exercise leaves the budget alone when the credit is off`() {
        assertEquals(2000, stateFor(burned = 320, addExercise = false).budgetKcal)
    }

    @Test
    fun `the budget is the profile's own target, never a second copy`() {
        assertEquals(profile.dailyTargets().calories, stateFor().budgetKcal)
    }

    @Test
    fun `over budget reports a negative remainder but a full bar`() {
        val state = stateFor(consumed = 2400)
        assertEquals(-400, state.remainingKcal)
        assertEquals(1f, state.progress, 0f)
    }

    @Test
    fun `a missing profile yields the onboarding state rather than dividing by zero`() {
        val state = todayWidgetState(null, DiaryTotals(0, 0, 0, 0), 0, 0, 0)
        assertTrue(state.onboarding)
        assertEquals(0f, state.progress, 0f)
    }

    @Test
    fun `adding a glass stops at the goal`() {
        assertEquals(6, stateFor(glasses = 5).glassesAfterAdd)
        assertFalse(stateFor(glasses = 5).waterGoalReached)

        val atGoal = stateFor(glasses = 8)
        assertEquals(8, atGoal.glassesAfterAdd)
        assertTrue(atGoal.waterGoalReached)
    }
}
