package ph.mart.healthapp.feature.progress.ui.weight

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The records list pages by growing a counter over weigh-ins already in memory — `FoodSearchUiState`'s
 * window, on a page's own state class. The clamp is the whole of it: get it wrong and a list already
 * scrolled to its foot keeps asking for a page that never comes.
 */
class WeightRecordsWindowTest {

    @Test
    fun `the window opens at one page`() {
        assertEquals(RECORDS_PAGE_SIZE, WeightState().shownRecords)
    }

    @Test
    fun `each ask appends a page`() {
        val state = WeightState()
        state.showMoreRecords(total = 200)
        assertEquals(2 * RECORDS_PAGE_SIZE, state.shownRecords)
        state.showMoreRecords(total = 200)
        assertEquals(3 * RECORDS_PAGE_SIZE, state.shownRecords)
    }

    /** The no-loop guarantee: at the end the count stops moving, so a page whose last row is on
     * screen can keep asking for more without the window running away. */
    @Test
    fun `the window stops at the end, and asking again changes nothing`() {
        val state = WeightState()
        repeat(5) { state.showMoreRecords(total = 45) }
        assertEquals(45, state.shownRecords)
        state.showMoreRecords(total = 45)
        assertEquals(45, state.shownRecords)
    }

    /** A short range must not strand the window: three weigh-ins in a month, then the toggle goes to
     * a year — the count has to still be a page, or the year's list opens at three rows. */
    @Test
    fun `a short window never clamps below one page`() {
        val state = WeightState()
        state.showMoreRecords(total = 3)
        assertEquals(RECORDS_PAGE_SIZE, state.shownRecords)
        state.showMoreRecords(total = 300)
        assertEquals(2 * RECORDS_PAGE_SIZE, state.shownRecords)
    }
}
