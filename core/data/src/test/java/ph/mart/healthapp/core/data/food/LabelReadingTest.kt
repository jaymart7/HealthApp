package ph.mart.healthapp.core.data.food

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The judgement and the three unit conversions behind a label scan — everything in that path a JVM
 * test can reach. [parseLabelReading] cannot be one of them: `org.json` is stubbed here, which is
 * exactly why these functions live in `LabelReading.kt` rather than beside the parser.
 */
class LabelReadingTest {

    @Test
    fun `a panel with nothing on it is not readable`() {
        assertFalse(LabelReading().readable())
    }

    @Test
    fun `a name alone is the front of the pack, not the panel`() {
        assertFalse(LabelReading(name = "Nutella", servingSize = "15 g").readable())
    }

    @Test
    fun `one figure is enough to be worth showing`() {
        assertTrue(LabelReading(calories = 539).readable())
        assertTrue(LabelReading(fatG = 31).readable())
        assertTrue(LabelReading(nutrients = Nutrients(sodiumMg = 41)).readable())
    }

    @Test
    fun `sodium is preferred over salt when both somehow arrive`() {
        assertEquals(41, sodiumMgFrom(sodiumMg = 41, saltG = 0.107))
    }

    @Test
    fun `salt converts to sodium at the label factor`() {
        // 1 g of salt is 400 mg of sodium — 1 ÷ 2.5 g, in milligrams.
        assertEquals(400, sodiumMgFrom(sodiumMg = null, saltG = 1.0))
        assertEquals(43, sodiumMgFrom(sodiumMg = null, saltG = 0.107))
    }

    @Test
    fun `no sodium and no salt is unknown, which this app stores as zero`() {
        assertEquals(0, sodiumMgFrom(sodiumMg = null, saltG = null))
    }

    @Test
    fun `vitamin D converts from international units at forty to the microgram`() {
        assertEquals(5, vitaminDUgFrom(vitaminDUg = null, vitaminDIu = 200))
        assertEquals(2, vitaminDUgFrom(vitaminDUg = 2.4, vitaminDIu = null))
        assertEquals(0, vitaminDUgFrom(vitaminDUg = null, vitaminDIu = null))
    }

    @Test
    fun `iron goes in as milligrams and is stored as micrograms`() {
        // The reason Nutrients gives for storing it that way: 0.4 mg at Int milligrams is nothing.
        assertEquals(1200, ironUgFrom(ironMg = 1.2))
        assertEquals(400, ironUgFrom(ironMg = 0.4))
        assertEquals(0, ironUgFrom(ironMg = null))
    }
}
