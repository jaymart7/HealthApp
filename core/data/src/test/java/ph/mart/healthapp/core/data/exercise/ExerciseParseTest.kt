package ph.mart.healthapp.core.data.exercise

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import ph.mart.healthapp.core.data.profile.KG_PER_LB
import ph.mart.healthapp.core.data.profile.UnitSystem

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

    // parsedSets — the strength screen's boundary.

    private fun row(lift: String? = "Bench", sets: Int? = 3, reps: Int? = 8, weight: Double? = 60.0, unit: String? = null) =
        ParsedLiftRow(lift, sets, reps, weight, unit)

    @Test
    fun `a set count expands into that many identical sets`() {
        val sets = parsedSets(listOf(row()), UnitSystem.Metric)
        assertEquals(List(3) { StrengthSet("Bench", 8, 60.0) }, sets)
    }

    @Test
    fun `a missing count is one set`() {
        assertEquals(1, parsedSets(listOf(row(sets = null)), UnitSystem.Metric).size)
    }

    /** The unit is never sent, so a load with none is the user's own unit, not the model's guess. */
    @Test
    fun `a named unit wins and a missing one is the user's`() {
        assertEquals(100 * KG_PER_LB, parsedSets(listOf(row(sets = 1, weight = 100.0, unit = "lb")), UnitSystem.Metric)[0].weightKg, 1e-9)
        assertEquals(100.0, parsedSets(listOf(row(sets = 1, weight = 100.0, unit = "KG")), UnitSystem.Imperial)[0].weightKg, 1e-9)
        assertEquals(100 * KG_PER_LB, parsedSets(listOf(row(sets = 1, weight = 100.0)), UnitSystem.Imperial)[0].weightKg, 1e-9)
    }

    /** Zero is bodyweight here, a real value — "pull-ups 3x10" says nothing more. */
    @Test
    fun `no weight is bodyweight`() {
        assertEquals(0.0, parsedSets(listOf(row(weight = null)), UnitSystem.Metric)[0].weightKg, 0.0)
        assertEquals(0.0, parsedSets(listOf(row(weight = -5.0)), UnitSystem.Metric)[0].weightKg, 0.0)
    }

    @Test
    fun `an unusable row is dropped and the rest kept`() {
        val sets = parsedSets(
            listOf(
                row(lift = "  "),
                row(reps = 0),
                row(reps = MAX_PARSED_REPS + 1),
                row(sets = MAX_PARSED_SET_COUNT + 1),
                row(weight = MAX_PARSED_LOAD_KG + 1),
                row(lift = "**Squat**", sets = 1),
            ),
            UnitSystem.Metric,
        )
        assertEquals(listOf(StrengthSet("Squat", 8, 60.0)), sets)
    }

    @Test
    fun `the session is capped`() {
        val rows = List(10) { row(sets = MAX_PARSED_SET_COUNT) }
        assertEquals(MAX_PARSED_SETS, parsedSets(rows, UnitSystem.Metric).size)
    }

    @Test
    fun `nothing usable is empty`() {
        assertTrue(parsedSets(listOf(row(reps = null)), UnitSystem.Metric).isEmpty())
        assertTrue(parsedSets(emptyList(), UnitSystem.Metric).isEmpty())
    }
}
