package ph.mart.healthapp.core.designsystem.component

import org.junit.Assert.assertEquals
import org.junit.Test

class WaterGlassRowTest {

    @Test
    fun `tapping an empty glass fills up to it`() {
        assertEquals(1, glassesAfterTap(current = 0, tappedIndex = 0))
        assertEquals(6, glassesAfterTap(current = 2, tappedIndex = 5))
    }

    @Test
    fun `tapping the last filled glass clears it`() {
        assertEquals(0, glassesAfterTap(current = 1, tappedIndex = 0))
        assertEquals(4, glassesAfterTap(current = 5, tappedIndex = 4))
    }

    @Test
    fun `tapping below the count sets that count`() {
        assertEquals(2, glassesAfterTap(current = 6, tappedIndex = 1))
    }

    @Test
    fun `never goes negative`() {
        assertEquals(0, glassesAfterTap(current = 0, tappedIndex = -1))
    }
}
