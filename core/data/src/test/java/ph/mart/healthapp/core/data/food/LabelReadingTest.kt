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

    /** The supplement sheet puts a stored figure back in a typable box, so the pair has to come
     * back where it started — a bottle printing 2000 IU must still read 2000 IU after a save. */
    @Test
    fun `vitamin D round-trips through international units`() {
        assertEquals(2000, vitaminDIuFrom(vitaminDUg = 50))
        assertEquals(50, vitaminDUgFrom(vitaminDUg = null, vitaminDIu = vitaminDIuFrom(50)))
        assertEquals(0, vitaminDIuFrom(vitaminDUg = 0))
    }

    /** Whole milligrams both ways, which is every iron supplement ever sold. The sub-milligram
     * case is the documented ceiling: 400 µg reads as nothing in a box measured in mg. */
    @Test
    fun `iron round-trips through milligrams, and rounds below one`() {
        assertEquals(18, ironMgFrom(ironUg = 18_000))
        assertEquals(18_000, ironUgFrom(ironMg = ironMgFrom(18_000).toDouble()))
        assertEquals(1, ironMgFrom(ironUg = 1200))
        assertEquals(0, ironMgFrom(ironUg = 400))
        assertEquals(0, ironMgFrom(ironUg = 0))
    }
}
