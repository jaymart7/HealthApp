package ph.mart.healthapp.feature.progress.ui.progress

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import ph.mart.healthapp.core.data.exercise.ExerciseEntry
import ph.mart.healthapp.core.data.exercise.ExerciseType
import ph.mart.healthapp.core.data.exercise.StrengthSet
import ph.mart.healthapp.core.data.food.DayNutrition
import ph.mart.healthapp.core.data.health.StepDay
import ph.mart.healthapp.core.data.mood.MoodDay
import ph.mart.healthapp.core.data.profile.DailyTargets
import ph.mart.healthapp.core.data.progress.ProgressPhoto
import ph.mart.healthapp.core.data.progress.WeightEntry

private const val TODAY = 20_000L

private val TARGETS = DailyTargets(calories = 2000, proteinG = 150, carbsG = 200, fatG = 67, floor = 1500)

/** Dense week ending today, zero-filled where nothing was eaten — the shape the repository emits. */
private fun week(vararg calories: Int): List<DayNutrition> =
    calories.mapIndexed { index, kcal ->
        val day = TODAY - (calories.size - 1 - index)
        DayNutrition(day, kcal, kcal / 16, kcal / 10, kcal / 30)
    }

private fun weekOf(
    nutrition: List<DayNutrition> = week(0, 0, 0, 0, 0, 0, 0),
    activeDays: Set<Long> = emptySet(),
    weightEntries: List<WeightEntry> = emptyList(),
    moodDays: List<MoodDay> = emptyList(),
    targets: DailyTargets? = TARGETS,
) = recap(
    period = RecapPeriod.Week,
    dailyNutrition = nutrition,
    activeDays = activeDays,
    weightEntries = weightEntries,
    moodDays = moodDays,
    targets = targets,
    todayEpochDay = TODAY,
)

class RecapTest {

    @Test
    fun `nothing logged in the window means no card at all`() {
        // Logged, but a fortnight ago — outside the window.
        assertNull(weekOf(activeDays = setOf(TODAY - 20, TODAY - 14)))
    }

    @Test
    fun `days logged counts every domain, calories average only food days`() {
        val result = weekOf(
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
        val result = weekOf(activeDays = setOf(TODAY - 7, TODAY - 6, TODAY))
        assertEquals(2, result!!.daysLogged)
    }

    @Test
    fun `best day is the closest to target, ties breaking to the more recent day`() {
        val result = weekOf(
            nutrition = week(0, 1900, 0, 0, 2100, 0, 1500),
            activeDays = (TODAY - 5..TODAY).toSet(),
        )
        // 1900 and 2100 are both 100 off; the later day wins.
        assertEquals(TODAY - 2, result!!.bestDay?.dateEpochDay)
        assertEquals(2100, result.bestDay?.calories)
    }

    @Test
    fun `no targets means no best day`() {
        val result = weekOf(
            nutrition = week(0, 1900, 0, 0, 0, 0, 0),
            activeDays = setOf(TODAY),
            targets = null,
        )
        assertNull(result!!.bestDay)
    }

    @Test
    fun `a weigh-in inside the window reports the delta`() {
        val result = weekOf(
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
        val result = weekOf(
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
        val result = weekOf(
            activeDays = (TODAY - 2..TODAY).toSet(),
            moodDays = listOf(MoodDay(TODAY - 30, mood = 1, energy = 1)),
        )
        assertNull(result!!.moodAverages)
    }

    @Test
    fun `mood inside the window is averaged, energy keeping its own denominator`() {
        val result = weekOf(
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

    // --- the longer windows -------------------------------------------------------------------

    @Test
    fun `a month counts the days a week cannot see`() {
        val activeDays = setOf(TODAY - 25, TODAY - 20, TODAY - 3, TODAY)
        assertEquals(2, weekOf(activeDays = activeDays)!!.daysLogged)
        assertEquals(4, monthOf(activeDays = activeDays)!!.daysLogged)
    }

    @Test
    fun `the window is inclusive of its own first day`() {
        // Exactly 30 days back is the first day of a 30-day window; 30 days plus one is outside.
        assertEquals(1, monthOf(activeDays = setOf(TODAY - 29))!!.daysLogged)
        assertNull(monthOf(activeDays = setOf(TODAY - 30)))
    }

    @Test
    fun `sparse inputs are sliced by date, never by tail`() {
        val result = monthOf(
            activeDays = setOf(TODAY),
            // Newest rows are the two inside the window; the older pair must not ride in on
            // being last in the list.
            photos = listOf(
                ProgressPhoto(id = 1, dateEpochDay = TODAY - 200, filePath = "a"),
                ProgressPhoto(id = 2, dateEpochDay = TODAY - 100, filePath = "b"),
                ProgressPhoto(id = 3, dateEpochDay = TODAY - 20, filePath = "c"),
                ProgressPhoto(id = 4, dateEpochDay = TODAY, filePath = "d"),
            ),
            stepDays = listOf(
                StepDay(dateEpochDay = TODAY - 100, steps = 20_000, burnedKcal = 600),
                StepDay(dateEpochDay = TODAY - 5, steps = 8_000, burnedKcal = 240),
            ),
        )!!
        assertEquals(listOf(3L, 4L), result.photos.map { it.id })
        assertEquals(1, result.steps.days)
        assertEquals(8_000, result.steps.averageSteps)
        // The out-of-window day was the better one, and must not be reported as the best.
        assertEquals(8_000, result.steps.bestSteps)
    }

    @Test
    fun `strength totals and the top lift fold over the window only`() {
        val result = monthOf(
            activeDays = setOf(TODAY),
            exerciseEntries = listOf(
                strength(TODAY - 60, "Deadlift", weightKg = 180.0, reps = 5),
                strength(TODAY - 10, "Squat", weightKg = 100.0, reps = 5),
                strength(TODAY - 2, "Bench", weightKg = 80.0, reps = 5),
            ),
        )!!
        assertEquals(2, result.strength.workouts)
        assertEquals(2, result.workouts)
        assertEquals(900.0, result.strength.volumeKg, 0.001)
        // Newest-first, so the deadlift is both out of the window and not the answer.
        assertEquals("Bench", result.topLift?.exerciseName)
    }

    @Test
    fun `the weight arc reports the window's own two ends`() {
        val result = monthOf(
            activeDays = setOf(TODAY),
            weightEntries = listOf(
                WeightEntry(dateEpochDay = TODAY - 90, weightKg = 90.0),
                WeightEntry(dateEpochDay = TODAY - 25, weightKg = 80.0),
                WeightEntry(dateEpochDay = TODAY - 1, weightKg = 78.0),
            ),
        )!!
        assertEquals(80.0, result.startWeightKg!!, 0.001)
        assertEquals(78.0, result.endWeightKg!!, 0.001)
        assertEquals(-2.0, result.weightArcKg!!, 0.001)
    }

    @Test
    fun `one weigh-in in the window is not an arc`() {
        val result = monthOf(
            activeDays = setOf(TODAY),
            weightEntries = listOf(
                WeightEntry(dateEpochDay = TODAY - 90, weightKg = 90.0),
                WeightEntry(dateEpochDay = TODAY - 3, weightKg = 78.0),
            ),
        )!!
        assertEquals(78.0, result.startWeightKg!!, 0.001)
        // Both ends are the same entry, so there is nothing to compare — never a zero delta.
        assertNull(result.weightArcKg)
    }

    @Test
    fun `an empty window has no report at any period`() {
        assertNull(weekOf())
        assertNull(monthOf())
    }
}

private fun monthOf(
    nutrition: List<DayNutrition> = week(0, 0, 0, 0, 0, 0, 0),
    activeDays: Set<Long> = emptySet(),
    weightEntries: List<WeightEntry> = emptyList(),
    moodDays: List<MoodDay> = emptyList(),
    targets: DailyTargets? = TARGETS,
    exerciseEntries: List<ExerciseEntry> = emptyList(),
    stepDays: List<StepDay> = emptyList(),
    photos: List<ProgressPhoto> = emptyList(),
) = recap(
    period = RecapPeriod.Month,
    dailyNutrition = nutrition,
    activeDays = activeDays,
    weightEntries = weightEntries,
    moodDays = moodDays,
    targets = targets,
    todayEpochDay = TODAY,
    exerciseEntries = exerciseEntries,
    stepDays = stepDays,
    photos = photos,
)

private fun strength(day: Long, lift: String, weightKg: Double, reps: Int) = ExerciseEntry(
    dateEpochDay = day,
    type = ExerciseType.Strength,
    minutes = 45,
    burnedKcal = 300,
    sets = listOf(StrengthSet(exerciseName = lift, weightKg = weightKg, reps = reps)),
)
