package ph.mart.healthapp.core.data.cycle

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Everything in `Cycle.kt` is a fold over logged days, and every figure the feature shows rests on
 * [periods] getting the run boundaries right: a period split in two by one missed day moves every
 * cycle length after it, and with it the prediction the card exists to print.
 */
class CycleTest {

    private fun flow(vararg days: Long) = days.map { CycleDay(it, FlowLevel.Medium.value) }

    @Test
    fun `a missed day inside a period does not split it`() {
        // 100, (101 missed), 102 — one period, the gap PERIOD_GAP_DAYS tolerates.
        val periods = flow(100, 102, 103).periods()
        assertEquals(listOf(CyclePeriod(100, 103)), periods)
        assertEquals(4, periods.single().lengthDays)
    }

    @Test
    fun `two missed days start a new period`() {
        assertEquals(
            listOf(CyclePeriod(100, 100), CyclePeriod(103, 104)),
            flow(100, 103, 104).periods(),
        )
    }

    @Test
    fun `a symptom without flow is not a period day`() {
        val days = flow(100, 101) + CycleDay(97, flow = 0, symptoms = setOf(CycleSymptom.Cramps))
        // A cramp three days early would otherwise start the period on 97 and shorten the cycle.
        assertEquals(listOf(CyclePeriod(100, 101)), days.periods())
    }

    @Test
    fun `N periods yield N-1 cycle lengths`() {
        val periods = flow(100, 101, 128, 129, 158, 159).periods()
        assertEquals(3, periods.size)
        assertEquals(listOf(28, 30), periods.cycleLengths())
    }

    @Test
    fun `one period is not a cycle, so there is no prediction`() {
        assertNull(flow(100, 101).periods().cyclePrediction())
    }

    @Test
    fun `the prediction averages the last cycles and counts from the latest start`() {
        val prediction = flow(100, 128, 158).periods().cyclePrediction()!!
        assertEquals(29, prediction.averageCycleDays) // (28 + 30) / 2
        assertEquals(158L + 29, prediction.nextStartEpochDay)
        assertEquals(2, prediction.basedOnCycles)
        assertEquals(-3, prediction.daysAway(todayEpochDay = 190))
    }

    @Test
    fun `the prediction window stops at PREDICTION_WINDOW_CYCLES`() {
        // Eight starts 30 days apart, then one 100-day gap at the very beginning that must not
        // reach the average.
        val starts = listOf(0L) + (1..8).map { 100L + it * 30 }
        val prediction = starts.flatMap { flow(it) }.periods().cyclePrediction()!!
        assertEquals(PREDICTION_WINDOW_CYCLES, prediction.basedOnCycles)
        assertEquals(30, prediction.averageCycleDays)
    }

    @Test
    fun `the cycle day number counts from the last start on or before today`() {
        val periods = flow(100, 101, 128).periods()
        assertEquals(1, periods.cycleDayNumber(128))
        assertEquals(15, periods.cycleDayNumber(142))
        // Before anything was logged there is no cycle to be on day one of.
        assertNull(periods.cycleDayNumber(99))
    }

    @Test
    fun `a running period is left out of the period-length average`() {
        // A finished 4-day period, and a 2-day one that today is still inside.
        val days = flow(100, 101, 102, 103) + flow(130, 131)
        val averages = days.cycleAverages(todayEpochDay = 131)
        assertEquals(4.0, averages.periodDays!!, 0.001)
        assertEquals(30.0, averages.cycleDays!!, 0.001)
        assertEquals(6, averages.daysLogged)
    }

    @Test
    fun `each average keeps its own denominator`() {
        // One period only: no cycle length to average, but a length of its own once it is over.
        val averages = flow(100, 101).cycleAverages(todayEpochDay = 200)
        assertNull(averages.cycleDays)
        assertEquals(2.0, averages.periodDays!!, 0.001)
    }

    @Test
    fun `a symptom-only day counts as logged`() {
        val day = CycleDay(100, flow = 0, symptoms = setOf(CycleSymptom.Fatigue))
        assertEquals(1, listOf(day).cycleAverages(todayEpochDay = 100).daysLogged)
    }

    @Test
    fun `an unknown symptom name is dropped rather than throwing`() {
        // A tag retired in a later build must not make the whole row unreadable.
        assertEquals(
            setOf(CycleSymptom.Cramps, CycleSymptom.Acne),
            cycleSymptoms("Cramps,Ovulation,Acne"),
        )
    }

    @Test
    fun `symptoms round-trip in declaration order whatever order they were tapped`() {
        val tapped = setOf(CycleSymptom.Acne, CycleSymptom.Cramps)
        assertEquals("Cramps,Acne", encodeCycleSymptoms(tapped))
        assertEquals(tapped, cycleSymptoms(encodeCycleSymptoms(tapped)))
    }
}
