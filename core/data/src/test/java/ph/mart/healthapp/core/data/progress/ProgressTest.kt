package ph.mart.healthapp.core.data.progress

import org.junit.Assert.assertEquals
import org.junit.Test

class ProgressTest {

    @Test
    fun `moving average is 2-point trailing, sorted by date`() {
        val entries = listOf(
            WeightEntry(dateEpochDay = 2, weightKg = 78.0),
            WeightEntry(dateEpochDay = 0, weightKg = 80.0),
            WeightEntry(dateEpochDay = 1, weightKg = 79.0),
        )
        val points = entries.withMovingAverage()

        assertEquals(listOf(0L, 1L, 2L), points.map { it.dateEpochDay })
        assertEquals(80.0, points[0].movingAverageKg, 0.001)
        assertEquals(79.5, points[1].movingAverageKg, 0.001)
        assertEquals(78.5, points[2].movingAverageKg, 0.001)
    }

    @Test
    fun `backdating an entry recomputes the whole series, not just the inserted point`() {
        val beforeBackdate = listOf(
            WeightEntry(dateEpochDay = 0, weightKg = 80.0),
            WeightEntry(dateEpochDay = 5, weightKg = 76.0),
        ).withMovingAverage()
        assertEquals(78.0, beforeBackdate.last().movingAverageKg, 0.001)

        val afterBackdate = listOf(
            WeightEntry(dateEpochDay = 0, weightKg = 80.0),
            WeightEntry(dateEpochDay = 3, weightKg = 78.0),
            WeightEntry(dateEpochDay = 5, weightKg = 76.0),
        ).withMovingAverage()
        assertEquals(77.0, afterBackdate.last().movingAverageKg, 0.001)
    }

    @Test
    fun `inRange filters relative to the latest entry, not today`() {
        val entries = listOf(
            WeightEntry(dateEpochDay = 100, weightKg = 80.0),
            WeightEntry(dateEpochDay = 190, weightKg = 78.0),
            WeightEntry(dateEpochDay = 200, weightKg = 76.0),
        )
        val filtered = entries.inRange(ChartRange.OneMonth)
        assertEquals(listOf(190L, 200L), filtered.map { it.dateEpochDay })
    }
}
