package ph.mart.healthapp.core.data.exercise

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import ph.mart.healthapp.core.data.todayEpochDay
import ph.mart.healthapp.core.data.weekdayIndex

private fun routine(name: String, days: Int) =
    Routine(id = 1, name = name, lifts = listOf(RoutineLift("Squat", 3, 5)), days = days)

private fun workout(day: Long) = ExerciseEntry(
    id = day,
    dateEpochDay = day,
    type = ExerciseType.Strength,
    minutes = 45,
    burnedKcal = 260,
    sets = listOf(StrengthSet("Squat", reps = 5, weightKg = 100.0)),
)

private fun cardio(day: Long) =
    ExerciseEntry(id = day, dateEpochDay = day, type = ExerciseType.Run, minutes = 30, burnedKcal = 300)

/**
 * Everything here is asserted **relative to today** rather than against a fixed epoch day: this
 * module's epoch day is local-midnight based, so which weekday day 20,000 falls on depends on the
 * machine's time zone. What must hold in every zone is that the days run in order and the week
 * starts on a Monday.
 */
class TrainingPlanTest {

    @Test
    fun `weekdays run Monday to Sunday and wrap`() {
        val today = todayEpochDay()
        val index = weekdayIndex(today)
        assertTrue(index in 0..6)
        assertEquals((index + 1) % 7, weekdayIndex(today + 1))
        assertEquals((index + 6) % 7, weekdayIndex(today - 1))
    }

    @Test
    fun `a week starts on the Monday on or before the day`() {
        val today = todayEpochDay()
        val monday = weekStart(today)
        assertEquals(0, weekdayIndex(monday))
        assertTrue(today - monday in 0..6)
    }

    @Test
    fun `a mask names its days, and toggling one flips only it`() {
        val monWedFri = routine("Push", days = 0b0010101)
        assertEquals("Mon · Wed · Fri", monWedFri.dayLabel())
        assertEquals("", routine("Unscheduled", days = 0).dayLabel())
        assertEquals(0b0010001, 0b0010101.toggleWeekday(2))
        assertEquals(0b0010101, 0b0010001.toggleWeekday(2))
    }

    @Test
    fun `only routines planned for that weekday are offered`() {
        val today = todayEpochDay()
        val todayBit = 1 shl weekdayIndex(today)
        val planned = routine("Today", days = todayBit)
        val other = routine("Not today", days = 1 shl ((weekdayIndex(today) + 1) % 7))
        assertEquals(listOf(planned), listOf(planned, other).plannedOn(today))
        assertTrue(listOf(planned, other).anyScheduled())
        assertFalse(listOf(routine("Unscheduled", days = 0)).anyScheduled())
    }

    @Test
    fun `the week is Monday to Sunday and today sits in it once`() {
        val today = todayEpochDay()
        val week = trainingWeek(routines = emptyList(), entries = emptyList(), todayEpochDay = today)
        assertEquals(7, week.size)
        assertEquals(weekStart(today), week.first().epochDay)
        assertEquals(listOf(today), week.filter { it.isToday }.map { it.epochDay })
    }

    @Test
    fun `a planned day counts only once it has a workout with sets`() {
        val today = todayEpochDay()
        val everyDay = routine("Daily", days = 0b1111111)
        val untrained = trainingWeek(listOf(everyDay), entries = listOf(cardio(today)), todayEpochDay = today)
        assertFalse(untrained.single { it.isToday }.trained)
        // A run is not a session this plan can claim — `withSets()` is the same discriminator the
        // Strength tab uses.
        assertEquals(0, untrained.trainedSoFar())

        val trained = trainingWeek(listOf(everyDay), entries = listOf(workout(today)), todayEpochDay = today)
        assertTrue(trained.single { it.isToday }.trained)
        assertEquals(1, trained.trainedSoFar())
    }

    @Test
    fun `days still to come are not counted as missed`() {
        val today = todayEpochDay()
        val everyDay = routine("Daily", days = 0b1111111)
        val week = trainingWeek(listOf(everyDay), entries = listOf(workout(today)), todayEpochDay = today)
        // Monday counts one elapsed day, Sunday counts seven — never the whole week from Monday.
        assertEquals(weekdayIndex(today) + 1, week.plannedSoFar())
    }
}
