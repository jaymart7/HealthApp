package ph.mart.healthapp.core.data.exercise

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import ph.mart.healthapp.core.data.progress.ChartRange

private const val TODAY = 20_000L

private fun workout(day: Long, vararg sets: StrengthSet) = ExerciseEntry(
    id = day,
    dateEpochDay = day,
    type = ExerciseType.Strength,
    minutes = 45,
    burnedKcal = 260,
    sets = sets.toList(),
)

class StrengthTest {

    @Test
    fun `volume is load times reps, and bodyweight adds none`() {
        val sets = listOf(
            StrengthSet("Bench press", reps = 8, weightKg = 60.0),
            StrengthSet("Bench press", reps = 8, weightKg = 60.0),
            StrengthSet("Push-up", reps = 20, weightKg = 0.0),
        )
        assertEquals(960.0, sets.volumeKg(), 0.001)
        assertEquals(0.0, emptyList<StrengthSet>().volumeKg(), 0.001)
    }

    @Test
    fun `Epley makes a heavy single and a lighter set comparable`() {
        // 80 x 8 is the better lift, even though 100 kg is the heavier bar.
        val single = estimatedOneRepMax(weightKg = 100.0, reps = 1)
        val eight = estimatedOneRepMax(weightKg = 80.0, reps = 8)
        assertEquals(103.33, single, 0.01)
        assertEquals(101.33, eight, 0.01)
        assertTrue(single > eight)
    }

    @Test
    fun `a bodyweight set claims no one-rep max`() {
        assertEquals(0.0, estimatedOneRepMax(weightKg = 0.0, reps = 20), 0.001)
        assertEquals(0.0, estimatedOneRepMax(weightKg = 60.0, reps = 0), 0.001)
    }

    @Test
    fun `the record is the best estimated max, not the heaviest bar`() {
        val entries = listOf(
            workout(TODAY - 10, StrengthSet("Squat", reps = 1, weightKg = 120.0)),
            workout(TODAY - 2, StrengthSet("Squat", reps = 8, weightKg = 110.0)),
        )
        val record = entries.personalRecords().single()
        assertEquals("Squat", record.exerciseName)
        // 110 x 8 = 139.3 beats 120 x 1 = 124.
        assertEquals(110.0, record.bestWeightKg, 0.001)
        assertEquals(8, record.bestReps)
        assertEquals(139.33, record.bestOneRepMaxKg, 0.01)
        assertEquals(TODAY - 2, record.dateEpochDay)
        assertEquals(2, record.sets)
    }

    @Test
    fun `matching a record again keeps the day it was set`() {
        val entries = listOf(
            workout(TODAY - 30, StrengthSet("Deadlift", reps = 5, weightKg = 140.0)),
            workout(TODAY, StrengthSet("Deadlift", reps = 5, weightKg = 140.0)),
        )
        assertEquals(TODAY - 30, entries.personalRecords().single().dateEpochDay)
    }

    @Test
    fun `records are listed most recently set first`() {
        val entries = listOf(
            workout(TODAY - 5, StrengthSet("Row", reps = 10, weightKg = 60.0)),
            workout(TODAY - 1, StrengthSet("Press", reps = 5, weightKg = 50.0)),
        )
        assertEquals(listOf("Press", "Row"), entries.personalRecords().map { it.exerciseName })
    }

    @Test
    fun `cardio has nothing to record`() {
        val entries = listOf(ExerciseEntry(type = ExerciseType.Run, minutes = 30, burnedKcal = 363))
        assertEquals(emptyList<LiftRecord>(), entries.personalRecords())
        assertEquals(emptyList<DayVolume>(), entries.volumeByDay())
        assertEquals(StrengthTotals(0, 0, 0.0), entries.strengthTotals())
    }

    @Test
    fun `a rest day is a gap, not a zero`() {
        val entries = listOf(
            workout(TODAY - 4, StrengthSet("Squat", reps = 5, weightKg = 100.0)),
            workout(TODAY, StrengthSet("Squat", reps = 5, weightKg = 100.0)),
            workout(TODAY, StrengthSet("Bench press", reps = 5, weightKg = 80.0)),
        )
        val series = entries.volumeByDay()
        assertEquals(listOf(TODAY - 4, TODAY), series.map { it.dateEpochDay })
        // Today's two sessions fold into one day: 500 + 400.
        assertEquals(900.0, series.last().volumeKg, 0.001)
        assertEquals(2, series.last().sets)
        assertEquals(2, series.last().workouts)
    }

    @Test
    fun `the window is anchored to today, so a stale series still shows its gaps`() {
        val series = listOf(
            DayVolume(TODAY - 200, 1000.0, 4, 1),
            DayVolume(TODAY - 10, 1200.0, 5, 1),
        )
        assertEquals(listOf(TODAY - 10), series.inRange(ChartRange.OneMonth, TODAY).map { it.dateEpochDay })
        assertEquals(2, series.inRange(ChartRange.OneYear, TODAY).size)
    }

    @Test
    fun `totals count only what was lifted`() {
        val entries = listOf(
            workout(TODAY - 1, StrengthSet("Squat", reps = 5, weightKg = 100.0)),
            ExerciseEntry(type = ExerciseType.Run, minutes = 30, burnedKcal = 363),
        )
        assertEquals(StrengthTotals(workouts = 1, sets = 1, volumeKg = 500.0), entries.strengthTotals())
    }

    @Test
    fun `lift suggestions come newest first, deduplicated`() {
        val entries = listOf(
            workout(TODAY - 7, StrengthSet("Squat", 5, 100.0), StrengthSet("Row", 10, 60.0)),
            workout(TODAY, StrengthSet("Bench press", 5, 80.0), StrengthSet("Squat", 5, 105.0)),
        )
        assertEquals(listOf("Bench press", "Squat", "Row"), entries.recentLiftNames())
        assertEquals(listOf("Bench press", "Squat"), entries.recentLiftNames(limit = 2))
    }
}
