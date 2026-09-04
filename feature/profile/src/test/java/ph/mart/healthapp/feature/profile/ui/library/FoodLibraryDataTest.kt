package ph.mart.healthapp.feature.profile.ui.library

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import ph.mart.healthapp.core.data.food.Recipe
import ph.mart.healthapp.core.data.food.SavedMeal
import ph.mart.healthapp.core.data.food.SavedMealItem
import ph.mart.healthapp.core.data.food.ScannedProduct

class FoodLibraryDataTest {

    private fun item(name: String, calories: Int) =
        SavedMealItem(name, 100.0, "g", calories, 0, 0, 0)

    private fun meal(vararg items: SavedMealItem) =
        SavedMeal(id = 1, name = "Usual breakfast", items = items.toList())

    @Test
    fun `a saved meal counts its items and totals their calories`() {
        assertEquals("2 items · 380 kcal", meal(item("Oats", 230), item("Yogurt", 150)).summary())
    }

    @Test
    fun `one item is not "1 items"`() {
        assertEquals("1 item · 230 kcal", meal(item("Oats", 230)).summary())
    }

    @Test
    fun `a recipe is priced per serving, not by its total`() {
        val chili = Recipe(
            id = 2,
            name = "Chili",
            servings = 4,
            items = listOf(item("Beef mince", 1100), item("Kidney beans", 380)),
        )

        // 1480 across four portions — the total is a number nobody eats in one sitting.
        assertEquals("370 kcal per serving · makes 4", chili.summary())
    }

    @Test
    fun `contents names the items`() {
        assertEquals("Oats, Yogurt", listOf(item("Oats", 230), item("Yogurt", 150)).contents())
    }

    @Test
    fun `no items means no third line at all, not a blank one`() {
        assertEquals("", emptyList<SavedMealItem>().contents())
    }

    @Test
    fun `a food says what amount its calories are for, without a trailing zero`() {
        assertEquals(
            "165 kcal · 100 g",
            ScannedProduct("Chicken breast", 100.0, "g", 165, 31, 0, 4).summary(),
        )
        assertEquals(
            "420 kcal · 1 serving",
            ScannedProduct("Adobo", 1.0, "serving", 420, 28, 12, 28).summary(),
        )
        assertEquals(
            "60 kcal · 0.5 cup",
            ScannedProduct("Rice", 0.5, "cup", 60, 1, 14, 0).summary(),
        )
    }

    @Test
    fun `a food's third line is its macros`() {
        assertEquals(
            "P 31g · C 0g · F 4g",
            ScannedProduct("Chicken breast", 100.0, "g", 165, 31, 0, 4).macroLine(),
        )
    }

    /** The empty state must not flash before the first Room emission lands. */
    @Test
    fun `an empty state is not "loaded"`() {
        assertFalse(FoodLibraryUiState().loaded)
        assertTrue(FoodLibraryUiState(savedMeals = listOf(meal(item("Oats", 230)))).loaded)
        assertTrue(
            FoodLibraryUiState(recipes = listOf(Recipe(1, "Chili", 4, emptyList()))).loaded,
        )
        assertTrue(
            FoodLibraryUiState(myFoods = listOf(ScannedProduct("Adobo", 1.0, "serving", 420, 28, 12, 28))).loaded,
        )
    }
}
