package ph.mart.healthapp.feature.food.ui.shared

import org.junit.Assert.assertEquals
import org.junit.Test
import ph.mart.healthapp.core.data.food.MealType
import ph.mart.healthapp.feature.food.ui.shared.components.MealTotal

/**
 * The one derivation behind both the review screen's headline card and its Log button. If these
 * two ever read different figures the total is worse than none, so the sum lives here.
 */
class MealTotalTest {

    private fun form(calories: Int?, protein: Int?, carbs: Int?, fat: Int?) = AddEntryForm(
        mealType = MealType.Breakfast,
        name = "x",
        portionAmount = 1.0,
        portionUnit = "serving",
        calories = calories,
        proteinG = protein,
        carbsG = carbs,
        fatG = fat,
    )

    @Test
    fun `the rows add up`() {
        val total = MealTotal.of(
            listOf(form(182, 13, 2, 14), form(90, 3, 17, 1), form(2, 0, 0, 0)),
        )
        assertEquals(274, total.calories)
        assertEquals(16, total.proteinG)
        assertEquals(19, total.carbsG)
        assertEquals(15, total.fatG)
    }

    /** A field the user cleared is zero toward the total, not a row to skip — the row is still
     * being logged, and dropping it would make the card disagree with the list under it. */
    @Test
    fun `a cleared field counts as zero rather than removing the row`() {
        val total = MealTotal.of(listOf(form(120, null, 4, null), form(null, 8, null, 3)))
        assertEquals(120, total.calories)
        assertEquals(8, total.proteinG)
        assertEquals(4, total.carbsG)
        assertEquals(3, total.fatG)
    }

    /** Every row removed. The screen still draws the card, and a bar of nothing is what
     * `MacroBar`'s own floor is for. */
    @Test
    fun `no rows is four zeroes, not a crash`() {
        assertEquals(MealTotal(), MealTotal.of(emptyList()))
    }
}
