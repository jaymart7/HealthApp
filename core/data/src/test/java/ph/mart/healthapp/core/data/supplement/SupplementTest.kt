package ph.mart.healthapp.core.data.supplement

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import ph.mart.healthapp.core.data.progress.ChartRange

class SupplementTest {

    private fun supplement(id: Long, timesPerDay: Int = 1) =
        Supplement(id = id, name = "S$id", timesPerDay = timesPerDay)

    @Test
    fun `the header counts completed supplements, not doses taken`() {
        val today = listOf(
            SupplementToday(supplement(1), taken = 1),
            SupplementToday(supplement(2, timesPerDay = 2), taken = 1),
            SupplementToday(supplement(3), taken = 0),
        )
        assertEquals(1, today.completedCount)
    }

    @Test
    fun `a tap steps up and wraps at the target`() {
        assertEquals(1, nextTaken(taken = 0, timesPerDay = 1))
        assertEquals(0, nextTaken(taken = 1, timesPerDay = 1))
        assertEquals(1, nextTaken(taken = 0, timesPerDay = 2))
        assertEquals(2, nextTaken(taken = 1, timesPerDay = 2))
        assertEquals(0, nextTaken(taken = 2, timesPerDay = 2))
    }

    /** The snapshot rule: the day is priced off its own `dueTimes`, so dropping a supplement from
     * twice daily to once can't retroactively turn a hit day into a missed one — or vice versa. */
    @Test
    fun `a day is scored against the target it carried, not today's`() {
        val days = listOf(
            SupplementDay(dateEpochDay = 10, supplementId = 1, taken = 2, dueTimes = 2),
            SupplementDay(dateEpochDay = 11, supplementId = 1, taken = 1, dueTimes = 2),
        )
        assertEquals(listOf(10L to 1f, 11L to 0.5f), days.adherenceByDay())
    }

    @Test
    fun `a day is a gap, not a zero, when nothing was due`() {
        val days = listOf(
            SupplementDay(dateEpochDay = 10, supplementId = 1, taken = 0, dueTimes = 1),
            SupplementDay(dateEpochDay = 12, supplementId = 1, taken = 1, dueTimes = 1),
        )
        // Day 11 has no row at all, so it is absent — day 10 is a real zero and stays.
        assertEquals(listOf(10L to 0f, 12L to 1f), days.adherenceByDay())
    }

    /** A mean of the days, not of the rows: a day holding six supplements is still one day. */
    @Test
    fun `the average weighs every day the same`() {
        val days = listOf(
            SupplementDay(10, supplementId = 1, taken = 1, dueTimes = 1),
            SupplementDay(10, supplementId = 2, taken = 1, dueTimes = 1),
            SupplementDay(10, supplementId = 3, taken = 1, dueTimes = 1),
            SupplementDay(11, supplementId = 1, taken = 0, dueTimes = 1),
        )
        assertEquals(0.5f, days.averageAdherence()!!, 0.0001f)
    }

    @Test
    fun `an empty window has no average`() {
        assertNull(emptyList<SupplementDay>().averageAdherence())
    }

    /** Anchored to today, so the gaps at the end of a sparse series survive the window. */
    @Test
    fun `the window is measured back from today`() {
        val today = 100L
        val days = listOf(
            SupplementDay(today - 40, supplementId = 1, taken = 1, dueTimes = 1),
            SupplementDay(today - 5, supplementId = 1, taken = 1, dueTimes = 1),
        )
        val windowed = days.inRange(ChartRange.OneMonth, today)
        assertEquals(1, windowed.size)
        assertEquals(today - 5, windowed.single().dateEpochDay)
        assertTrue(days.inRange(ChartRange.OneYear, today).size == 2)
    }
}
