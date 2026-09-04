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

    @Test
    fun `with no foods of the user's own, search is exactly the built-in list`() {
        assertEquals(searchCommonFoods("chicken"), searchFoods("chicken", emptyList()))
        assertEquals(COMMON_FOODS, searchFoods("", emptyList()))
    }

    /** The roadmap's named check: one food, one answer. */
    @Test
    fun `a food of the user's own replaces the built-in row of the same name`() {
        val mine = ScannedProduct("  chicken breast, COOKED  ", 100.0, "g", 190, 35, 0, 5)

        val hits = searchFoods("chicken", listOf(mine))

        assertEquals(mine, hits.first())
        assertEquals(0, hits.count { it.name.trim().equals("chicken breast, cooked", ignoreCase = true) && it !== mine })
        assertTrue(hits.any { it.name == "Chicken thigh, cooked" })
    }

    @Test
    fun `the user's own foods are filtered by the query too`() {
        val mine = listOf(
            ScannedProduct("Mum's adobo", 1.0, "serving", 420, 28, 12, 28),
            ScannedProduct("Whey, chocolate", 30.0, "g", 120, 24, 3, 1),
        )

        assertEquals(listOf(mine[0]), searchFoods("adobo", mine))
        assertEquals(mine, searchFoods("", mine).take(2))
        assertEquals(emptyList<ScannedProduct>(), searchFoods("zzzz", mine))
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
