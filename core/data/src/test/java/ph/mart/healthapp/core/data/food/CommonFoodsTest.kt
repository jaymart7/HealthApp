package ph.mart.healthapp.core.data.food

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CommonFoodsTest {

    @Test
    fun `a blank query is every food, so the panel has something to page through`() {
        assertEquals(COMMON_FOODS, searchCommonFoods(""))
        assertEquals(COMMON_FOODS, searchCommonFoods("   "))
    }

    @Test
    fun `matching is a case-insensitive substring`() {
        val hits = searchCommonFoods("CHICKEN").map { it.name }

        assertTrue(hits.isNotEmpty())
        assertTrue(hits.all { it.contains("chicken", ignoreCase = true) })
        assertEquals(hits, searchCommonFoods(" chicken ").map { it.name })
    }

    @Test
    fun `nothing matched is empty, not the whole list`() {
        assertEquals(emptyList<ScannedProduct>(), searchCommonFoods("zzzz"))
    }

    /** The real guard on a hand-typed table: a row with no name or no calories is unloggable. */
    @Test
    fun `every food is named, priced per 100 g and unique`() {
        COMMON_FOODS.forEach { food ->
            assertTrue(food.name, food.name.isNotBlank())
            assertTrue(food.name, food.calories > 0)
            assertTrue(food.name, food.portionAmount == 100.0 && food.portionUnit == "g")
        }
        assertEquals(COMMON_FOODS.size, COMMON_FOODS.map { it.name.lowercase() }.toSet().size)
    }
}
