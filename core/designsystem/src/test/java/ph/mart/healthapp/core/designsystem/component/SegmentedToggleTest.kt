package ph.mart.healthapp.core.designsystem.component

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The scroll the pill track allows itself. Figures are Progress's ten tabs on a 360dp screen:
 * 64dp pills inside 4dp of track padding, so five and a half pills are in view at a time.
 */
class SegmentedToggleTest {

    private fun target(index: Int, currentPx: Int) =
        scrollTargetFor(index = index, pillPx = 64f, padPx = 4f, viewportPx = 360f, currentPx = currentPx)

    /** The regression: tapping a pill that was already on screen must not slide the track. */
    @Test
    fun `a visible pill does not move the track`() {
        assertNull(target(index = 4, currentPx = 0))
    }

    @Test
    fun `the first pill at rest does not move the track`() {
        assertNull(target(index = 0, currentPx = 0))
    }

    /** Just the overhang — left-aligning it would throw four visible pills off the screen. */
    @Test
    fun `a pill past the right edge scrolls by the overhang`() {
        assertEquals(32, target(index = 5, currentPx = 0))
    }

    /** The last of ten: 8dp of padding plus ten pills, less the viewport. */
    @Test
    fun `the last pill scrolls flush to the end`() {
        assertEquals(288, target(index = 9, currentPx = 0))
    }

    @Test
    fun `a pill left of the current offset scrolls back to its leading edge`() {
        assertEquals(128, target(index = 2, currentPx = 288))
    }
}
