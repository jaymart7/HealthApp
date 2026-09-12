package ph.mart.healthapp.feature.food.ui.diary

import androidx.compose.runtime.saveable.SaverScope
import org.junit.Assert.assertEquals
import org.junit.Test
import ph.mart.healthapp.core.data.food.MealType
import ph.mart.healthapp.feature.food.ui.shared.AddEntryForm

class FoodScreenStateSaverTest {

    private fun roundTrip(state: FoodScreenState): FoodScreenState {
        val saver = FoodScreenState.Saver()
        val saved = with(saver) { SaverScope { true }.save(state) }!!
        return saver.restore(saved)!!
    }

    /** The four figures are nullable and null means "nobody has said" — a blank quick add or a
     * not-found barcode sits on the sheet with no numbers at all, and rotating there used to
     * crash on the restore cast. */
    @Test
    fun `restores a form with no figures entered`() {
        val restored = roundTrip(FoodScreenState(addForm = AddEntryForm(name = "Rice")))

        assertEquals(AddEntryForm(name = "Rice"), restored.addForm)
    }

    @Test
    fun `restores a filled form and the flags around it`() {
        val state = FoodScreenState(
            activeMealSheet = MealType.Lunch,
            addForm = AddEntryForm(
                mealType = MealType.Lunch,
                name = "Adobo",
                portionAmount = 150.0,
                portionUnit = "g",
                calories = 320,
                proteinG = 22,
                carbsG = 8,
                fatG = 21,
            ),
            searchQuery = "ado",
            filterExpanded = true,
            editingEntryId = 7L,
            shareOpen = true,
        )

        val restored = roundTrip(state)

        assertEquals(state.addForm, restored.addForm)
        assertEquals(MealType.Lunch, restored.activeMealSheet)
        assertEquals("ado", restored.searchQuery)
        assertEquals(true, restored.filterExpanded)
        assertEquals(7L, restored.editingEntryId)
        assertEquals(true, restored.shareOpen)
    }
}
