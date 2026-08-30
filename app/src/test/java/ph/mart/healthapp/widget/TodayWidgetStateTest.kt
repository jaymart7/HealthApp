package ph.mart.healthapp.widget

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import ph.mart.healthapp.core.data.exercise.ExerciseEntry
import ph.mart.healthapp.core.data.exercise.ExerciseType
import ph.mart.healthapp.core.data.fasting.FastSession
import ph.mart.healthapp.core.data.food.DiaryTotals
import ph.mart.healthapp.core.data.health.StepDay
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
        steps: StepDay? = null,
        addExercise: Boolean = true,
        activeFast: FastSession? = null,
        nowMillis: Long = NOW,
    ) = todayWidgetState(
        profile = profile.copy(addExerciseToBudget = addExercise),
        totals = DiaryTotals(consumed, 0, 0, 0),
        glasses = glasses,
        exercise = if (burned == 0) {
            emptyList()
        } else {
            listOf(ExerciseEntry(type = ExerciseType.Strength, minutes = 40, burnedKcal = burned))
        },
        steps = steps,
        streakDays = 3,
        activeFast = activeFast,
        nowMillis = nowMillis,
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
    fun `steps raise the budget, and the count reaches the widget`() {
        val state = stateFor(steps = StepDay(dateEpochDay = 20_000, steps = 8432, burnedKcal = 300))
        assertEquals(8432, state.steps)
        assertEquals(2300, state.budgetKcal)
    }

    @Test
    fun `steps leave the budget alone when the credit is off`() {
        val state = stateFor(
            steps = StepDay(dateEpochDay = 20_000, steps = 8432, burnedKcal = 300),
            addExercise = false,
        )
        // Still shown — the switch governs the budget, not whether the day is reported.
        assertEquals(8432, state.steps)
        assertEquals(2000, state.budgetKcal)
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
        val state = todayWidgetState(null, DiaryTotals(0, 0, 0, 0), 0, emptyList(), null, 0)
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

    @Test
    fun `no running fast leaves the line out rather than reporting a zero`() {
        val state = stateFor()
        assertNull(state.fastingUntilMillis)
        assertFalse(state.fastingGoalReached)
    }

    /** A target *time*, not an elapsed duration: Glance can't tick, so an elapsed figure would go
     * stale between the widget's half-hourly updates. */
    @Test
    fun `a running fast carries its target time, not its elapsed time`() {
        val start = NOW - 9 * HOUR
        val state = stateFor(activeFast = FastSession(startMillis = start, goalHours = 16))
        assertEquals(start + 16 * HOUR, state.fastingUntilMillis)
        assertFalse(state.fastingGoalReached)
    }

    @Test
    fun `a fast past its target reports it as reached`() {
        val state = stateFor(activeFast = FastSession(startMillis = NOW - 17 * HOUR, goalHours = 16))
        assertTrue(state.fastingGoalReached)
    }

    private companion object {
        const val HOUR = 3_600_000L
        const val NOW = 1_700_000_000_000L
    }
}
