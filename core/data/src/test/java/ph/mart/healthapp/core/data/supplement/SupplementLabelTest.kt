package ph.mart.healthapp.core.data.supplement

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import ph.mart.healthapp.core.data.food.Nutrients

/** The judgement [parseSupplementLabel] feeds. The parse itself is `org.json` and stubbed on the
 * JVM, which is the whole reason this half lives in its own file — see `SupplementLabelJson.kt`. */
class SupplementLabelTest {

    @Test
    fun `a panel with a figure this app grades is readable`() {
        val reading = SupplementLabelReading(nutrients = Nutrients(vitaminDUg = 50))
        assertTrue(reading.readable())
    }

    /** The case the food label's `readable()` cannot express: a B-complex declares nothing
     * [Nutrients] has a field for, and refusing it would tell the user their bottle is unreadable
     * when the app is simply unable to grade it. */
    @Test
    fun `a panel of nutrients this app cannot grade is still readable`() {
        val reading = SupplementLabelReading(panel = "Vitamin B12 2.4 µg\nVitamin B6 1.7 mg")
        assertTrue(reading.readable())
    }

    /** The front of the bottle: a brand read off it, nothing else. A sheet holding a name and no
     * figures is a worse answer than "point it at the panel". */
    @Test
    fun `a name on its own is not a panel`() {
        assertFalse(SupplementLabelReading(name = "Daily Multivitamin", dose = "2 tablets").readable())
    }

    @Test
    fun `an empty reading is not readable`() {
        assertFalse(SupplementLabelReading().readable())
    }

    /** What the scan's confirmation does: layered over a blank row, so the figures are the
     * reading's and the row is still an add. */
    @Test
    fun `a reading applied to a blank supplement is the reading`() {
        val reading = SupplementLabelReading(
            name = "Daily Multivitamin",
            dose = "2 tablets",
            timesPerDay = 2,
            nutrients = Nutrients(vitaminDUg = 25),
            panel = "Vitamin D 25 µg",
        )
        val applied = reading.appliedTo(Supplement(name = ""))
        assertEquals("Daily Multivitamin", applied.name)
        assertEquals("2 tablets", applied.dose)
        assertEquals(2, applied.timesPerDay)
        assertEquals(25, applied.nutrients.vitaminDUg)
        assertEquals("Vitamin D 25 µg", applied.panel)
        assertEquals(0L, applied.id)
    }

    /** The lookup's case: the row already exists, and filling in its figures must not renumber it
     * or rewrite the schedule the user set. */
    @Test
    fun `applying a reading keeps what identifies the row`() {
        val existing = Supplement(
            id = 7,
            name = "Multi",
            createdAt = 1_700_000_000_000,
            days = 0b0010101,
        )
        val applied = SupplementLabelReading(
            name = "Daily Multivitamin",
            nutrients = Nutrients(calciumMg = 210),
        ).appliedTo(existing)
        assertEquals(7L, applied.id)
        assertEquals(1_700_000_000_000, applied.createdAt)
        assertEquals(0b0010101, applied.days)
        assertEquals(210, applied.nutrients.calciumMg)
    }

    /** A panel that states no frequency has not answered the question, so whatever was already set
     * stands — the app's default of once on an add, the user's own figure on an edit. */
    @Test
    fun `a reading with no frequency leaves the existing one`() {
        val existing = Supplement(id = 1, name = "Creatine", dose = "5 g", timesPerDay = 3)
        val applied = SupplementLabelReading(panel = "Creatine 5 g").appliedTo(existing)
        assertEquals(3, applied.timesPerDay)
        assertEquals("Creatine", applied.name)
        assertEquals("5 g", applied.dose)
    }
}
