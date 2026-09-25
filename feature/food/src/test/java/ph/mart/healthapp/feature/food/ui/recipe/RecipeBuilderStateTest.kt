package ph.mart.healthapp.feature.food.ui.recipe

import androidx.compose.runtime.saveable.SaverScope
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import ph.mart.healthapp.core.data.food.SavedMealItem

class RecipeBuilderStateTest {

    private val beans = SavedMealItem("Beans", 400.0, "g", 480, 28, 80, 4)
    private val beef = SavedMealItem("Beef mince", 500.0, "g", 1100, 100, 0, 80)

    private fun roundTrip(state: RecipeBuilderState): RecipeBuilderState {
        val saver = RecipeBuilderState.Saver()
        val saved = with(saver) { SaverScope { true }.save(state) }!!
        return saver.restore(saved)!!
    }

    /** The header grew from three values to six; the draft and the list sit after it. */
    @Test
    fun `restores the AI field, the flags and the list behind them`() {
        val restored = roundTrip(
            RecipeBuilderState(
                name = "Chili",
                servings = 4,
                ingredients = listOf(beans, beef),
                draft = beef.copy(name = "Onion"),
                description = "Chili for 4",
                editorOpen = true,
                replaceOpen = true,
            ),
        )

        assertEquals("Chili", restored.name)
        assertEquals(4, restored.servings)
        assertEquals(listOf(beans, beef), restored.ingredients)
        assertEquals("Onion", restored.draft.name)
        assertEquals("Chili for 4", restored.description)
        assertTrue(restored.editorOpen)
        assertTrue(restored.replaceOpen)
    }

    @Test
    fun `a tapped row moves into the editor and a named draft is kept`() {
        val state = RecipeBuilderState(ingredients = listOf(beans, beef), draft = beef.copy(name = "Onion"))

        state.editIngredient(0)

        assertEquals(beans, state.draft)
        assertEquals(listOf("Beef mince", "Onion"), state.ingredients.map { it.name })
        assertTrue(state.editorOpen)
    }

    @Test
    fun `a fill keeps a name the user typed`() {
        val state = RecipeBuilderState(name = "Nana's chili")

        state.applyFill("Chili con carne", 6, listOf(beef))

        assertEquals("Nana's chili", state.name)
        assertEquals(6, state.servings)
        assertEquals(listOf(beef), state.ingredients)
    }
}
