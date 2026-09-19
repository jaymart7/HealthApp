package ph.mart.healthapp.core.data.supplement

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
}
