package ph.mart.healthapp.feature.profile.ui.library

import org.junit.Assert.assertEquals
import org.junit.Test
import ph.mart.healthapp.core.data.food.Recipe
import ph.mart.healthapp.core.data.food.SavedMeal
import ph.mart.healthapp.core.data.food.SavedMealItem
import ph.mart.healthapp.core.data.food.ScannedProduct

/**
 * The half of the list worth asserting. The chips, the field and the highlight are Compose; the
 * fold under them decides which rows show and in what order.
 */
class LibrarySearchTest {

    private fun item(name: String) = SavedMealItem(name, 100.0, "g", 100, 0, 0, 0)

    private val library = FoodLibraryUiState(
        myFoods = listOf(
            ScannedProduct("Oat milk", 100.0, "ml", 46, 1, 7, 2),
            ScannedProduct("Mum's adobo", 1.0, "serving", 420, 28, 12, 28),
        ),
        savedMeals = listOf(
            SavedMeal(id = 1, name = "Usual breakfast", items = listOf(item("Greek yogurt"), item("Oats"))),
            SavedMeal(id = 2, name = "post-gym shake", items = listOf(item("Whey shake"))),
        ),
        recipes = listOf(Recipe(id = 3, name = "Chili", servings = 4, items = listOf(item("Beef mince")))),
    )

    private fun names(filter: LibraryFilter, query: String = "") = library.entries(filter, query).map { it.name }

    @Test
    fun `everything is one list, A to Z, whatever its kind and case`() {
        assertEquals(
            listOf("Chili", "Mum's adobo", "Oat milk", "post-gym shake", "Usual breakfast"),
            names(LibraryFilter.All),
        )
    }

    @Test
    fun `a chip keeps only its kind`() {
        assertEquals(listOf("Mum's adobo", "Oat milk"), names(LibraryFilter.Foods))
        assertEquals(listOf("Chili"), names(LibraryFilter.Recipes))
        assertEquals(listOf("post-gym shake", "Usual breakfast"), names(LibraryFilter.Meals))
    }

    @Test
    fun `search matches names only, ignoring case and surrounding space`() {
        assertEquals(listOf("Oat milk"), names(LibraryFilter.All, "  OAT "))
        // "Usual breakfast" holds oats, but the row no longer says so, so it is not a hit.
        assertEquals(emptyList<String>(), names(LibraryFilter.Meals, "oat"))
    }

    @Test
    fun `a saved meal and a recipe never share a key with a food of the same name`() {
        val keys = library.entries(LibraryFilter.All, "").map { it.key }
        assertEquals(keys.size, keys.toSet().size)
    }

    @Test
    fun `chips are drawn only when there is more than one kind to choose between`() {
        assertEquals(
            listOf(LibraryFilter.All, LibraryFilter.Foods, LibraryFilter.Recipes, LibraryFilter.Meals),
            library.filters(),
        )
        assertEquals(emptyList<LibraryFilter>(), library.copy(savedMeals = emptyList(), recipes = emptyList()).filters())
        assertEquals(
            listOf(LibraryFilter.All, LibraryFilter.Foods, LibraryFilter.Meals),
            library.copy(recipes = emptyList()).filters(),
        )
    }
}
