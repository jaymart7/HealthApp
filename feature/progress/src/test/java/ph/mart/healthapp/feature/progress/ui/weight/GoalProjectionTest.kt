package ph.mart.healthapp.feature.progress.ui.weight

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import ph.mart.healthapp.core.data.profile.Goal
import ph.mart.healthapp.core.data.progress.WeightEntry

private const val TODAY = 20_000L

/** Weekly weigh-ins ending today, newest last — the shape the repository emits. */
private fun weighIns(vararg weightsKg: Double, everyDays: Long = 7, endingDaysAgo: Long = 0) =
    weightsKg.mapIndexed { index, kg ->
        WeightEntry(TODAY - endingDaysAgo - (weightsKg.size - 1 - index) * everyDays, kg)
    }

private fun projection(
    entries: List<WeightEntry> = weighIns(78.0, 77.5, 77.0, 76.5),
    goalWeightKg: Double? = 72.0,
    goal: Goal? = Goal.Lose,
) = goalProjection(entries, goalWeightKg, goal, todayEpochDay = TODAY)

class GoalProjectionTest {

    @Test
    fun `no goal weight means no projection`() {
        assertNull(projection(goalWeightKg = null))
    }

    @Test
    fun `maintain has no direction to project in`() {
        assertNull(projection(goal = Goal.Maintain))
    }

    @Test
    fun `two weigh-ins are not enough to fit a rate`() {
        assertNull(projection(entries = weighIns(78.0, 76.5)))
    }

    @Test
    fun `three weigh-ins in one week are too narrow a window`() {
        assertNull(projection(entries = weighIns(78.0, 77.6, 77.2, everyDays = 2)))
    }

    @Test
    fun `a stale series projects nothing, however long it ran`() {
        // Four weekly weigh-ins, but the newest is ten days old — today is not on that line.
        assertNull(projection(entries = weighIns(78.0, 77.5, 77.0, 76.5, endingDaysAgo = 10)))
    }

    @Test
    fun `steady loss projects a date at the fitted rate`() {
        val result = projection()!!
        assertEquals(-0.5, result.kgPerWeek, 0.01)
        assertFalse(result.reached)
        // 4.5 kg to go at 0.5 kg/week is 63 days.
        assertEquals(TODAY + 63, result.targetEpochDay)
    }

    @Test
    fun `backdating a weigh-in cannot change the projection`() {
        val entries = weighIns(78.0, 77.5, 77.0, 76.5)
        assertEquals(projection(entries), projection(entries.reversed()))
    }

    @Test
    fun `the rate is reported even when the trend points away from the goal`() {
        val result = projection(entries = weighIns(76.5, 77.0, 77.5, 78.0))!!
        assertNull(result.targetEpochDay)
        assertEquals(0.5, result.kgPerWeek, 0.01)
    }

    @Test
    fun `a flat trend gives no date rather than one years out`() {
        val result = projection(entries = weighIns(76.5, 76.52, 76.48, 76.5))!!
        assertNull(result.targetEpochDay)
    }

    @Test
    fun `at the goal reports arrival, not a date`() {
        val result = projection(entries = weighIns(73.0, 72.6, 72.3, 71.9))!!
        assertTrue(result.reached)
        assertNull(result.targetEpochDay)
    }

    @Test
    fun `a crawl toward a distant goal stays inside the year horizon or reports no date`() {
        // 26 kg to go at 0.1 kg/week is roughly five years.
        val result = projection(entries = weighIns(98.1, 98.05, 98.0, 97.9))!!
        assertNull(result.targetEpochDay)
    }

    @Test
    fun `building projects upward off the same fit`() {
        val result = projection(
            entries = weighIns(70.0, 70.4, 70.8, 71.2),
            goalWeightKg = 75.0,
            goal = Goal.Build,
        )!!
        assertNotNull(result.targetEpochDay)
        assertTrue(result.kgPerWeek > 0)
        assertFalse(result.reached)
    }
}
