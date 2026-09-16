package ph.mart.healthapp.feature.progress.ui.weight

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import ph.mart.healthapp.core.data.progress.NOTE_GOOGLE_HEALTH
import ph.mart.healthapp.core.data.progress.WeightEntry

/**
 * The split has four ways to answer "nothing honest to say" and one way to answer at all, and this
 * pins all five. Its point is the floors: a card that appears on noise is worse than no card.
 */
class WeighInTimingTest {

    private fun entry(day: Long, kg: Double, minute: Int?, note: String = "") =
        WeightEntry(dateEpochDay = day, weightKg = kg, note = note, minuteOfDay = minute)

    /** Twelve readings, six at 7:00 and six at 19:00, the evenings a clear kilo heavier. */
    private fun splitLog(
        earlyMinute: Int = 7 * 60,
        lateMinute: Int = 19 * 60,
        earlyKg: Double = 76.0,
        lateKg: Double = 77.0,
        each: Int = 6,
    ): List<WeightEntry> = buildList {
        repeat(each) { add(entry(20_000L + it, earlyKg, earlyMinute)) }
        repeat(each) { add(entry(20_100L + it, lateKg, lateMinute)) }
    }

    @Test
    fun `a real morning-evening split is reported, with both sides' counts`() {
        val split = splitLog().weighInTimeSplit()
        assertNotNull(split)
        requireNotNull(split)
        assertEquals(6, split.earlyDays)
        assertEquals(6, split.lateDays)
        assertEquals(76.0, split.earlyAvgKg, 0.001)
        assertEquals(77.0, split.lateAvgKg, 0.001)
        assertEquals(1.0, split.deltaKg, 0.001)
        // The exact boundary, so "from 7:00 PM on" is true rather than rounded.
        assertEquals(19 * 60, split.lateFromMinute)
    }

    @Test
    fun `too few timed weigh-ins says nothing`() {
        assertNull(splitLog(each = 5).weighInTimeSplit())
    }

    @Test
    fun `a weigh-in with no time does not count towards the floor`() {
        val untimed = (0 until 20).map { entry(21_000L + it, 76.0, minute = null) }
        assertNull((splitLog(each = 5) + untimed).weighInTimeSplit())
    }

    @Test
    fun `a side thinner than the floor says nothing`() {
        // Nine mornings against three evenings: twelve readings, but one side is three deep.
        val lopsided = List(9) { entry(20_000L + it, 76.0, 7 * 60) } +
            List(3) { entry(20_100L + it, 77.5, 19 * 60) }
        assertNull(lopsided.weighInTimeSplit())
    }

    @Test
    fun `times ninety minutes apart are one habit, not two`() {
        assertNull(splitLog(earlyMinute = 7 * 60, lateMinute = 8 * 60 + 30).weighInTimeSplit())
    }

    @Test
    fun `a gap small enough to be water says nothing`() {
        assertNull(splitLog(earlyKg = 76.0, lateKg = 76.2).weighInTimeSplit())
    }

    @Test
    fun `an evening lighter than the morning reports a negative delta`() {
        val split = splitLog(earlyKg = 77.0, lateKg = 76.0).weighInTimeSplit()
        requireNotNull(split)
        assertTrue(split.deltaKg < 0)
    }

    /** Provenance is not the question: a scale's own record carries the hour the user stood on it,
     * which is exactly what this measures. */
    @Test
    fun `imported weigh-ins count`() {
        val imported = splitLog().map { it.copy(note = NOTE_GOOGLE_HEALTH) }
        assertNotNull(imported.weighInTimeSplit())
    }

    /** Every reading at the same minute: the median lands on it, the tie-flip runs, and one side
     * comes out empty — which the spread floor catches rather than a divide by zero. */
    @Test
    fun `one habit at a single minute says nothing`() {
        val same = List(14) { entry(20_000L + it, 76.0 + it * 0.1, 7 * 60) }
        assertNull(same.weighInTimeSplit())
    }

    @Test
    fun `an empty log says nothing`() {
        assertNull(emptyList<WeightEntry>().weighInTimeSplit())
    }
}
