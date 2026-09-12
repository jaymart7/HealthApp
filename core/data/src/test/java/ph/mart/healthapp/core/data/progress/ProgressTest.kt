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

    /** Both cards that draw a photo run fold it here, so the ends are the ends of the *weighed*
     * shots — the unweighed ones between and beyond them are still photos, just not measurements. */
    @Test
    fun `the weight arc spans the weighed shots and ignores the rest`() {
        val arc = listOf(
            ProgressPhoto(id = 1, dateEpochDay = 100, filePath = "a"),
            ProgressPhoto(id = 2, dateEpochDay = 108, filePath = "b", weightKg = 79.0),
            ProgressPhoto(id = 3, dateEpochDay = 150, filePath = "c"),
            ProgressPhoto(id = 4, dateEpochDay = 200, filePath = "d", weightKg = 76.9),
            ProgressPhoto(id = 5, dateEpochDay = 210, filePath = "e"),
        ).weightArc()

        assertEquals(-2.1, arc?.deltaKg ?: 0.0, 0.001)
        assertEquals(92L, arc?.days)
    }

    @Test
    fun `one weighed shot is not an arc`() {
        val arc = listOf(
            ProgressPhoto(id = 1, dateEpochDay = 100, filePath = "a"),
            ProgressPhoto(id = 2, dateEpochDay = 200, filePath = "b", weightKg = 76.9),
        ).weightArc()

        assertEquals(null, arc)
    }

    /** Two weighings on one day is a difference over no time at all — the shots are the same
     * morning, and reporting a change between them would be reporting the scale's noise. */
    @Test
    fun `two weighed shots on the same day are not an arc`() {
        val arc = listOf(
            ProgressPhoto(id = 1, dateEpochDay = 200, filePath = "a", weightKg = 79.0),
            ProgressPhoto(id = 2, dateEpochDay = 200, filePath = "b", weightKg = 76.9),
        ).weightArc()

        assertEquals(null, arc)
    }

    /** The list arrives newest-first from the repository as often as not, and the arc is a
     * direction: reading the ends off an unsorted list flips its sign. */
    @Test
    fun `the arc reads the same whichever order the photos arrive in`() {
        val photos = listOf(
            ProgressPhoto(id = 1, dateEpochDay = 108, filePath = "a", weightKg = 79.0),
            ProgressPhoto(id = 2, dateEpochDay = 200, filePath = "b", weightKg = 76.9),
        )

        assertEquals(photos.weightArc(), photos.reversed().weightArc())
    }
}
