package ph.mart.healthapp.core.data.water

import org.junit.Assert.assertEquals
import org.junit.Test
import ph.mart.healthapp.core.data.profile.UnitSystem

class WaterTest {

    @Test
    fun `metric stays in ml below a litre`() {
        assertEquals("0 ml", waterVolumeLabel(0, UnitSystem.Metric))
        assertEquals("750 ml", waterVolumeLabel(3, UnitSystem.Metric))
    }

    @Test
    fun `metric switches to litres at a litre`() {
        assertEquals("1.0 L", waterVolumeLabel(4, UnitSystem.Metric))
        assertEquals("2.0 L", waterVolumeLabel(8, UnitSystem.Metric))
    }

    @Test
    fun `imperial counts fluid ounces`() {
        assertEquals("0 fl oz", waterVolumeLabel(0, UnitSystem.Imperial))
        assertEquals("64 fl oz", waterVolumeLabel(8, UnitSystem.Imperial))
    }
}
