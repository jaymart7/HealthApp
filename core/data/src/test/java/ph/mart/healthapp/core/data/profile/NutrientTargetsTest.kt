package ph.mart.healthapp.core.data.profile

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The bands are the whole of this function, and getting one wrong misreports a target by more than
 * double — iron for a woman of 50 versus 51 is 18 mg against 8 mg. Every boundary is pinned.
 */
class NutrientTargetsTest {

    private fun profile(sex: Sex, age: Int) = Profile(
        sex = sex,
        age = age,
        heightCm = 170.0,
        weightKg = 70.0,
        activityLevel = ActivityLevel.Moderate,
        goal = Goal.Maintain,
    )

    private fun targetsAt(calories: Int) =
        DailyTargets(calories = calories, proteinG = 0, carbsG = 0, fatG = 0, floor = 1200)

    @Test
    fun `fiber and sugar ride the calorie target`() {
        val p = profile(Sex.Male, 30)

        // 14 g per 1000 kcal, and free sugars under 10% of energy at 4 kcal a gram.
        assertEquals(28, nutrientTargets(p, targetsAt(2000)).fiberG)
        assertEquals(50, nutrientTargets(p, targetsAt(2000)).sugarG)

        // This is the point of deriving them: an edited calorie target moves them with it.
        assertEquals(42, nutrientTargets(p, targetsAt(3000)).fiberG)
        assertEquals(75, nutrientTargets(p, targetsAt(3000)).sugarG)
    }

    @Test
    fun `sodium is one figure for every adult`() {
        assertEquals(2300, nutrientTargets(profile(Sex.Male, 19), targetsAt(2000)).sodiumMg)
        assertEquals(2300, nutrientTargets(profile(Sex.Female, 80), targetsAt(2000)).sodiumMg)
    }

    @Test
    fun `iron drops for a woman past fifty and never moves for a man`() {
        assertEquals(18_000, nutrientTargets(profile(Sex.Female, 50), targetsAt(2000)).ironUg)
        assertEquals(8_000, nutrientTargets(profile(Sex.Female, 51), targetsAt(2000)).ironUg)
        assertEquals(8_000, nutrientTargets(profile(Sex.Male, 30), targetsAt(2000)).ironUg)
    }

    @Test
    fun `calcium rises at fifty-one for women and seventy-one for men`() {
        assertEquals(1000, nutrientTargets(profile(Sex.Female, 50), targetsAt(2000)).calciumMg)
        assertEquals(1200, nutrientTargets(profile(Sex.Female, 51), targetsAt(2000)).calciumMg)
        assertEquals(1000, nutrientTargets(profile(Sex.Male, 70), targetsAt(2000)).calciumMg)
        assertEquals(1200, nutrientTargets(profile(Sex.Male, 71), targetsAt(2000)).calciumMg)
    }

    @Test
    fun `vitamin D rises at seventy-one for both`() {
        assertEquals(15, nutrientTargets(profile(Sex.Female, 70), targetsAt(2000)).vitaminDUg)
        assertEquals(20, nutrientTargets(profile(Sex.Female, 71), targetsAt(2000)).vitaminDUg)
        assertEquals(20, nutrientTargets(profile(Sex.Male, 71), targetsAt(2000)).vitaminDUg)
    }

    @Test
    fun `potassium differs by sex alone`() {
        assertEquals(3400, nutrientTargets(profile(Sex.Male, 30), targetsAt(2000)).potassiumMg)
        assertEquals(2600, nutrientTargets(profile(Sex.Female, 30), targetsAt(2000)).potassiumMg)
    }
}
