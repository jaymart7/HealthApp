package ph.mart.healthapp.core.data.exercise

import org.junit.Assert.assertEquals
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
