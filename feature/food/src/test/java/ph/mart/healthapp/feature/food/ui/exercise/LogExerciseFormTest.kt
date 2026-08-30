package ph.mart.healthapp.feature.food.ui.exercise

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import ph.mart.healthapp.core.data.exercise.ExerciseEntry
import ph.mart.healthapp.core.data.exercise.ExerciseType

class LogExerciseFormTest {

    private val logged = ExerciseEntry(
        id = 3,
        dateEpochDay = 20000,
        type = ExerciseType.Run,
        name = "Riverside loop",
        minutes = 30,
        burnedKcal = 363,
        steps = 4200,
    )

    @Test
    fun `a logged activity round-trips through the edit form unchanged`() {
        assertEquals(logged.copy(id = 0), logged.toLogExerciseForm().toExerciseEntry(logged.dateEpochDay))
    }

    /**
     * The regression that matters: [withEstimate] re-derives the burn from MET × weight × hours,
     * and the sheet calls it on every recomposition. Seeding an edit without latching
     * [LogExerciseForm.burnedEdited] would rewrite what a past workout burned at whatever the
     * user weighs today.
     */
    @Test
    fun `opening an activity to edit it keeps the burn that was logged`() {
        val form = logged.toLogExerciseForm()
        assertTrue(form.burnedEdited)
        assertEquals(363, form.withEstimate(weightKg = 95.0).burnedKcal)
    }

    /** The watch's own step count is the one number here nobody guessed — an edit carries it. */
    @Test
    fun `an edit carries the stored step count`() {
        assertEquals(4200, logged.toLogExerciseForm().toExerciseEntry().steps)
    }

    /** A new form still reports zero, which is what makes the repository fill in the estimate. */
    @Test
    fun `a fresh form claims no steps`() {
        assertEquals(0, LogExerciseForm().toExerciseEntry().steps)
    }
}
