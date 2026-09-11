package ph.mart.healthapp.feature.food.ui.history

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import ph.mart.healthapp.core.data.food.FoodEntry
import ph.mart.healthapp.core.data.food.MealType

/**
 * The one piece of the history search that isn't Room or Compose. Get the fold wrong and the list
 * either re-sorts itself — putting a March row above an August one because a map decided so — or
 * opens a second heading for a day that already has one.
 */
class FoodHistoryTest {

    private fun entry(name: String, day: Long) = FoodEntry(
        name = name,
        dateEpochDay = day,
        mealType = MealType.Lunch,
        portionAmount = 1.0,
        portionUnit = "serving",
        calories = 200,
        proteinG = 10,
        carbsG = 20,
        fatG = 5,
    )

    @Test
    fun `rows on the same day share one group, in the order they arrived`() {
        val grouped = listOf(entry("a", 20_000), entry("b", 20_000)).groupedByDay()

        assertEquals(1, grouped.size)
        assertEquals(20_000L, grouped[0].first)
        assertEquals(listOf("a", "b"), grouped[0].second.map { it.name })
    }

    @Test
    fun `days keep the newest-first order Room returned them in`() {
        val grouped = listOf(
            entry("today", 20_000),
            entry("yesterday", 19_999),
            entry("also yesterday", 19_999),
            entry("last month", 19_970),
        ).groupedByDay()

        assertEquals(listOf(20_000L, 19_999L, 19_970L), grouped.map { it.first })
        assertEquals(2, grouped[1].second.size)
    }

    /** A day that comes back, comes back as its own group — the fold only ever merges *adjacent*
     * rows, so it can never reorder what the query ordered. */
    @Test
    fun `a day that reappears out of order opens a second group rather than jumping back`() {
        val grouped = listOf(entry("a", 20_000), entry("b", 19_999), entry("c", 20_000)).groupedByDay()

        assertEquals(listOf(20_000L, 19_999L, 20_000L), grouped.map { it.first })
    }

    @Test
    fun `no results is no groups`() {
        assertTrue(emptyList<FoodEntry>().groupedByDay().isEmpty())
    }
}
