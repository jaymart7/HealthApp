package ph.mart.healthapp.feature.food.ui.photo

import org.junit.Assert.assertEquals
import org.junit.Test
import ph.mart.healthapp.feature.food.ui.photo.components.maxPan

class PhotoViewerZoomTest {

    @Test
    fun `unzoomed image cannot pan`() {
        assertEquals(0f, maxPan(scale = 1f, extent = 1080f), 0f)
    }

    @Test
    fun `pan limit is half the extent the zoom added`() {
        assertEquals(540f, maxPan(scale = 2f, extent = 1080f), 0f)
        assertEquals(1620f, maxPan(scale = 4f, extent = 1080f), 0f)
    }

    /** A pinch below 1x is clamped before it reaches here, but a negative limit would invert the
     * coerce range and throw rather than simply not pan. */
    @Test
    fun `limit never goes negative`() {
        assertEquals(0f, maxPan(scale = 0.5f, extent = 1080f), 0f)
    }

    /** The first gesture can land before onSizeChanged has reported a frame. */
    @Test
    fun `unmeasured frame cannot pan`() {
        assertEquals(0f, maxPan(scale = 4f, extent = 0f), 0f)
    }
}
