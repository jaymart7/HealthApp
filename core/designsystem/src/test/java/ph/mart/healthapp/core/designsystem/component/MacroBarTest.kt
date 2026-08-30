package ph.mart.healthapp.core.designsystem.component

import org.junit.Assert.assertEquals
import org.junit.Test

/** The fill behind the diary's macro progress. */
class MacroBarTest {

    @Test
    fun `a part-eaten macro fills its share`() {
        assertEquals(0.5f, fillFraction(consumedG = 73, goalG = 146), 0.001f)
    }

    /** Past the goal the segment stops at full; the legend beside it reports the overage. */
    @Test
    fun `an exceeded macro clamps at full`() {
        assertEquals(1f, fillFraction(consumedG = 200, goalG = 146), 0.001f)
    }

    /** No goal is nothing to be a fraction of — an unset macro must read empty, not complete. */
    @Test
    fun `a macro with no goal reads empty`() {
        assertEquals(0f, fillFraction(consumedG = 40, goalG = 0), 0.001f)
    }

    @Test
    fun `an untouched macro reads empty`() {
        assertEquals(0f, fillFraction(consumedG = 0, goalG = 146), 0.001f)
    }
}
