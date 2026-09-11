package ph.mart.healthapp.feature.food.ui.search

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import ph.mart.healthapp.core.data.food.ScannedProduct

class FoodSearchWindowTest {

    private fun results(n: Int) = List(n) { ScannedProduct("food $it", 100.0, "g", 100, 1, 1, 1) }

    @Test
    fun `the window opens at one page`() {
        val state = FoodSearchUiState(results = results(30))
        assertEquals(FOOD_PAGE_SIZE, state.visibleItems.size)
        assertEquals("food 0", state.visibleItems.first().name)
        assertTrue(state.hasMore)
    }

    @Test
    fun `each ask appends a page`() {
        val state = FoodSearchUiState(results = results(30)).withMore()
        assertEquals(2 * FOOD_PAGE_SIZE, state.visibleItems.size)
        assertEquals("food 15", state.visibleItems.last().name)
    }

    @Test
    fun `the window stops at the end, and asking again changes nothing`() {
        var state = FoodSearchUiState(results = results(20))
        repeat(5) { state = state.withMore() }
        assertEquals(20, state.visibleItems.size)
        assertFalse(state.hasMore)
        // The no-loop guarantee: an equal state is dropped by the flow, so a box already scrolled
        // to its bottom can keep asking.
        assertEquals(state, state.withMore())
    }

    @Test
    fun `a short answer never shrinks the window below a page`() {
        // Three local hits, then twenty online ones land behind them: the box must still be able to
        // grow, which it cannot if the ask clamped the window down to three.
        val short = FoodSearchUiState(results = results(3)).withMore()
        assertEquals(FOOD_PAGE_SIZE, short.shown)
        assertEquals(3, short.visibleItems.size)
        assertFalse(short.hasMore)
        assertTrue(short.copy(results = results(23)).hasMore)
    }
}
