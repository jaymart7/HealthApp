package ph.mart.healthapp.core.data.food

import org.junit.Assert.assertEquals
import org.junit.Test
import ph.mart.healthapp.core.data.food.local.SavedMealEntity
import ph.mart.healthapp.core.data.food.local.SavedMealItemEntity

class RecipeTest {

    private fun parent(id: Long, name: String, servings: Int? = null) =
        SavedMealEntity(id = id, name = name, createdAt = 0, servings = servings)

    private fun item(
        id: Long,
        mealId: Long,
        name: String,
        calories: Int = 100,
        proteinG: Int = 10,
        carbsG: Int = 10,
        fatG: Int = 10,
    ) = SavedMealItemEntity(
        id = id,
        mealId = mealId,
        name = name,
        portionAmount = 100.0,
        portionUnit = "g",
        calories = calories,
        proteinG = proteinG,
        carbsG = carbsG,
        fatG = fatG,
    )

    private fun recipe(servings: Int, vararg items: SavedMealItem) =
        Recipe(id = 1, name = "Chili", servings = servings, items = items.toList())

    private fun ingredient(calories: Int, proteinG: Int = 0, carbsG: Int = 0, fatG: Int = 0) =
        SavedMealItem("Ingredient", 100.0, "g", calories, proteinG, carbsG, fatG)

    @Test
    fun `per serving divides fiber, sugar and sodium too`() {
        val dish = recipe(
            4,
            ingredient(400).copy(fiberG = 8, sugarG = 12, sodiumMg = 1200),
            ingredient(400).copy(fiberG = 4, sugarG = 4, sodiumMg = 600),
        )

        val serving = dish.perServing()

        assertEquals(3, serving.fiberG)
        assertEquals(4, serving.sugarG)
        assertEquals(450, serving.sodiumMg)
    }

    @Test
    fun `per serving divides the ingredient totals`() {
        val perServing = recipe(4, ingredient(400, 40, 80, 20), ingredient(400, 40, 80, 20)).perServing()
        assertEquals(200, perServing.calories)
        assertEquals(20, perServing.proteinG)
        assertEquals(40, perServing.carbsG)
        assertEquals(10, perServing.fatG)
    }

    @Test
    fun `per serving rounds rather than truncates`() {
        // 350 / 4 = 87.5 — integer division would say 87.
        assertEquals(88, recipe(4, ingredient(350)).perServing().calories)
        // 349 / 4 = 87.25 rounds down.
        assertEquals(87, recipe(4, ingredient(349)).perServing().calories)
    }

    @Test
    fun `one serving is the whole recipe`() {
        val single = recipe(1, ingredient(150, 20, 8, 4), ingredient(230, 8, 40, 4))
        assertEquals(380, single.perServing().calories)
        assertEquals(380, single.totalKcal())
    }

    @Test
    fun `zero servings is treated as one rather than dividing by zero`() {
        assertEquals(500, recipe(0, ingredient(500)).perServing().calories)
    }

    @Test
    fun `an empty recipe costs nothing`() {
        assertEquals(0, recipe(4).perServing().calories)
        assertEquals(0, recipe(4).totalKcal())
    }

    @Test
    fun `recipes and saved meals never take each other's items`() {
        val parents = listOf(parent(1, "Usual breakfast"), parent(2, "Chili", servings = 4))
        val items = listOf(item(1, 1, "Oats"), item(2, 2, "Beans"), item(3, 2, "Tomatoes"))

        val meals = groupSavedMeals(meals = listOf(parents[0]), items = items)
        val recipes = groupRecipes(recipes = listOf(parents[1]), items = items)

        assertEquals(listOf("Oats"), meals.single().items.map { it.name })
        assertEquals(listOf("Beans", "Tomatoes"), recipes.single().items.map { it.name })
        assertEquals(4, recipes.single().servings)
    }

    @Test
    fun `a parent with no servings is still readable as a one-serving recipe`() {
        val grouped = groupRecipes(recipes = listOf(parent(1, "Stray")), items = emptyList())
        assertEquals(1, grouped.single().servings)
    }
}
