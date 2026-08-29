package ph.mart.healthapp.core.data.health

import org.junit.Assert.assertEquals
import org.junit.Test
import ph.mart.healthapp.core.data.exercise.ExerciseEntry
import ph.mart.healthapp.core.data.exercise.ExerciseType

/** The step credit's arithmetic — every branch that decides how much a day's walking is worth. */
class StepsTest {

    private fun day(steps: Int, burnedKcal: Int) =
        StepDay(dateEpochDay = 20_000, steps = steps, burnedKcal = burnedKcal)

    private fun walk(minutes: Int, burnedKcal: Int, steps: Int) = ExerciseEntry(
        type = ExerciseType.Walk,
        minutes = minutes,
        burnedKcal = burnedKcal,
        steps = steps,
    )

    @Test
    fun `steps convert to kcal at the walking MET, and scale with weight`() {
        // 10000 steps = 100 minutes at the moderate cadence; 3.5 MET x 80kg x 100/60 h.
        assertEquals(467, stepsBurnedKcal(10_000, weightKg = 80.0))
        assertEquals(0, stepsBurnedKcal(0, weightKg = 80.0))

        val light = stepsBurnedKcal(10_000, weightKg = 60.0)
        val heavy = stepsBurnedKcal(10_000, weightKg = 90.0)
        assertEquals(true, heavy > light)
    }

    @Test
    fun `only step-bearing activities are assumed to have taken steps`() {
        assertEquals(3000, estimatedSteps(ExerciseType.Walk, minutes = 30))
        assertEquals(3000, estimatedSteps(ExerciseType.Run, minutes = 30))
        // A swim or a lifting session can burn a great deal without moving a pedometer, and
        // pretending otherwise would subtract a day's real walking.
        assertEquals(0, estimatedSteps(ExerciseType.Swim, minutes = 30))
        assertEquals(0, estimatedSteps(ExerciseType.Strength, minutes = 30))
    }

    @Test
    fun `a workout's own steps come off before anything is credited`() {
        // Half the day's steps belong to the walk, so half the day's burn is already paid for.
        val credit = stepsCreditKcal(day(steps = 8000, burnedKcal = 300), listOf(walk(40, 150, 4000)))
        assertEquals(150, credit)
    }

    @Test
    fun `a day whose workouts account for every step credits nothing`() {
        assertEquals(0, stepsCreditKcal(day(steps = 4000, burnedKcal = 150), listOf(walk(40, 150, 4000))))
        // And it floors at zero rather than going negative when the watch counted more in the
        // session than it did for the day.
        assertEquals(0, stepsCreditKcal(day(steps = 3000, burnedKcal = 120), listOf(walk(40, 150, 4000))))
    }

    @Test
    fun `no steps means no credit, and no crash`() {
        assertEquals(0, stepsCreditKcal(null, listOf(walk(40, 150, 4000))))
        assertEquals(0, stepsCreditKcal(day(steps = 0, burnedKcal = 0), emptyList()))
    }

    @Test
    fun `the day's burn is unchanged for a user with no step data`() {
        val entries = listOf(walk(40, 150, 4000), ExerciseEntry(type = ExerciseType.Swim, minutes = 30, burnedKcal = 250))
        assertEquals(400, dayBurnedKcal(entries, steps = null))
        assertEquals(0, dayBurnedKcal(emptyList(), steps = null))
    }

    @Test
    fun `the day's burn adds the uncredited walking on top of the workouts`() {
        val entries = listOf(walk(40, 150, 4000))
        assertEquals(150 + 150, dayBurnedKcal(entries, day(steps = 8000, burnedKcal = 300)))
    }
}
