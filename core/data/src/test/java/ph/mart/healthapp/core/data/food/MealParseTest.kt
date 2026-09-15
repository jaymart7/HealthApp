package ph.mart.healthapp.core.data.food

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MealParseTest {

    private fun food(name: String, calories: Int) = RecognizedFood(
        name = name,
        portionAmount = 1.0,
        portionUnit = "serving",
        calories = calories,
        proteinG = 0,
        carbsG = 0,
        fatG = 0,
        confidence = RecognitionConfidence.High,
    )

    /** The photo flow judges one food rather than a list, so it reads [isLoggable] directly —
     * these three are what stop a named plate priced at zero seeding the confirmation form. */
    @Test
    fun `a named food with calories is loggable`() {
        assertTrue(food("Scrambled eggs", 156).isLoggable)
    }

    @Test
    fun `a named food priced at zero is not loggable`() {
        assertFalse(food("Chicken adobo", 0).isLoggable)
    }

    @Test
    fun `a nameless food is not loggable`() {
        assertFalse(food("  ", 156).isLoggable)
    }

    @Test
    fun `drops a nameless item and keeps the order of the rest`() {
        val parsed = listOf(food("Scrambled eggs", 156), food("  ", 90), food("Toast", 80))

        assertEquals(listOf("Scrambled eggs", "Toast"), parsed.loggable().map { it.name })
    }

    @Test
    fun `drops an item with no calories`() {
        val parsed = listOf(food("Black coffee", 0), food("Toast", 80))

        assertEquals(listOf("Toast"), parsed.loggable().map { it.name })
    }

    @Test
    fun `caps a runaway list at MAX_PARSED_FOODS`() {
        val parsed = (1..20).map { food("Food $it", 100) }

        assertEquals(MAX_PARSED_FOODS, parsed.loggable().size)
    }

    @Test
    fun `passes an ordinary meal through untouched`() {
        val parsed = listOf(food("Scrambled eggs", 156), food("Toast", 90), food("Latte", 120))

        assertEquals(parsed, parsed.loggable())
    }
}
