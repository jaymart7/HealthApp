package ph.mart.healthapp.feature.training.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import ph.mart.healthapp.core.data.exercise.ExerciseEntry
import ph.mart.healthapp.core.data.exercise.ExerciseType
import ph.mart.healthapp.core.data.exercise.ParsedExercise
import ph.mart.healthapp.core.data.exercise.StrengthSet

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

    /** The strength screen's whole output. A round trip that dropped them would save a workout
     * with no lifts in it, which is the one thing that screen exists to prevent. */
    @Test
    fun `sets survive the form round trip, trimmed`() {
        val sets = listOf(
            StrengthSet("  Bench press ", reps = 8, weightKg = 62.5),
            StrengthSet("Dip", reps = 12, weightKg = 0.0),
        )
        val entry = LogExerciseForm(type = ExerciseType.Strength, minutes = 45, sets = sets).toExerciseEntry()

        assertEquals(listOf("Bench press", "Dip"), entry.sets.map { it.exerciseName })
        assertEquals(sets.map { it.reps to it.weightKg }, entry.sets.map { it.reps to it.weightKg })
        assertEquals(entry.sets, entry.toLogExerciseForm().sets)
    }

    /**
     * The mirror of the edit case above, and what makes the model's silence on calories safe: a
     * parsed activity lands on a **new** form, where [LogExerciseForm.burnedEdited] is false, so
     * `withEstimate` prices the parsed type and duration at the user's own weight. Nothing the
     * model said reaches the kcal field, because there is nothing it could have said.
     */
    @Test
    fun `a parsed activity is priced by the app, not by whatever answered`() {
        val parsed = ParsedExercise(type = ExerciseType.Run, name = "easy river loop", minutes = 45)
        val form = LogExerciseForm().withParsed(parsed).withEstimate(weightKg = 74.0)

        assertEquals(ExerciseType.Run, form.type)
        assertEquals("easy river loop", form.name)
        assertEquals(45, form.minutes)
        // MET 9.8 × 74 kg × 0.75 h.
        assertEquals(544, form.burnedKcal)
        assertFalse(form.burnedEdited)
    }

    /** A kcal figure the user typed before describing the workout is still theirs: the latch is
     * what a parse must not reach past, exactly as a type chip must not. */
    @Test
    fun `a parse leaves a hand-typed burn alone`() {
        val edited = LogExerciseForm(burnedKcal = 200, burnedEdited = true)
        val parsed = ParsedExercise(type = ExerciseType.Run, name = "", minutes = 45)

        assertEquals(200, edited.withParsed(parsed).withEstimate(weightKg = 74.0).burnedKcal)
    }

    /** An activity logged through the sheet has nothing to lift, and must not acquire any. */
    @Test
    fun `a cardio form carries no sets`() {
        assertEquals(emptyList<StrengthSet>(), LogExerciseForm().toExerciseEntry().sets)
    }
}
