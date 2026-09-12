package ph.mart.healthapp.core.designsystem.component

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** The value maths behind the ruler's drag. Everything the gesture does to a number happens here,
 * which is what lets the drag itself stay a translation. */
class RulerPickerFieldTest {

    private val age = 13.0..100.0
    private val weightKg = 30.0..250.0

    @Test
    fun `a value between ticks lands on the nearer one`() {
        assertEquals(25.0, snapToStep(25.4, age, step = 1.0), 0.001)
        assertEquals(26.0, snapToStep(25.6, age, step = 1.0), 0.001)
    }

    /** A half-kilo scale has to land on 65.0 and 65.5 — which it only does if the steps are
     * measured from the range's start rather than from zero. */
    @Test
    fun `a half-step scale lands on halves`() {
        assertEquals(65.0, snapToStep(65.1, weightKg, step = 0.5), 0.001)
        assertEquals(65.5, snapToStep(65.4, weightKg, step = 0.5), 0.001)
    }

    @Test
    fun `a value past either end clamps to the range`() {
        assertEquals(13.0, snapToStep(4.0, age, step = 1.0), 0.001)
        assertEquals(100.0, snapToStep(140.0, age, step = 1.0), 0.001)
    }

    /** What an untouched field takes on first contact. It has to be a value the scale can return
     * to, so it is snapped like any other. */
    @Test
    fun `the unset default is the snapped midpoint`() {
        assertEquals(57.0, midpoint(age, step = 1.0), 0.001)
        assertEquals(140.0, midpoint(weightKg, step = 0.5), 0.001)
    }

    @Test
    fun `a drag inside the range is not resisted`() {
        assertEquals(300f, resist(offsetPx = 300f, maxPx = 900f, limit = 36f), 0.001f)
    }

    /** Past the end the band still moves — a wall would read as a broken drag — but it can never
     * exceed its own width however hard the drag pushes. */
    @Test
    fun `a drag past the end is resisted within the band`() {
        val overshoot = resist(offsetPx = 1000f, maxPx = 900f, limit = 36f)
        assertTrue("$overshoot", overshoot > 900f && overshoot < 936f)
        assertTrue(resist(offsetPx = 90000f, maxPx = 900f, limit = 36f) < 936f)
    }

    @Test
    fun `a drag before the start is resisted the same way`() {
        val under = resist(offsetPx = -100f, maxPx = 900f, limit = 36f)
        assertTrue("$under", under < 0f && under > -36f)
    }

    /** Weight carries a decimal and age does not, and neither may render the other's shape. */
    @Test
    fun `values render to their own precision`() {
        assertEquals("65.5", format(65.5, decimals = 1))
        assertEquals("25", format(25.0, decimals = 0))
        assertEquals("26", format(25.6, decimals = 0))
    }
}
