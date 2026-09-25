package ph.mart.healthapp.feature.food.ui.library

import androidx.compose.runtime.saveable.SaverScope
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import ph.mart.healthapp.core.data.food.Nutrients
import ph.mart.healthapp.core.data.food.RecipeParseResult
import ph.mart.healthapp.core.data.food.SavedMealItem
import ph.mart.healthapp.feature.food.ui.shared.AddEntryForm

class LibraryItemStateTest {

    private val beans = SavedMealItem("Beans", 400.0, "g", 480, 28, 80, 4, Nutrients(fiberG = 24, ironUg = 9))
    private val beef = SavedMealItem("Beef mince", 500.0, "g", 1100, 100, 0, 80)
    private val chili = LibraryItemForm(kind = LibraryKind.Recipe, name = "Chili", servings = 4, ingredients = listOf(beans, beef))

    private fun roundTrip(state: LibraryItemState): LibraryItemState {
        val saver = LibraryItemState.Saver()
        val saved = with(saver) { SaverScope { true }.save(state) }!!
        return saver.restore(saved)!!
    }

    @Test
    fun `restores the step, the form, the open sheet and every nutrient`() {
        val restored = roundTrip(
            LibraryItemState(
                step = LibraryStep.Review,
                form = chili,
                description = "Chili for 4",
                fromAi = true,
                loaded = true,
                draft = beef.copy(name = "Onion"),
                editingIndex = 1,
                dialog = LibraryDialog.Replace,
            ),
        )

        assertEquals(LibraryStep.Review, restored.step)
        assertEquals(chili, restored.form)
        assertEquals("Chili for 4", restored.description)
        assertTrue(restored.fromAi)
        assertTrue(restored.loaded)
        assertEquals("Onion", restored.draft?.name)
        assertEquals(1, restored.editingIndex)
        assertEquals(LibraryDialog.Replace, restored.dialog)
    }

    @Test
    fun `a food survives a rotation equal to the one that was opened`() {
        val food = LibraryItemForm(
            kind = LibraryKind.Food,
            food = AddEntryForm(name = "Whey", portionAmount = 30.0, calories = 120, proteinG = 24, servingSize = "1 scoop"),
        )
        val restored = roundTrip(LibraryItemState(step = LibraryStep.Review, form = food))

        assertEquals(food, restored.form)
        assertNull(restored.draft)
        assertNull(restored.dialog)
    }

    @Test
    fun `a tapped row is edited in place and dismissing it loses nothing`() {
        val state = LibraryItemState(form = chili)

        state.openIngredient(0)
        state.draft = state.draft!!.copy(name = "Black beans")
        state.draft = null
        assertEquals(listOf(beans, beef), state.form.ingredients)

        state.openIngredient(0)
        state.draft = state.draft!!.copy(name = "Black beans")
        state.commitDraft()
        assertEquals(listOf("Black beans", "Beef mince"), state.form.ingredients.map { it.name })
        assertNull(state.draft)
    }

    @Test
    fun `a new ingredient is appended, and a nameless one is not`() {
        val state = LibraryItemState(form = chili)

        state.addIngredient()
        state.commitDraft()
        assertEquals(2, state.form.ingredients.size)

        state.addIngredient()
        state.draft = state.draft!!.copy(name = "Onion")
        state.commitDraft()
        assertEquals(listOf("Beans", "Beef mince", "Onion"), state.form.ingredients.map { it.name })
    }

    @Test
    fun `a fill replaces the form and moves on to the review`() {
        val state = LibraryItemState(form = chili)

        state.applyFill(LibraryItemForm(kind = LibraryKind.Recipe, name = "Adobo", ingredients = listOf(beef)))

        assertEquals("Adobo", state.form.name)
        assertEquals(listOf(beef), state.form.ingredients)
        assertTrue(state.fromAi)
        assertEquals(LibraryStep.Review, state.step)
    }

    @Test
    fun `an item only loads once, so a restored edit is kept`() {
        val state = LibraryItemState(step = LibraryStep.Review)

        state.applyLoaded(chili)
        state.form = chili.copy(name = "Nana's chili")
        state.applyLoaded(chili)

        assertEquals("Nana's chili", state.form.name)
    }

    @Test
    fun `save needs a name and something in it, or a food with calories`() {
        assertTrue(LibraryItemState(form = chili).canSave)
        assertFalse(LibraryItemState(form = chili.copy(name = " ")).canSave)
        assertFalse(LibraryItemState(form = chili.copy(ingredients = emptyList())).canSave)
        assertFalse(LibraryItemState(form = LibraryItemForm(kind = LibraryKind.Food, food = AddEntryForm(name = "Whey"))).canSave)
        assertTrue(
            LibraryItemState(form = LibraryItemForm(kind = LibraryKind.Food, food = AddEntryForm(name = "Whey", calories = 120))).canSave,
        )
    }

    @Test
    fun `the model's food becomes a food form named as the user said it`() {
        val form = RecipeParseResult.Parsed("Mum's adobo", 1, listOf(beef.copy(name = "Pork adobo")), isFood = true).toForm()

        assertEquals(LibraryKind.Food, form.kind)
        assertEquals("Mum's adobo", form.food.name)
        assertEquals(1100, form.food.calories)
        assertEquals(500.0, form.food.portionAmount, 0.0)
    }

    @Test
    fun `the model's recipe keeps its yield and every ingredient`() {
        val form = RecipeParseResult.Parsed("Chili", 4, listOf(beans, beef)).toForm()

        assertEquals(chili, form)
    }
}
