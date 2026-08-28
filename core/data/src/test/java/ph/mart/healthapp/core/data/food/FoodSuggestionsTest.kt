package ph.mart.healthapp.core.data.food

import org.junit.Assert.assertEquals
import org.junit.Test

class FoodSuggestionsTest {

    private fun suggestion(name: String, isFavorite: Boolean = false) = FoodSuggestion(
        name = name,
        portionAmount = 100.0,
        portionUnit = "g",
        calories = 100,
        proteinG = 10,
        carbsG = 10,
        fatG = 10,
        isFavorite = isFavorite,
    )

    @Test
    fun `favorites lead, then recents`() {
        val merged = mergeSuggestions(
            recents = listOf(suggestion("Oats"), suggestion("Eggs")),
            favorites = listOf(suggestion("Greek yogurt", isFavorite = true)),
        )
        assertEquals(listOf("Greek yogurt", "Oats", "Eggs"), merged.map { it.name })
    }

    @Test
    fun `a recent that is already starred appears once, as the favorite`() {
        val merged = mergeSuggestions(
            recents = listOf(suggestion("greek YOGURT"), suggestion("Oats")),
            favorites = listOf(suggestion("Greek yogurt", isFavorite = true)),
        )
        assertEquals(listOf("Greek yogurt", "Oats"), merged.map { it.name })
        assertEquals(listOf(true, false), merged.map { it.isFavorite })
    }

    @Test
    fun `the merged list is capped`() {
        val merged = mergeSuggestions(
            recents = (1..30).map { suggestion("Food $it") },
            favorites = emptyList(),
        )
        assertEquals(MAX_SUGGESTIONS, merged.size)
    }

    @Test
    fun `nothing logged and nothing starred yields nothing`() {
        assertEquals(emptyList<FoodSuggestion>(), mergeSuggestions(emptyList(), emptyList()))
    }
}
