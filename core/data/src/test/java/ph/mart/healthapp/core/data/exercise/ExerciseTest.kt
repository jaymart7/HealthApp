package ph.mart.healthapp.core.data.exercise

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExerciseTest {

    @Test
    fun `the estimate is MET times kilograms times hours`() {
        // Run (9.8 MET) for 30 min at 74 kg -> 9.8 * 74 * 0.5 = 362.6 -> 363
        assertEquals(363, estimateBurnedKcal(ExerciseType.Run, minutes = 30, weightKg = 74.0))
        // Half the time, half the burn.
        assertEquals(181, estimateBurnedKcal(ExerciseType.Run, minutes = 15, weightKg = 74.0))
        assertEquals(0, estimateBurnedKcal(ExerciseType.Run, minutes = 0, weightKg = 74.0))
    }

    /** A set is priced by its reps and by the body plus the bar — ten push-ups at 70 kg is MET 5
     * over 30 seconds. More reps, or more load, is more burn. */
    @Test
    fun `a set burns by its reps and its load`() {
        assertEquals(3, StrengthSet("Push-ups", reps = 10, weightKg = 0.0).burnedKcal(bodyKg = 70.0))
        assertEquals(6, StrengthSet("Push-ups", reps = 20, weightKg = 0.0).burnedKcal(bodyKg = 70.0))
        // Bench 8 at 60 kg on an 80 kg body: 5 × 140 × 24 s.
        assertEquals(5, StrengthSet("Bench press", reps = 8, weightKg = 60.0).burnedKcal(bodyKg = 80.0))
    }

    /** The total is the sum of the rounded sets, so the rows drawn above it always add up to it. */
    @Test
    fun `a workout's burn is the sum of its sets`() {
        val sets = List(3) { StrengthSet("Push-ups", reps = 10, weightKg = 0.0) }
        assertEquals(9, sets.burnedKcal(bodyKg = 70.0))
        assertEquals(9, activityBurnedKcal(ExerciseType.Strength, minutes = null, sets = sets, weightKg = 70.0))
    }

    /** Timed or counted: only a Strength activity with its sets is counted. A jog keeps its minutes
     * and is priced by them, and so does "45 minutes of weights" with no sets named. */
    @Test
    fun `only a strength session with sets is counted`() {
        val sets = listOf(StrengthSet("Push-ups", reps = 10, weightKg = 0.0))
        assertTrue(isCounted(ExerciseType.Strength, sets))
        assertFalse(isCounted(ExerciseType.Strength, emptyList()))
        assertFalse(isCounted(ExerciseType.Run, sets))
        assertEquals(363, activityBurnedKcal(ExerciseType.Run, minutes = 30, sets = sets, weightKg = 74.0))
    }

    @Test
    fun `a heavier body burns more for the same session`() {
        val light = estimateBurnedKcal(ExerciseType.Walk, minutes = 60, weightKg = 60.0)
        val heavy = estimateBurnedKcal(ExerciseType.Walk, minutes = 60, weightKg = 90.0)
        assertEquals(210, light)
        assertEquals(315, heavy)
    }

    @Test
    fun `the toggle is what stops the activity multiplier being counted twice`() {
        assertEquals(2400, budgetKcal(targetKcal = 2100, burnedKcal = 300, addExercise = true))
        assertEquals(2100, budgetKcal(targetKcal = 2100, burnedKcal = 300, addExercise = false))
        // Nothing logged is the same budget either way.
        assertEquals(2100, budgetKcal(targetKcal = 2100, burnedKcal = 0, addExercise = true))
    }

    @Test
    fun `the day's burn is the sum of its entries`() {
        val entries = listOf(
            ExerciseEntry(type = ExerciseType.Run, minutes = 30, burnedKcal = 363),
            ExerciseEntry(type = ExerciseType.Yoga, minutes = 45, burnedKcal = 166),
        )
        assertEquals(529, entries.totalBurnedKcal())
        assertEquals(0, emptyList<ExerciseEntry>().totalBurnedKcal())
    }
}
