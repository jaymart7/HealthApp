package ph.mart.healthapp.feature.food.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import ph.mart.healthapp.core.data.food.MealType
import ph.mart.healthapp.core.data.food.QUICK_ADD_NAME

class AddEntryFormTest {

    @Test
    fun `a bare calorie figure is enough`() {
        assertTrue(AddEntryForm(name = "", calories = 650).isValid())
    }

    @Test
    fun `a blank form is still rejected`() {
        assertFalse(AddEntryForm().isValid())
    }

    @Test
    fun `a named zero-calorie entry is still valid`() {
        assertTrue(AddEntryForm(name = "Black coffee", calories = 0).isValid())
    }

    @Test
    fun `a blank name logs as a quick add of one serving`() {
        val entry = AddEntryForm(name = "", calories = 650, mealType = MealType.Lunch).toFoodEntry()
        assertEquals(QUICK_ADD_NAME, entry.name)
        assertEquals(1.0, entry.portionAmount, 0.0)
        assertEquals(SERVING_UNIT, entry.portionUnit)
        assertEquals(650, entry.calories)
        assertEquals(MealType.Lunch, entry.mealType)
    }

    /** The regression that matters: the photo and barcode confirmation screens share this. */
    @Test
    fun `a named entry keeps its own name and portion`() {
        val entry = AddEntryForm(
            name = "Grilled chicken breast",
            portionAmount = 150.0,
            portionUnit = "g",
            calories = 210,
            proteinG = 32,
        ).toFoodEntry(dateEpochDay = 20000)
        assertEquals("Grilled chicken breast", entry.name)
        assertEquals(150.0, entry.portionAmount, 0.0)
        assertEquals("g", entry.portionUnit)
        assertEquals(20000L, entry.dateEpochDay)
        assertEquals(32, entry.proteinG)
    }
}
