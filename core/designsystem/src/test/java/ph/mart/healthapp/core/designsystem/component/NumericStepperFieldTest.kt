package ph.mart.healthapp.core.designsystem.component

import org.junit.Assert.assertEquals
import org.junit.Test

/** The sanitiser behind every typed calorie, macro and portion figure in the app. */
class NumericStepperFieldTest {

    @Test
    fun `typing over the placeholder zero does not leave it behind`() {
        assertEquals("320", "0320".keepDigits(decimal = false))
    }

    @Test
    fun `a cleared field stays cleared`() {
        assertEquals("", "".keepDigits(decimal = false))
    }

    @Test
    fun `zero survives on its own`() {
        assertEquals("0", "0".keepDigits(decimal = false))
        assertEquals("0", "000".keepDigits(decimal = false))
    }

    @Test
    fun `letters and separators are dropped`() {
        assertEquals("150", "1a5 0".keepDigits(decimal = false))
        assertEquals("150", "1.5 0".keepDigits(decimal = false))
    }

    @Test
    fun `a decimal portion keeps one point and two places`() {
        assertEquals("0.5", "0.5".keepDigits(decimal = true))
        assertEquals("1.25", "1.2567".keepDigits(decimal = true))
        // A second point is ignored rather than truncating what follows it.
        assertEquals("1.57", "1.5.7".keepDigits(decimal = true))
    }

    /** Six digits keeps the parse inside Int no matter what is pasted in. */
    @Test
    fun `the digit cap holds`() {
        assertEquals("999999", "9999999999".keepDigits(decimal = false))
    }
}
