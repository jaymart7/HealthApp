package ph.mart.healthapp.core.data.food

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecipeParseTest {

    private fun food(name: String, calories: Int, sodiumMg: Int = 0) = RecognizedFood(
        name = name,
        portionAmount = 100.0,
        portionUnit = "g",
        calories = calories,
        proteinG = 0,
        carbsG = 0,
        fatG = 0,
        nutrients = Nutrients(sodiumMg = sodiumMg),
        confidence = RecognitionConfidence.High,
    )

    private fun parsed(result: RecipeParseResult) = result as RecipeParseResult.Parsed

    @Test
    fun `names are cleaned and a blank one is not an ingredient`() {
        val result = parsed(recipeParseResult("**Chili** ", 4, listOf(food(" **Beef** ", 250), food("  ", 90))))

        assertEquals("Chili", result.name)
        assertEquals(listOf("Beef"), result.items.map { it.name })
    }

    /** The one place this boundary parts from `loggable()`: salt is 0 kcal and all sodium. */
    @Test
    fun `a zero-calorie ingredient is kept with its sodium`() {
        val result = parsed(recipeParseResult("Soup", 2, listOf(food("Salt", 0, sodiumMg = 2300))))

        assertEquals(2300, result.items.single().nutrients.sodiumMg)
    }

    @Test
    fun `the list is capped`() {
        val many = (1..30).map { food("Item $it", 10) }

        assertEquals(MAX_RECIPE_INGREDIENTS, parsed(recipeParseResult("Stew", 4, many)).items.size)
    }

    @Test
    fun `servings are clamped rather than trusted`() {
        assertEquals(1, parsed(recipeParseResult("Stew", 0, listOf(food("Beef", 250)))).servings)
        assertEquals(MAX_RECIPE_SERVINGS, parsed(recipeParseResult("Stew", 500, listOf(food("Beef", 250)))).servings)
    }

    @Test
    fun `no ingredients is nothing found`() {
        assertEquals(RecipeParseResult.NothingFound, recipeParseResult("Stew", 4, emptyList()))
        assertTrue(recipeParseResult(null, 4, listOf(food("", 10))) is RecipeParseResult.NothingFound)
    }
}
