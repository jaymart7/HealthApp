package ph.mart.healthapp.core.data.food

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * [servingGrams] is the whole of the third preset chip: it decides whether a food has a natural
 * serving at all, and what amount the chip sets. Sources write a serving every way a label does,
 * so the cases below are the shapes actually seen off Open Food Facts and FoodData Central.
 */
class ServingSizeTest {

    @Test
    fun `a bare gram figure is the serving`() {
        assertEquals(30.0, servingGrams("30 g"))
        assertEquals(30.0, servingGrams("30g"))
    }

    @Test
    fun `the parenthesised weight wins over the leading count`() {
        // "1 bar (25 g)" is 25 grams of food, not 1 — the last figure is the one that is a weight.
        assertEquals(25.0, servingGrams("1 bar (25 g)"))
        assertEquals(30.0, servingGrams("2 cookies (30g)"))
    }

    @Test
    fun `a decimal serving survives either separator`() {
        assertEquals(12.5, servingGrams("12.5 g"))
        assertEquals(12.5, servingGrams("12,5 g"))
    }

    @Test
    fun `a serving with no grams in it has none to find`() {
        // Guessing that a cup is 240 g would be an invented number.
        assertNull(servingGrams("1 cup (240 ml)"))
        assertNull(servingGrams("1 slice"))
        assertNull(servingGrams(null))
        assertNull(servingGrams(""))
    }

    @Test
    fun `a gram symbol inside a longer unit is not a gram figure`() {
        assertNull(servingGrams("400 mg"))
        assertNull(servingGrams("2 gallon"))
    }

    @Test
    fun `a zero serving is no serving`() {
        assertNull(servingGrams("0 g"))
    }
}
