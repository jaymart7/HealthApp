package ph.mart.healthapp.core.data.bloodpressure

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import ph.mart.healthapp.core.data.epochDayStartMillis
import ph.mart.healthapp.core.data.progress.ChartRange
import ph.mart.healthapp.core.data.todayEpochDay

class BloodPressureTest {

    // Captured once, so nothing here disagrees with itself if the clock crosses midnight mid-run.
    private val today = todayEpochDay()

    /** 09:00 on the day in question — a real timestamp, since the day is derived from it. */
    private fun reading(daysAgo: Int, systolic: Int, diastolic: Int, pulse: Int = 0) =
        BloodPressureReading(
            takenAtMillis = epochDayStartMillis(today - daysAgo) + 9 * 3_600_000L,
            systolic = systolic,
            diastolic = diastolic,
            pulseBpm = pulse,
        )

    @Test
    fun `the worse of the two numbers picks the category`() {
        // The one a normal-first chain gets wrong: a crisis systolic under a healthy diastolic.
        assertEquals(BloodPressureCategory.Crisis, categoryOf(185, 70))
        assertEquals(BloodPressureCategory.Crisis, categoryOf(119, 121))
        assertEquals(BloodPressureCategory.Stage2, categoryOf(128, 95))
        assertEquals(BloodPressureCategory.Stage1, categoryOf(119, 80))
    }

    @Test
    fun `each band starts where the one below it stops`() {
        assertEquals(BloodPressureCategory.Normal, categoryOf(119, 79))
        assertEquals(BloodPressureCategory.Elevated, categoryOf(120, 79))
        assertEquals(BloodPressureCategory.Stage1, categoryOf(130, 79))
        assertEquals(BloodPressureCategory.Stage2, categoryOf(140, 79))
        assertEquals(BloodPressureCategory.Crisis, categoryOf(181, 79))
    }

    @Test
    fun `a day folds to the mean of its own readings, and days with none are absent`() {
        val days = listOf(
            reading(daysAgo = 3, systolic = 120, diastolic = 80),
            reading(daysAgo = 3, systolic = 140, diastolic = 90),
            reading(daysAgo = 0, systolic = 100, diastolic = 70),
        ).byDay()

        // Two rows, not four: the days in between have no reading, and the chart draws that gap.
        assertEquals(2, days.size)
        assertEquals(today - 3, days.first().dateEpochDay)
        assertEquals(130, days.first().systolic)
        assertEquals(85, days.first().diastolic)
        assertEquals(2, days.first().readings)
        assertEquals(today, days.last().dateEpochDay)
    }

    @Test
    fun `averages weigh each day once, not each reading`() {
        val averages = listOf(
            reading(daysAgo = 3, systolic = 120, diastolic = 80),
            reading(daysAgo = 3, systolic = 140, diastolic = 80),
            reading(daysAgo = 0, systolic = 100, diastolic = 60),
        ).averages()

        // Mean of the days: (130 + 100) / 2. A mean of the readings would report 120.
        assertEquals(115, averages.systolic)
        assertEquals(70, averages.diastolic)
        assertEquals(3, averages.readings)
    }

    @Test
    fun `the pulse keeps its own denominator and skips the readings that carry none`() {
        val averages = listOf(
            reading(daysAgo = 2, systolic = 120, diastolic = 80, pulse = 60),
            reading(daysAgo = 1, systolic = 120, diastolic = 80),
            reading(daysAgo = 0, systolic = 120, diastolic = 80, pulse = 80),
        ).averages()

        // 70, not 46 — a reading with no pulse is not a pulse of zero.
        assertEquals(70, averages.pulseBpm)
        assertEquals(3, averages.readings)
    }

    @Test
    fun `an empty window averages to nothing rather than to zero`() {
        val averages = emptyList<BloodPressureReading>().averages()
        assertNull(averages.systolic)
        assertNull(averages.diastolic)
        assertNull(averages.pulseBpm)
        assertEquals(0, averages.readings)
    }

    @Test
    fun `the window anchors to today, not to the last reading`() {
        val stale = listOf(reading(daysAgo = 100, systolic = 120, diastolic = 80))
        // Anchored to the newest entry the way the weight chart is, a "1M" window would still be
        // showing a reading from three months ago.
        assertEquals(emptyList<BloodPressureReading>(), stale.inRange(ChartRange.OneMonth, today))
        assertEquals(1, stale.inRange(ChartRange.OneYear, today).size)
    }

    @Test
    fun `the day is derived from the timestamp`() {
        assertEquals(today - 5, reading(daysAgo = 5, systolic = 120, diastolic = 80).dateEpochDay)
    }

    @Test
    fun `a reading formats the one way the UI renders it`() {
        assertEquals("128/82", formatBloodPressure(128, 82))
    }
}
