package ph.mart.healthapp.core.data.food

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** The two pure halves of the meal-ideas feature: the guard on what the model says, and the answer
 * given when it says nothing at all. The call between them parses with `org.json`, which is
 * stubbed on the JVM — the same reason `FoodRecognitionRepositoryImpl` has no test. */
class MealIdeaTest {

    private fun idea(name: String, calories: Int, proteinG: Int = 10) = MealIdea(
        name = name,
        portionAmount = 1.0,
        portionUnit = "serving",
        calories = calories,
        proteinG = proteinG,
        carbsG = 10,
        fatG = 5,
    )

    private fun suggestion(name: String, calories: Int, proteinG: Int) = FoodSuggestion(
        name = name,
        portionAmount = 100.0,
        portionUnit = "g",
        calories = calories,
        proteinG = proteinG,
        carbsG = 10,
        fatG = 5,
        isFavorite = false,
    )

    private fun recipe(name: String, servings: Int, itemKcal: Int, itemProteinG: Int) = Recipe(
        id = 1,
        name = name,
        servings = servings,
        items = listOf(
            SavedMealItem(
                name = "Ingredient",
                portionAmount = 100.0,
                portionUnit = "g",
                calories = itemKcal,
                proteinG = itemProteinG,
                carbsG = 10,
                fatG = 5,
            ),
        ),
    )

    @Test
    fun `an idea with no name or no calories is not a shorter idea`() {
        val kept = listOf(idea("", 300), idea("Omelette", 0), idea("Yogurt bowl", 250))
            .fitting(remainingKcal = 640)

        assertEquals(listOf("Yogurt bowl"), kept.map { it.name })
    }

    /** The header says how much is left; a card twice that size makes the screen a liar. Just over
     * is the right answer for a portion nobody weighs. */
    @Test
    fun `just over the budget stays, far over is dropped`() {
        val kept = listOf(idea("Snug", 700), idea("Feast", 1400)).fitting(remainingKcal = 640)

        assertEquals(listOf("Snug"), kept.map { it.name })
    }

    @Test
    fun `never more than three cards`() {
        val kept = List(6) { idea("Idea $it", 200) }.fitting(remainingKcal = 640)

        assertEquals(MAX_MEAL_IDEAS, kept.size)
    }

    @Test
    fun `a day already over target has room for nothing`() {
        val kept = listOf(idea("Anything", 200)).fitting(remainingKcal = -80)

        assertTrue(kept.isEmpty())
    }

    @Test
    fun `the local fallback keeps what fits and leads with protein`() {
        val ideas = localMealIdeas(
            suggestions = listOf(
                suggestion("Greek yogurt", calories = 150, proteinG = 20),
                suggestion("Croissant", calories = 300, proteinG = 6),
                suggestion("Roast dinner", calories = 900, proteinG = 60),
            ),
            recipes = emptyList(),
            remainingKcal = 400,
        )

        assertEquals(listOf("Greek yogurt", "Croissant"), ideas.map { it.name })
    }

    /** A recipe is offered at one serving, priced exactly as its own panel prices it — so picking
     * the idea and picking the recipe log the same row. */
    @Test
    fun `a recipe is priced per serving, not per pot`() {
        val ideas = localMealIdeas(
            suggestions = emptyList(),
            recipes = listOf(recipe("Chili", servings = 4, itemKcal = 1200, itemProteinG = 80)),
            remainingKcal = 400,
        )

        assertEquals(1, ideas.size)
        assertEquals(300, ideas[0].calories)
        assertEquals(20, ideas[0].proteinG)
        assertEquals(1.0, ideas[0].portionAmount, 0.0)
    }

    /** Day one: nothing logged, nothing saved. The screen has to be able to say so. */
    @Test
    fun `an empty pool is an empty answer`() {
        assertTrue(localMealIdeas(emptyList(), emptyList(), remainingKcal = 640).isEmpty())
    }
}
