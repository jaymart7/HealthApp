package ph.mart.healthapp.core.data.food

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NutrientsTest {

    private val day = Nutrients(
        fiberG = 12,
        sugarG = 40,
        sodiumMg = 1240,
        vitaminDUg = 4,
        calciumMg = 420,
        ironUg = 6200,
        potassiumMg = 1480,
    )

    @Test
    fun `plus adds every field`() {
        val summed = day + day
        assertEquals(24, summed.fiberG)
        assertEquals(2480, summed.sodiumMg)
        assertEquals(12_400, summed.ironUg)
        assertEquals(2960, summed.potassiumMg)
    }

    @Test
    fun `div means, integer division like the macros do`() {
        val mean = (day + day + day) / 3
        assertEquals(12, mean.fiberG)
        assertEquals(420, mean.calciumMg)
    }

    /** Every caller guards on an empty list first, so this is the shape of a bug rather than a
     * case — it must not take a chart down with it. */
    @Test
    fun `div by zero is empty rather than a crash`() {
        assertTrue((day / 0).isEmpty)
    }

    /** Rounded, not truncated: a doubled 1 mg that stayed 1 mg is the bug this exists to stop. */
    @Test
    fun `times rounds each field`() {
        val doubled = Nutrients(fiberG = 1, ironUg = 3) * 2.0
        assertEquals(2, doubled.fiberG)
        assertEquals(6, doubled.ironUg)

        val halved = Nutrients(fiberG = 3) * 0.5
        assertEquals(2, halved.fiberG)
    }

    @Test
    fun `isEmpty is what stops the panel drawing on a quick-add day`() {
        assertTrue(Nutrients().isEmpty)
        assertFalse(Nutrients(sodiumMg = 1).isEmpty)
    }

    /** Fiber, sugar and sodium have been filled by the built-in food list for years. Counting them
     * as coverage would report a full house for a day with no vitamin figure in it. */
    @Test
    fun `hasMicronutrients ignores the three that predate the panel`() {
        assertFalse(Nutrients(fiberG = 9, sugarG = 40, sodiumMg = 1240).hasMicronutrients)
        assertTrue(Nutrients(calciumMg = 1).hasMicronutrients)
        assertTrue(Nutrients(ironUg = 1).hasMicronutrients)
    }

    /** Iron is stored in micrograms and read in milligrams; everything else prints its own unit,
     * grouped where it runs into the thousands. */
    @Test
    fun `formatNutrient converts iron and groups the large figures`() {
        assertEquals("12 g", formatNutrient(Nutrient.Fiber, 12))
        assertEquals("1,240 mg", formatNutrient(Nutrient.Sodium, 1240))
        assertEquals("15 µg", formatNutrient(Nutrient.VitaminD, 15))
        assertEquals("6.2 mg", formatNutrient(Nutrient.Iron, 6200))
        assertEquals("18.0 mg", formatNutrient(Nutrient.Iron, 18_000))
        assertEquals("0.4 mg", formatNutrient(Nutrient.Iron, 400))
    }

    @Test
    fun `valueOf reads every nutrient off the value type`() {
        assertEquals(
            listOf(12, 40, 1240, 4, 420, 6200, 1480),
            Nutrient.entries.map { day.valueOf(it) },
        )
    }
}
