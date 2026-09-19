package ph.mart.healthapp.core.data.supplement

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import ph.mart.healthapp.core.data.epochDayStartMillis
import ph.mart.healthapp.core.data.food.Nutrients
import ph.mart.healthapp.core.data.progress.ChartRange
import ph.mart.healthapp.core.data.todayEpochDay
import ph.mart.healthapp.core.data.weekdayIndex

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

    /** The anchor every schedule case below counts off. Found rather than hardcoded: an epoch day
     * is a *local* day here, so which one is a Monday depends on the running timezone. */
    private val monday = (0L..6L).first { weekdayIndex(it) == 0 }

    private val monWedFri = 0b0010101

    @Test
    fun `a narrowed schedule is due on its own days and no others`() {
        val supplement = supplement(1).copy(days = monWedFri)
        val due = (0..6).map { supplement.isDueOn(monday + it) }
        assertEquals(listOf(true, false, true, false, true, false, false), due)
    }

    @Test
    fun `the default schedule is due every day`() {
        val supplement = supplement(1)
        assertEquals(EVERY_DAY, supplement.days)
        assertTrue((0..6).all { supplement.isDueOn(monday + it) })
    }

    /** A mask of 0 is unreachable through the app, but an edited backup could carry one — it is
     * due on nothing rather than throwing or quietly meaning "every day" here. The repository is
     * what normalises it on the way in. */
    @Test
    fun `an empty mask is due on nothing`() {
        val supplement = supplement(1).copy(days = 0)
        assertFalse((0..6).any { supplement.isDueOn(monday + it) })
    }

    @Test
    fun `only a narrowed schedule spells its days out`() {
        assertEquals("", supplement(1).dayLabel())
        assertEquals("Mon · Wed · Fri", supplement(1).copy(days = monWedFri).dayLabel())
    }

    /**
     * The bug the schedule exists for. Two supplements, one daily and one Mon/Wed/Fri: on a
     * Tuesday only the daily one has a row, so ticking it is a full day — not the half it read as
     * when every supplement was seeded onto every date.
     */
    @Test
    fun `a day is scored against what was due on it, not the whole list`() {
        val tuesday = monday + 1
        val days = listOf(
            SupplementDay(monday, supplementId = 1, taken = 1, dueTimes = 1),
            SupplementDay(monday, supplementId = 2, taken = 1, dueTimes = 1),
            SupplementDay(tuesday, supplementId = 1, taken = 1, dueTimes = 1),
        )
        assertEquals(listOf(monday to 1f, tuesday to 1f), days.adherenceByDay())
    }

    private fun scanned(id: Long, vitaminDUg: Int = 0, calciumMg: Int = 0) = Supplement(
        id = id,
        name = "S$id",
        nutrients = Nutrients(vitaminDUg = vitaminDUg, calciumMg = calciumMg),
    )

    /** A dose is the unit: two doses of a 10 µg tablet is 20 µg, and two supplements on one day
     * sum. */
    @Test
    fun `a day carries what its doses carried`() {
        val days = listOf(
            SupplementDay(dateEpochDay = 10, supplementId = 1, taken = 2, dueTimes = 2),
            SupplementDay(dateEpochDay = 10, supplementId = 2, taken = 1, dueTimes = 1),
        )
        val supplements = listOf(scanned(1, vitaminDUg = 10), scanned(2, calciumMg = 500))
        val byDay = supplementNutrientsByDay(days, supplements)
        assertEquals(Nutrients(vitaminDUg = 20, calciumMg = 500), byDay.getValue(10))
    }

    /** A day nobody ticked, and a day of supplements nobody scanned, are both absent rather than
     * zero — the panel above them must not grade a blank as a shortfall. */
    @Test
    fun `a day with nothing to carry is absent, not zero`() {
        val days = listOf(
            SupplementDay(dateEpochDay = 10, supplementId = 1, taken = 0, dueTimes = 1),
            SupplementDay(dateEpochDay = 11, supplementId = 2, taken = 1, dueTimes = 1),
        )
        val byDay = supplementNutrientsByDay(days, listOf(scanned(1, vitaminDUg = 10), supplement(2)))
        assertTrue(byDay.isEmpty())
    }

    /** A supplement removed since still names what a past day carried, which is why the deleted
     * rows are read too. */
    @Test
    fun `a soft-deleted supplement still names a past day's figures`() {
        val days = listOf(SupplementDay(dateEpochDay = 10, supplementId = 1, taken = 1, dueTimes = 1))
        val deleted = scanned(1, vitaminDUg = 15).copy(deleted = true)
        assertEquals(
            Nutrients(vitaminDUg = 15),
            supplementNutrientsByDay(days, listOf(deleted)).getValue(10),
        )
    }

    /** An imported day pointing at an id the file no longer holds contributes nothing rather than
     * throwing into a chart. */
    @Test
    fun `a day pointing at an unknown supplement contributes nothing`() {
        val days = listOf(SupplementDay(dateEpochDay = 10, supplementId = 9, taken = 1, dueTimes = 1))
        assertTrue(supplementNutrientsByDay(days, listOf(scanned(1, vitaminDUg = 10))).isEmpty())
    }

    // ---- supplementsOn: the Progress page's catch-up checklist ----

    /** A supplement created a week ago, so `createdAt` never rules it out of a recent day. */
    private fun listed(id: Long, timesPerDay: Int = 1, days: Int = EVERY_DAY) = Supplement(
        id = id,
        name = "S$id",
        timesPerDay = timesPerDay,
        days = days,
        createdAt = epochDayStartMillis(todayEpochDay() - 7),
    )

    @Test
    fun `an existing row wins on both its count and its own due times`() {
        val date = todayEpochDay() - 2
        // Since dropped to once daily; the day it was taken twice must still read two of two.
        val supplement = listed(1, timesPerDay = 1)
        val rows = supplementsOn(
            date = date,
            supplements = listOf(supplement),
            days = listOf(SupplementDay(date, supplementId = 1, taken = 2, dueTimes = 2)),
        )
        assertEquals(listOf(SupplementOnDay(supplement, taken = 2, dueTimes = 2)), rows)
        assertTrue(rows.single().isComplete)
    }

    @Test
    fun `a day with no row is offered at zero against today's target`() {
        val date = todayEpochDay() - 2
        val supplement = listed(1, timesPerDay = 3)
        assertEquals(
            listOf(SupplementOnDay(supplement, taken = 0, dueTimes = 3)),
            supplementsOn(date, listOf(supplement), days = emptyList()),
        )
    }

    @Test
    fun `a supplement not due on that weekday is absent`() {
        val date = todayEpochDay() - 2
        // Due on every day but that one.
        val mask = EVERY_DAY and (1 shl weekdayIndex(date)).inv()
        assertTrue(supplementsOn(date, listOf(listed(1, days = mask)), emptyList()).isEmpty())
    }

    /** A row is evidence it was due, whatever the mask says now — narrowing a schedule must not
     * hide a tick the user already made. */
    @Test
    fun `a supplement off the schedule is kept where the day already has a row`() {
        val date = todayEpochDay() - 2
        val mask = EVERY_DAY and (1 shl weekdayIndex(date)).inv()
        val rows = supplementsOn(
            date = date,
            supplements = listOf(listed(1, days = mask)),
            days = listOf(SupplementDay(date, supplementId = 1, taken = 1, dueTimes = 1)),
        )
        assertEquals(1, rows.size)
    }

    @Test
    fun `a supplement created after the day is absent`() {
        val today = todayEpochDay()
        val added = Supplement(id = 1, name = "S1", createdAt = epochDayStartMillis(today))
        assertTrue(supplementsOn(today - 1, listOf(added), emptyList()).isEmpty())
        assertEquals(1, supplementsOn(today, listOf(added), emptyList()).size)
    }

    /** Only the day asked for — a row on the day either side must not leak into it. */
    @Test
    fun `rows from other days are ignored`() {
        val date = todayEpochDay() - 2
        val supplement = listed(1, timesPerDay = 1)
        val rows = supplementsOn(
            date = date,
            supplements = listOf(supplement),
            days = listOf(
                SupplementDay(date - 1, supplementId = 1, taken = 1, dueTimes = 1),
                SupplementDay(date + 1, supplementId = 1, taken = 1, dueTimes = 1),
            ),
        )
        assertEquals(0, rows.single().taken)
    }
}
