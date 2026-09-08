package ph.mart.healthapp.core.data.food

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
        assertEquals(470, totals.calories)
        assertEquals(30, totals.proteinG)
        assertEquals(66, totals.carbsG)
        assertEquals(10, totals.fatG)
    }

    @Test
    fun `every nutrient folds with the macros`() {
        val entries = listOf(
            entry(calories = 150, proteinG = 20, carbsG = 8, fatG = 4).copy(
                nutrients = Nutrients(fiberG = 3, sugarG = 2, sodiumMg = 410, calciumMg = 120, ironUg = 900),
            ),
            entry(calories = 320, proteinG = 10, carbsG = 58, fatG = 6).copy(
                nutrients = Nutrients(fiberG = 5, sugarG = 19, sodiumMg = 830, calciumMg = 60, ironUg = 1500),
            ),
        )

        val totals = entries.dailyTotals().nutrients

        assertEquals(8, totals.fiberG)
        assertEquals(21, totals.sugarG)
        assertEquals(1240, totals.sodiumMg)
        assertEquals(180, totals.calciumMg)
        assertEquals(2400, totals.ironUg)
    }

    @Test
    fun `an entry that carries nothing does not disturb the ones that do`() {
        val entries = listOf(
            entry(calories = 150, proteinG = 20, carbsG = 8, fatG = 4)
                .copy(nutrients = Nutrients(sodiumMg = 410)),
            entry(calories = 90, proteinG = 0, carbsG = 22, fatG = 0),
        )

        assertEquals(410, entries.dailyTotals().nutrients.sodiumMg)
    }

    /** The count the graded panel stands on. Fiber, sugar and sodium deliberately do not make a
     * food "covered": the built-in list has filled those for years, so counting them would report
     * full coverage for a day with no vitamin figure in it. */
    @Test
    fun `coverage counts only the foods carrying a panel nutrient`() {
        val entries = listOf(
            entry(calories = 150, proteinG = 20, carbsG = 8, fatG = 4)
                .copy(nutrients = Nutrients(calciumMg = 120)),
            // Sodium only — this is the case that would read as covered if the predicate widened.
            entry(calories = 90, proteinG = 0, carbsG = 22, fatG = 0)
                .copy(nutrients = Nutrients(sodiumMg = 410)),
            entry(calories = 200, proteinG = 5, carbsG = 30, fatG = 5),
        )

        val totals = entries.dailyTotals()

        assertEquals(3, totals.foodCount)
        assertEquals(1, totals.foodsWithMicronutrients)
    }

    /** A nutrient with no figure is absent, not a shortfall — the whole reason `0` cannot be
     * graded. */
    @Test
    fun `readings drop nutrients with no value and keep the ones with data`() {
        val readings = Nutrients(fiberG = 12, sodiumMg = 1200)
            .readings(Nutrients(fiberG = 27, sodiumMg = 2300, calciumMg = 1000))

        assertEquals(listOf(Nutrient.Fiber, Nutrient.Sodium), readings.map { it.nutrient })
        assertEquals(27, readings.first().target)
    }

    @Test
    fun `a reading with no targets at all is reported ungraded`() {
        val readings = Nutrients(fiberG = 12).readings(targets = null)

        assertEquals(1, readings.size)
        assertNull(readings.single().target)
        assertEquals(0f, readings.single().fraction, 0f)
    }

    /** Passing a calcium goal is good news; only a limit can be exceeded. */
    @Test
    fun `only a stay-under nutrient reads as over its target`() {
        val readings = Nutrients(sodiumMg = 3000, calciumMg = 1400)
            .readings(Nutrients(sodiumMg = 2300, calciumMg = 1000))

        assertEquals(true, readings.single { it.nutrient == Nutrient.Sodium }.overLimit)
        assertEquals(false, readings.single { it.nutrient == Nutrient.Calcium }.overLimit)
        // Clamped either way: the bar stops at full rather than overflowing its track.
        assertEquals(1f, readings.single { it.nutrient == Nutrient.Calcium }.fraction, 0f)
    }
}
