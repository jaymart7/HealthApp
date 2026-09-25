package ph.mart.healthapp.core.data.fake

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import ph.mart.healthapp.core.data.food.RecipeParseResult

class FakeRecipeParseTest {

    @Test
    fun `a named dish with listed ingredients comes back as a recipe`() {
        val result = fakeRecipeParse("Chili for 4, beef, beans") as RecipeParseResult.Parsed

        assertEquals("Chili", result.name)
        assertEquals(4, result.servings)
        assertTrue(result.items.any { "beef" in it.name.lowercase() })
    }

    @Test
    fun `one known food with no yield comes back as a food`() {
        val result = fakeRecipeParse("banana") as RecipeParseResult.Parsed

        assertTrue(result.isFood)
        assertFalse((fakeRecipeParse("Chili for 4, beef, beans") as RecipeParseResult.Parsed).isFood)
    }
}
