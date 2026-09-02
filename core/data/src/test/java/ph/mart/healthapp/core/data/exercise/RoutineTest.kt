package ph.mart.healthapp.core.data.exercise

import org.junit.Assert.assertEquals
import org.junit.Test

class RoutineTest {

    @Test
    fun `a set list collapses into one lift per exercise, in the order they were first performed`() {
        val lifts = listOf(
            StrengthSet("Bench press", reps = 8, weightKg = 60.0),
            StrengthSet("Row", reps = 10, weightKg = 40.0),
            StrengthSet("Bench press", reps = 8, weightKg = 62.5),
            StrengthSet("Bench press", reps = 6, weightKg = 65.0),
        ).toRoutineLifts()

        assertEquals(
            // Three sets of bench at the rep figure hit most often — 8, not the 6 that fell short.
            listOf(RoutineLift("Bench press", sets = 3, reps = 8), RoutineLift("Row", sets = 1, reps = 10)),
            lifts,
        )
    }

    @Test
    fun `the same lift typed two ways is one lift`() {
        val lifts = listOf(
            StrengthSet("Squat", reps = 5, weightKg = 100.0),
            StrengthSet(" squat ", reps = 5, weightKg = 100.0),
        ).toRoutineLifts()

        // Named as it was first typed, counted as one lift.
        assertEquals(listOf(RoutineLift("Squat", sets = 2, reps = 5)), lifts)
    }

    @Test
    fun `a tie on reps lands on the figure hit first`() {
        val lifts = listOf(
            StrengthSet("Curl", reps = 12, weightKg = 15.0),
            StrengthSet("Curl", reps = 10, weightKg = 15.0),
        ).toRoutineLifts()

        assertEquals(listOf(RoutineLift("Curl", sets = 2, reps = 12)), lifts)
    }

    @Test
    fun `a nameless set is dropped rather than saved as a nameless lift`() {
        assertEquals(emptyList<RoutineLift>(), listOf(StrengthSet("  ", reps = 8, weightKg = 20.0)).toRoutineLifts())
    }

    @Test
    fun `starting a routine seeds every set at what was last lifted`() {
        val routine = Routine(
            id = 1,
            name = "Push day",
            lifts = listOf(RoutineLift("Bench press", sets = 3, reps = 8), RoutineLift("Dip", sets = 2, reps = 10)),
        )

        // Keyed the way liftKey() keys it, which is how lastPerformances() hands it over.
        val seeded = routine.toSets(mapOf("bench press" to 62.5))

        assertEquals(5, seeded.size)
        assertEquals(List(3) { StrengthSet("Bench press", reps = 8, weightKg = 62.5) }, seeded.take(3))
        // No history for the dip, so it seeds at bodyweight and the user types the load if there is one.
        assertEquals(List(2) { StrengthSet("Dip", reps = 10, weightKg = 0.0) }, seeded.drop(3))
    }

    @Test
    fun `a saved workout round-trips back into the sets it was saved from`() {
        val logged = listOf(
            StrengthSet("Squat", reps = 5, weightKg = 100.0),
            StrengthSet("Squat", reps = 5, weightKg = 100.0),
        )
        val routine = Routine(id = 1, name = "Legs", lifts = logged.toRoutineLifts())

        assertEquals(2, routine.totalSets())
        assertEquals(logged, routine.toSets(mapOf("squat" to 100.0)))
    }
}
