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
}
