package ph.mart.healthapp.core.designsystem.component

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * One tap of a portion stepper, per unit. The rule is shared by the two steppers in the app and
 * used to be duplicated — the copy in `PortionControl` stepped ounces and servings by ten, which is
 * what this test exists to stop coming back.
 */
class PortionStepTest {

    @Test
    fun `grams step by ten`() {
        assertEquals(10.0, portionStep("g"), 0.0)
    }

    @Test
    fun `a cup steps by a quarter`() {
        assertEquals(0.25, portionStep("cup"), 0.0)
    }

    @Test
    fun `ounces and servings step by a half`() {
        assertEquals(0.5, portionStep("oz"), 0.0)
        assertEquals(0.5, portionStep("serving"), 0.0)
    }

    @Test
    fun `a unit the app never offers still steps sensibly`() {
        // A food logged as "scoop" by an older build, or by the recipe editor's free-text unit.
        assertEquals(0.5, portionStep("scoop"), 0.0)
    }
}
