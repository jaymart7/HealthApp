package ph.mart.healthapp.feature.progress.ui.photo

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The selection rule the Photos page's whole gesture rests on: the second pick is what pushes a
 * comparison, and the order the two land in is the order the tile badges number them.
 */
class PhotosStateTest {

    @Test
    fun `picks accumulate in tap order`() {
        val state = PhotosState()
        state.toggle(7)
        state.toggle(3)
        assertEquals(listOf(7L, 3L), state.selectedIds)
    }

    @Test
    fun `tapping a picked tile drops it`() {
        val state = PhotosState()
        state.toggle(7)
        state.toggle(3)
        state.toggle(7)
        assertEquals(listOf(3L), state.selectedIds)
    }

    /** A third tap swaps one end of the pair rather than doing nothing — the oldest pick leaves. */
    @Test
    fun `at the ceiling the oldest pick leaves`() {
        val state = PhotosState()
        state.toggle(7)
        state.toggle(3)
        state.toggle(9)
        assertEquals(listOf(3L, 9L), state.selectedIds)
    }

    @Test
    fun `clear empties the selection`() {
        val state = PhotosState()
        state.toggle(7)
        state.toggle(3)
        state.clear()
        assertEquals(emptyList<Long>(), state.selectedIds)
    }
}
