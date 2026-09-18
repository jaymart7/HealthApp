package ph.mart.healthapp.feature.profile.ui.library

import org.junit.Assert.assertEquals
import org.junit.Test
import ph.mart.healthapp.core.data.food.Recipe
import ph.mart.healthapp.core.data.food.SavedMeal
import ph.mart.healthapp.core.data.food.SavedMealItem
import ph.mart.healthapp.core.data.food.ScannedProduct

/**
 * The half of search worth asserting. The field, the highlight and the counts are Compose; the
 * predicate under them is a fold over three lists, and it is the thing that decides whether a
 * section keeps its header.
 */
class LibrarySearchTest {

    private fun item(name: String) = SavedMealItem(name, 100.0, "g", 100, 0, 0, 0)

    private val library = FoodLibraryUiState(
        myFoods = listOf(
            ScannedProduct("Mum's adobo", 1.0, "serving", 420, 28, 12, 28),
            ScannedProduct("Oat milk", 100.0, "ml", 46, 1, 7, 2),
        ),
        savedMeals = listOf(
            // The name says nothing about oats; the contents line does.
            SavedMeal(id = 1, name = "Usual breakfast", items = listOf(item("Greek yogurt"), item("Oats"))),
            SavedMeal(id = 2, name = "Post-gym shake", items = listOf(item("Whey shake"))),
        ),
        recipes = listOf(Recipe(id = 3, name = "Chili", servings = 4, items = listOf(item("Beef mince")))),
    )

    @Test
    fun `an empty query is the whole library`() {
        val all = library.filter("")
        assertEquals(2, all.myFoods.size)
        assertEquals(2, all.savedMeals.size)
        assertEquals(1, all.recipes.size)
        assertEquals(library.total, all.total)
    }

    @Test
    fun `a query matches a contents line whose name misses`() {
        val hits = library.filter("oat")
        assertEquals(listOf("Oat milk"), hits.myFoods.map { it.name })
        assertEquals(listOf("Usual breakfast"), hits.savedMeals.map { it.name })
    }

    @Test
    fun `matching ignores case`() {
        assertEquals(1, library.filter("CHILI").recipes.size)
        assertEquals(1, library.filter("chili").recipes.size)
    }

    /** An empty section is what makes its header disappear — the same rule the screen already
     * applies to a section that was empty to begin with. */
    @Test
    fun `a section with no matches comes back empty`() {
        val hits = library.filter("oat")
        assertEquals(0, hits.recipes.size)
        assertEquals(2, hits.total)
    }

    @Test
    fun `surrounding whitespace is not part of the query`() {
        assertEquals(library.filter("chili").total, library.filter("  chili  ").total)
        assertEquals(library.total, library.filter("   ").total)
    }
}
