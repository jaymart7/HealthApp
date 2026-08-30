package ph.mart.healthapp.core.data.profile

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DailyTargetsTest {

    private fun profile(
        sex: Sex,
        age: Int = 30,
        heightCm: Double = 170.0,
        weightKg: Double = 70.0,
        activityLevel: ActivityLevel = ActivityLevel.Sedentary,
        goal: Goal = Goal.Maintain,
    ) = Profile(
        sex = sex,
        age = age,
        heightCm = heightCm,
        weightKg = weightKg,
        activityLevel = activityLevel,
        goal = goal,
    )

    @Test
    fun `male formula matches Mifflin-St Jeor`() {
        // BMR = 10*70 + 6.25*170 - 5*30 + 5 = 1617.5 -> TDEE (sedentary x1.2) = 1941.0 -> round
        val targets = calculateDailyTargets(profile(sex = Sex.Male))
        assertEquals(1941, targets.calories)
        assertEquals(MALE_CALORIE_FLOOR, targets.floor)
    }

    @Test
    fun `female formula matches Mifflin-St Jeor`() {
        // BMR = 10*70 + 6.25*170 - 5*30 - 161 = 1451.5 -> TDEE (sedentary x1.2) = 1741.8 -> round 1742
        val targets = calculateDailyTargets(profile(sex = Sex.Female))
        assertEquals(1742, targets.calories)
        assertEquals(FEMALE_CALORIE_FLOOR, targets.floor)
    }

    @Test
    fun `male calories are clamped to the 1500 kcal floor`() {
        val targets = calculateDailyTargets(
            profile(sex = Sex.Male, age = 80, heightCm = 150.0, weightKg = 45.0, goal = Goal.Lose),
        )
        assertEquals(MALE_CALORIE_FLOOR, targets.calories)
    }

    @Test
    fun `female calories are clamped to the 1200 kcal floor`() {
        val targets = calculateDailyTargets(
            profile(sex = Sex.Female, age = 80, heightCm = 150.0, weightKg = 45.0, goal = Goal.Lose),
        )
        assertEquals(FEMALE_CALORIE_FLOOR, targets.calories)
    }

    @Test
    fun `macro split is 30-40-30 at 4-4-9 kcal per gram`() {
        val targets = calculateDailyTargets(profile(sex = Sex.Male, activityLevel = ActivityLevel.Very, goal = Goal.Build))
        val macroCalories = targets.proteinG * 4 + targets.carbsG * 4 + targets.fatG * 9
        assertTrue(Math.abs(macroCalories - targets.calories) <= 3)
    }

    @Test
    fun `dailyTargets applies manual overrides on top of the computed value`() {
        val base = profile(sex = Sex.Male)
        val overridden = base.copy(calorieOverrideKcal = 2000, proteinOverrideG = 150)
        val targets = overridden.dailyTargets()
        assertEquals(2000, targets.calories)
        assertEquals(150, targets.proteinG)
        // Carbs carry no override, so they are the 40% share of the *manual* 2000 kcal.
        assertEquals(200, targets.carbsG)
    }

    @Test
    fun `a manual calorie target reprices the whole macro split`() {
        val targets = profile(sex = Sex.Male).copy(calorieOverrideKcal = 2000).dailyTargets()
        val macroCalories = targets.proteinG * 4 + targets.carbsG * 4 + targets.fatG * 9
        assertTrue(Math.abs(macroCalories - 2000) <= 3)
    }

    @Test
    fun `dailyTargets without overrides is the computed value`() {
        val base = profile(sex = Sex.Female)
        assertEquals(calculateDailyTargets(base), base.dailyTargets())
    }

    @Test
    fun `belowFloor only trips under the safety floor`() {
        val base = profile(sex = Sex.Male)
        assertTrue(base.copy(calorieOverrideKcal = MALE_CALORIE_FLOOR - 1).dailyTargets().belowFloor)
        assertFalse(base.dailyTargets().belowFloor)
    }
}
