package ph.mart.healthapp.core.data.food

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NutritionTrendTest {

    private fun entry(day: Long, calories: Int, proteinG: Int, carbsG: Int, fatG: Int) = FoodEntry(
        name = "Item",
        dateEpochDay = day,
        mealType = MealType.Snacks,
        portionAmount = 1.0,
        portionUnit = "g",
        calories = calories,
        proteinG = proteinG,
        carbsG = carbsG,
        fatG = fatG,
    )

    @Test
    fun `series is dense and zero-fills unlogged days`() {
        val series = listOf(entry(10, 400, 30, 40, 10)).dailySeries(fromEpochDay = 9, toEpochDay = 11)

        assertEquals(listOf(9L, 10L, 11L), series.map { it.dateEpochDay })
        assertEquals(listOf(0, 400, 0), series.map { it.calories })
        assertEquals(listOf(false, true, false), series.map { it.isLogged })
    }

    @Test
    fun `entries on the same day are summed`() {
        val series = listOf(
            entry(5, 150, 20, 8, 4),
            entry(5, 320, 10, 58, 6),
        ).dailySeries(fromEpochDay = 5, toEpochDay = 5)

        assertEquals(1, series.size)
        assertEquals(DayNutrition(5, calories = 470, proteinG = 30, carbsG = 66, fatG = 10), series.single())
    }

    @Test
    fun `averages ignore zero-filled days`() {
        val series = listOf(
            entry(1, 1000, 100, 100, 40),
            entry(3, 2000, 140, 200, 80),
        ).dailySeries(fromEpochDay = 1, toEpochDay = 3)

        // Day 2 is empty: dividing by 3 would report 1000 kcal/day, which never happened.
        assertEquals(NutritionAverages(1500, 120, 150, 60, daysLogged = 2), series.averages())
    }

    @Test
    fun `an empty range averages to zero rather than dividing by zero`() {
        val series = emptyList<FoodEntry>().dailySeries(fromEpochDay = 0, toEpochDay = 6)

        assertEquals(7, series.size)
        assertTrue(series.none { it.isLogged })
        assertEquals(NutritionAverages(0, 0, 0, 0, daysLogged = 0), series.averages())
        assertFalse(series.first().isLogged)
    }
}
