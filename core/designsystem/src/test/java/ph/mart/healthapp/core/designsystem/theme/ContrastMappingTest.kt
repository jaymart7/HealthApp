package ph.mart.healthapp.core.designsystem.theme

import org.junit.Assert.assertEquals
import org.junit.Test

class ContrastMappingTest {

    @Test
    fun `system contrast values map to their schemes`() {
        assertEquals(AppContrast.Standard, contrastFor(0f))
        assertEquals(AppContrast.Medium, contrastFor(0.5f))
        assertEquals(AppContrast.High, contrastFor(1f))
    }

    @Test
    fun `bucket boundaries and out-of-range values are clamped by the when order`() {
        assertEquals(AppContrast.Standard, contrastFor(0.32f))
        assertEquals(AppContrast.Medium, contrastFor(0.33f))
        assertEquals(AppContrast.Medium, contrastFor(0.65f))
        assertEquals(AppContrast.High, contrastFor(0.66f))
        assertEquals(AppContrast.Standard, contrastFor(-1f))
        assertEquals(AppContrast.High, contrastFor(2f))
    }
}
