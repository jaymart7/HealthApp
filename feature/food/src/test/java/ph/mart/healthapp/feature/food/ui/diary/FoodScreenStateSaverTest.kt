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
            copyPickerOpen = true,
            sheetView = AddEntryView.Form,
            browseTab = BrowseTab.Saved,
            quickAddKcal = 320,
            seededFromProduct = true,
            saveMyFood = true,
        )

        val restored = roundTrip(state)

        assertEquals(state.addForm, restored.addForm)
        assertEquals(MealType.Lunch, restored.activeMealSheet)
        assertEquals("ado", restored.searchQuery)
        assertEquals(true, restored.filterExpanded)
        assertEquals(7L, restored.editingEntryId)
        assertEquals(true, restored.shareOpen)
        assertEquals(true, restored.copyPickerOpen)
        assertEquals(AddEntryView.Form, restored.sheetView)
        assertEquals(BrowseTab.Saved, restored.browseTab)
        assertEquals(320, restored.quickAddKcal)
        assertEquals(true, restored.seededFromProduct)
        assertEquals(true, restored.saveMyFood)
    }

    /** The quick-add pill's figure is nullable for the reason every other figure on the sheet is:
     * null is "nobody has said", and it prints an em dash. The saver's list holds nulls, so this is
     * the cast that would break if the field were ever typed as a plain `Int`. */
    @Test
    fun `restores an untouched quick add as nobody having said`() {
        val restored = roundTrip(FoodScreenState())

        assertEquals(null, restored.quickAddKcal)
        assertEquals(AddEntryView.Browse, restored.sheetView)
        assertEquals(BrowseTab.Recents, restored.browseTab)
    }
}

/**
 * Back inside the add-entry sheet, level by level. The ladder is the thing a redesign of this sheet
 * is most likely to quietly break, and it is pure state: no composition needed to check it.
 */
class AddEntryBackTest {

    @Test
    fun `search steps back to browse rather than closing`() {
        val state = FoodScreenState(sheetView = AddEntryView.Search)

        assertEquals(true, state.backFromSheet())
        assertEquals(AddEntryView.Browse, state.sheetView)
    }

    @Test
    fun `the form steps back to browse`() {
        val state = FoodScreenState(sheetView = AddEntryView.Form)

        assertEquals(true, state.backFromSheet())
        assertEquals(AddEntryView.Browse, state.sheetView)
    }

    /** A correction opened straight into the form, so there is no browse state behind it to
     * return to — one back closes the sheet. */
    @Test
    fun `an edit closes from the form in one step`() {
        val state = FoodScreenState(sheetView = AddEntryView.Form, editingEntryId = 7L)

        assertEquals(false, state.backFromSheet())
    }

    @Test
    fun `browse has nowhere left to go`() {
        assertEquals(false, FoodScreenState(sheetView = AddEntryView.Browse).backFromSheet())
    }

    /** The tab chips are a filter, not a level: back does not undo one. */
    @Test
    fun `back does not step through the browse tabs`() {
        val state = FoodScreenState(sheetView = AddEntryView.Browse, browseTab = BrowseTab.Saved)

        assertEquals(false, state.backFromSheet())
        assertEquals(BrowseTab.Saved, state.browseTab)
    }
}
