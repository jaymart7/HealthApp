package ph.mart.healthapp.core.data.exercise

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * [parsedExercise] is the whole trust boundary on what a model says a workout was, and it is pure
 * so that this file can reach it — the split `loggable()` already makes, for the same reason: the
 * `org.json` read around it is stubbed on the JVM.
 */
class ExerciseParseTest {

    @Test
    fun `a named type is matched however the model cased it`() {
        assertEquals(ExerciseType.Run, parsedExercise("run", "", 30)?.type)
        assertEquals(ExerciseType.Run, parsedExercise("RUN", "", 30)?.type)
        assertEquals(ExerciseType.Hiit, parsedExercise("Hiit", "", 30)?.type)
    }

    /**
     * The one rejection worth naming: a type the model invented is **not** folded into
     * [ExerciseType.Other], because the type is what the burn is computed from and a guessed one
     * would price a workout the user never described.
     */
    @Test
    fun `a type the model invented is rejected rather than bucketed`() {
        assertNull(parsedExercise("Pilates", "", 30))
        assertNull(parsedExercise(null, "", 30))
    }

    /** Zero minutes is the prompt's "they named no physical activity", not a shorter workout. */
    @Test
    fun `zero minutes is no activity at all`() {
        assertNull(parsedExercise("Run", "", 0))
        assertNull(parsedExercise("Run", "", null))
    }

    @Test
    fun `a duration past the ceiling is rejected`() {
        assertEquals(MAX_EXERCISE_MINUTES, parsedExercise("Walk", "", MAX_EXERCISE_MINUTES)?.minutes)
        assertNull(parsedExercise("Walk", "", MAX_EXERCISE_MINUTES + 1))
        assertNull(parsedExercise("Walk", "", -30))
    }

    /** The note becomes a diary row's title, so it is stripped exactly as a recognised food's
     * name is — a model told to write no markdown writes it anyway. */
    @Test
    fun `the note is stripped of markdown and trimmed`() {
        assertEquals("easy river loop", parsedExercise("Run", "  **easy river loop**  ", 45)?.name)
    }

    @Test
    fun `an over-long note is truncated rather than rejected`() {
        val parsed = parsedExercise("Cycle", "a ".repeat(200), 20)
        assertEquals(MAX_EXERCISE_NAME_CHARS - 1, parsed?.name?.length)
        assertEquals(20, parsed?.minutes)
    }

    /** Blank is a real answer: it is what [ExerciseEntry] means by "call it by its type", and the
     * prompt asks for the field to be left out rather than filled in with an invention. */
    @Test
    fun `a blank note is kept blank`() {
        assertEquals("", parsedExercise("Swim", null, 30)?.name)
        assertEquals("", parsedExercise("Swim", "   ", 30)?.name)
    }
}
