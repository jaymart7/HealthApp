package ph.mart.healthapp.core.data.food

import org.junit.Assert.assertEquals
import org.junit.Test

class FoodTotalsTest {

    private fun entry(calories: Int, proteinG: Int, carbsG: Int, fatG: Int) = FoodEntry(
        name = "Item",
        mealType = MealType.Snacks,
        portionAmount = 1.0,
        portionUnit = "g",
        calories = calories,
        proteinG = proteinG,
        carbsG = carbsG,
        fatG = fatG,
    )

    @Test
    fun `empty list totals to zero`() {
        val totals = emptyList<FoodEntry>().dailyTotals()
        assertEquals(DiaryTotals(0, 0, 0, 0), totals)
    }

    @Test
    fun `totals sum across multiple entries`() {
        val entries = listOf(
            entry(calories = 150, proteinG = 20, carbsG = 8, fatG = 4),
            entry(calories = 320, proteinG = 10, carbsG = 58, fatG = 6),
        )
        val totals = entries.dailyTotals()
        assertEquals(DiaryTotals(calories = 470, proteinG = 30, carbsG = 66, fatG = 10), totals)
    }

    @Test
    fun `fiber, sugar and sodium fold with the macros`() {
        val entries = listOf(
            entry(calories = 150, proteinG = 20, carbsG = 8, fatG = 4).copy(
                fiberG = 3,
                sugarG = 2,
                sodiumMg = 410,
            ),
            entry(calories = 320, proteinG = 10, carbsG = 58, fatG = 6).copy(
                fiberG = 5,
                sugarG = 19,
                sodiumMg = 830,
            ),
        )

        val totals = entries.dailyTotals()

        assertEquals(8, totals.fiberG)
        assertEquals(21, totals.sugarG)
        assertEquals(1240, totals.sodiumMg)
    }

    @Test
    fun `an entry that carries none of the three does not disturb the ones that do`() {
        val entries = listOf(
            entry(calories = 150, proteinG = 20, carbsG = 8, fatG = 4).copy(sodiumMg = 410),
            entry(calories = 90, proteinG = 0, carbsG = 22, fatG = 0),
        )

        assertEquals(410, entries.dailyTotals().sodiumMg)
    }
}
