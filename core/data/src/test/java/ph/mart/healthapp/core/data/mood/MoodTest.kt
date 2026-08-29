package ph.mart.healthapp.core.data.mood

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import ph.mart.healthapp.core.data.progress.ChartRange

class MoodTest {

    @Test
    fun `averages skip unset values and keep separate denominators`() {
        val days = listOf(
            MoodDay(dateEpochDay = 1, mood = 4, energy = 2),
            MoodDay(dateEpochDay = 2, mood = 2, energy = 0),
            MoodDay(dateEpochDay = 3, mood = 0, energy = 4),
        )
        val averages = days.moodAverages()
        assertEquals(3.0, averages.mood!!, 0.001)
        assertEquals(3.0, averages.energy!!, 0.001)
        assertEquals(3, averages.daysLogged)
    }

    @Test
    fun `a mood-only week reports no energy average rather than a halved one`() {
        val days = listOf(MoodDay(1, mood = 5, energy = 0), MoodDay(2, mood = 3, energy = 0))
        val averages = days.moodAverages()
        assertEquals(4.0, averages.mood!!, 0.001)
        assertNull(averages.energy)
        assertEquals(2, averages.daysLogged)
    }

    @Test
    fun `an empty list averages to nothing`() {
        val averages = emptyList<MoodDay>().moodAverages()
        assertNull(averages.mood)
        assertNull(averages.energy)
        assertEquals(0, averages.daysLogged)
    }

    @Test
    fun `range is anchored to today, not to the newest entry`() {
        val today = 20_000L
        // Newest entry is 100 days stale; a 1M window must come back empty rather than
        // re-centring itself on that entry the way the weight chart's inRange does.
        val stale = listOf(MoodDay(today - 120, 4, 4), MoodDay(today - 100, 3, 3))
        assertEquals(emptyList<MoodDay>(), stale.inRange(ChartRange.OneMonth, today))
    }

    @Test
    fun `range boundary is inclusive and 1Y keeps everything`() {
        val today = 20_000L
        val days = listOf(MoodDay(today - 31, 1, 1), MoodDay(today - 30, 2, 2), MoodDay(today, 3, 3))
        assertEquals(listOf(2, 3), days.inRange(ChartRange.OneMonth, today).map { it.mood })
        assertEquals(3, days.inRange(ChartRange.OneYear, today).size)
    }
}
