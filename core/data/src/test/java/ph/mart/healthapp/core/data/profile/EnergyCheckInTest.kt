package ph.mart.healthapp.core.data.profile

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import ph.mart.healthapp.core.data.food.DayNutrition
import ph.mart.healthapp.core.data.progress.WeightEntry

private const val TODAY = 20_000L

class EnergyCheckInTest {

    /** Dense days ending today, like the repository's own series. */
    private fun intake(kcal: Int, days: Int = 20, endingDaysAgo: Long = 0) =
        (0 until days).map { index ->
            DayNutrition(
                dateEpochDay = TODAY - endingDaysAgo - index,
                calories = kcal,
                proteinG = 100,
                carbsG = 200,
                fatG = 60,
            )
        }

    /** Weekly weigh-ins ending today, oldest first. */
    private fun weighIns(vararg weightsKg: Double, everyDays: Long = 7, endingDaysAgo: Long = 0) =
        weightsKg.mapIndexed { index, kg ->
            WeightEntry(TODAY - endingDaysAgo - (weightsKg.size - 1 - index) * everyDays, kg)
        }

    /** Losing exactly 0.5 kg a week, over three weeks — inside the 28-day window. */
    private fun losingHalfAKilo() = weighIns(79.5, 79.0, 78.5, 78.0)

    private fun profile(
        goal: Goal = Goal.Lose,
        calorieOverrideKcal: Int? = null,
        sex: Sex = Sex.Male,
    ) = Profile(
        sex = sex,
        age = 30,
        heightCm = 170.0,
        weightKg = 78.0,
        activityLevel = ActivityLevel.Sedentary,
        goal = goal,
        calorieOverrideKcal = calorieOverrideKcal,
    )

    private fun checkIn(
        nutrition: List<DayNutrition> = intake(2100),
        weights: List<WeightEntry> = losingHalfAKilo(),
        profile: Profile = profile(),
    ) = energyCheckIn(nutrition, weights, profile, todayEpochDay = TODAY)

    @Test
    fun `nothing logged in the window has nothing to report at all`() {
        assertNull(checkIn(nutrition = emptyList()))
        // Logged, but all of it older than the window.
        assertNull(checkIn(nutrition = intake(2100, endingDaysAgo = CHECKIN_WINDOW_DAYS)))
    }

    @Test
    fun `maintenance is intake plus the energy the weight change accounts for`() {
        // 2100 eaten a day while losing 0.5 kg a week = 550 kcal a day of deficit -> 2650 burned.
        val estimate = requireNotNull(checkIn()?.estimate)
        assertEquals(2650, estimate.maintenanceKcal)
        // A Lose goal prices the recommendation 500 under measured maintenance.
        assertEquals(2150, estimate.recommendedKcal)
        assertEquals(-0.5, estimate.kgPerWeek, 0.0001)
    }

    @Test
    fun `gaining weight measures a maintenance under what was eaten`() {
        val estimate = requireNotNull(
            checkIn(weights = weighIns(78.0, 78.5, 79.0, 79.5), profile = profile(goal = Goal.Build))?.estimate,
        )
        assertEquals(1550, estimate.maintenanceKcal)
        assertEquals(1850, estimate.recommendedKcal)
    }

    @Test
    fun `the delta is signed against the target actually in force`() {
        // The override is the target the user sees, so it is the one the recommendation moves.
        val estimate = requireNotNull(checkIn(profile = profile(calorieOverrideKcal = 1900))?.estimate)
        assertEquals(250, estimate.deltaKcal)
    }

    @Test
    fun `applying the recommendation drives the delta to zero`() {
        val first = requireNotNull(checkIn()?.estimate)
        // What the Apply button writes, and the only state the feature keeps anywhere.
        val applied = checkIn(profile = profile(calorieOverrideKcal = first.recommendedKcal))
        assertEquals(0, applied?.estimate?.deltaKcal)
        assertEquals(first.recommendedKcal, applied?.currentTargetKcal)
    }

    @Test
    fun `half a window of logged days is not enough intake to divide by`() {
        val thin = checkIn(nutrition = intake(2100, days = MIN_CHECKIN_LOGGED_DAYS - 1))
        assertNull(thin?.estimate)
        // The counts are still reported, so the screen can say what is missing.
        assertEquals(MIN_CHECKIN_LOGGED_DAYS - 1, thin?.daysLogged)
        assertEquals(4, thin?.weighIns)
        assertEquals(2100, thin?.avgIntakeKcal)
    }

    @Test
    fun `three weigh-ins are not enough to fit a rate`() {
        assertNull(checkIn(weights = weighIns(79.5, 79.0, 78.5))?.estimate)
    }

    @Test
    fun `four weigh-ins in one week are a rate about hydration`() {
        assertNull(checkIn(weights = weighIns(79.5, 79.0, 78.5, 78.0, everyDays = 2))?.estimate)
    }

    @Test
    fun `a stale weigh-in cannot anchor a check-in at today`() {
        assertNull(checkIn(weights = losingHalfAKilo().map { it.copy(dateEpochDay = it.dateEpochDay - 8) })?.estimate)
    }

    @Test
    fun `arithmetic outside the sanity range is refused rather than recommended`() {
        // 4 kg a week off a 2100 kcal intake measures a 6500 kcal maintenance: water, not fat.
        assertNull(checkIn(weights = weighIns(90.0, 86.0, 82.0, 78.0))?.estimate)
    }

    @Test
    fun `a recommendation under the safety floor is raised to it and says so`() {
        // Maintenance 1650 on a Lose goal prices at 1150, under the 1500 male floor.
        val estimate = requireNotNull(checkIn(nutrition = intake(1100))?.estimate)
        assertEquals(1650, estimate.maintenanceKcal)
        assertEquals(1500, estimate.recommendedKcal)
        assertTrue(estimate.clampedToFloor)
    }

    @Test
    fun `a maintain goal recommends maintenance itself`() {
        val estimate = requireNotNull(checkIn(profile = profile(goal = Goal.Maintain))?.estimate)
        assertEquals(estimate.maintenanceKcal, estimate.recommendedKcal)
    }
}
