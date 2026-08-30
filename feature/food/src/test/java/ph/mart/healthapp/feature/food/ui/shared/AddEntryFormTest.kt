package ph.mart.healthapp.feature.food.ui.shared

import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import ph.mart.healthapp.core.data.food.FoodEntry
import ph.mart.healthapp.core.data.food.MealType
import ph.mart.healthapp.core.data.food.QUICK_ADD_NAME
import ph.mart.healthapp.core.data.food.SavedMealItem

class AddEntryFormTest {

    @Test
    fun `changing the portion reprices fiber, sugar and sodium with everything else`() {
        val form = AddEntryForm(
            name = "Tortilla chips",
            portionAmount = 100.0,
            calories = 536,
            proteinG = 7,
            carbsG = 64,
            fatG = 25,
            fiberG = 5,
            sugarG = 4,
            sodiumMg = 1071,
        )

        val halved = form.withPortionAmount(50.0)

        assertEquals(268, halved.calories)
        assertEquals(3, halved.fiberG)
        assertEquals(2, halved.sugarG)
        assertEquals(536, halved.sodiumMg)
    }

    @Test
    fun `a recipe ingredient reprices the three the same way`() {
        val item = SavedMealItem(
            name = "Beans",
            portionAmount = 200.0,
            portionUnit = "g",
            calories = 300,
            proteinG = 20,
            carbsG = 50,
            fatG = 2,
            fiberG = 16,
            sugarG = 4,
            sodiumMg = 800,
        )

        val doubled = item.withPortionAmount(400.0)

        assertEquals(32, doubled.fiberG)
        assertEquals(8, doubled.sugarG)
        assertEquals(1600, doubled.sodiumMg)
    }

    @Test
    fun `reopening a logged row round-trips the three`() {
        val entry = FoodEntry(
            id = 7,
            name = "Tortilla chips",
            dateEpochDay = 20_690,
            mealType = MealType.Snacks,
            portionAmount = 30.0,
            portionUnit = "g",
            calories = 161,
            proteinG = 2,
            carbsG = 19,
            fatG = 8,
            fiberG = 2,
            sugarG = 1,
            sodiumMg = 321,
        )

        val reopened = entry.toAddEntryForm().toFoodEntry(dateEpochDay = entry.dateEpochDay)

        assertEquals(entry.copy(id = 0), reopened)
    }

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

    /** Tapping a logged row and saving it straight back must be a no-op on every figure. */
    @Test
    fun `a logged entry round-trips through the edit form unchanged`() {
        val logged = FoodEntry(
            id = 7,
            name = "Grilled chicken breast",
            dateEpochDay = 20000,
            mealType = MealType.Dinner,
            portionAmount = 150.0,
            portionUnit = "g",
            calories = 210,
            proteinG = 32,
            carbsG = 2,
            fatG = 8,
        )
        val saved = logged.toAddEntryForm().toFoodEntry(dateEpochDay = logged.dateEpochDay)
        assertEquals(logged.copy(id = 0), saved)
    }

    /** The trap: a quick add is stored under [QUICK_ADD_NAME], and reopening it must not turn
     * that placeholder into a food the user claims to have named. */
    @Test
    fun `a quick add reopens as a quick add`() {
        val logged = FoodEntry(
            name = QUICK_ADD_NAME,
            mealType = MealType.Snacks,
            portionAmount = 1.0,
            portionUnit = SERVING_UNIT,
            calories = 320,
            proteinG = 0,
            carbsG = 0,
            fatG = 0,
        )
        val form = logged.toAddEntryForm()
        assertEquals("", form.name)
        assertEquals(QUICK_ADD_NAME, form.toFoodEntry().name)
        assertEquals(320, form.toFoodEntry().calories)
    }

    /**
     * The defect this guards: the barcode screen tells the user to "adjust the portion to match
     * what you ate", and before this the numbers stayed where the lookup left them — 30 g of
     * Nutella logged at the 100 g price.
     */
    @Test
    fun `changing the portion reprices the entry`() {
        val scanned = AddEntryForm(
            name = "Nutella",
            portionAmount = 100.0,
            portionUnit = "g",
            calories = 539,
            proteinG = 6,
            carbsG = 58,
            fatG = 31,
        )
        val eaten = scanned.withPortionAmount(30.0)
        assertEquals(30.0, eaten.portionAmount, 0.0)
        assertEquals(162, eaten.calories)
        assertEquals(2, eaten.proteinG)
        assertEquals(17, eaten.carbsG)
        assertEquals(9, eaten.fatG)
    }

    @Test
    fun `a portion with nothing to scale from only moves the amount`() {
        val blank = AddEntryForm(portionAmount = 0.0, calories = 0)
        val scaled = blank.withPortionAmount(50.0)
        assertEquals(50.0, scaled.portionAmount, 0.0)
        assertEquals(0, scaled.calories)
    }

    /**
     * Scaling from the current pair rather than a remembered original means each step rounds, so
     * seven taps of the stepper can land a unit away from the one-shot answer. That bound is the
     * thing worth holding: a kilocalorie of drift is invisible, a compounding one would not be.
     */
    @Test
    fun `stepping down repeatedly stays within a unit of scaling in one go`() {
        val start = AddEntryForm(name = "Oats", portionAmount = 100.0, calories = 389, carbsG = 66)
        var stepped = start
        repeat(7) { stepped = stepped.withPortionAmount(stepped.portionAmount - 10.0) }
        val direct = start.withPortionAmount(30.0)
        assertTrue(abs(direct.calories - stepped.calories) <= 1)
        assertTrue(abs(direct.carbsG - stepped.carbsG) <= 1)
    }

    @Test
    fun `a recipe ingredient reprices the same way`() {
        val ingredient = SavedMealItem("Beef mince", 500.0, "g", 1100, 100, 0, 80)
        val half = ingredient.withPortionAmount(250.0)
        assertEquals(250.0, half.portionAmount, 0.0)
        assertEquals(550, half.calories)
        assertEquals(50, half.proteinG)
        assertEquals(40, half.fatG)
    }
}
