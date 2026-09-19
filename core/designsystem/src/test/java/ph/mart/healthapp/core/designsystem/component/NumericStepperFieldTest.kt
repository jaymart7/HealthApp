package ph.mart.healthapp.core.designsystem.component

import java.util.Locale
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

    /**
     * A `KeyboardType.Decimal` keyboard in a comma-locale emits `','` for the decimal key. It used
     * to be filtered away, so the key did nothing and no stepper in the app could take a decimal.
     */
    @Test
    fun `a typed comma is the decimal point`() {
        assertEquals("75.5", "75,5".keepDigits(decimal = true))
        assertEquals("1.25", "1,2567".keepDigits(decimal = true))
        // Still a separator to drop when the field takes whole numbers.
        assertEquals("755", "75,5".keepDigits(decimal = false))
    }

    /** `Char.isDigit()` is true for these; `toDoubleOrNull()` is not, so they used to read zero. */
    @Test
    fun `non-ASCII digits are not digits here`() {
        assertEquals("", "\u0665\u0667".keepDigits(decimal = false))
        assertEquals("12", "1\u09662".keepDigits(decimal = false))
    }

    /**
     * The whole point of pinning the formatter to [Locale.US]: whatever the device's locale, what
     * a stepper *shows* is what `toDoubleOrNull()` — and therefore the field's own model — reads
     * back. Before this, a German device seeded "75,5", parsed it as zero, stopped refreshing on
     * +/-, and turned the next keystroke into 7552.
     */
    @Test
    fun `what a field shows is what it parses back`() {
        val original = Locale.getDefault()
        try {
            Locale.setDefault(Locale.GERMANY)
            assertEquals("75.5", formatOneDecimal(75.5))
            assertEquals(75.5, formatOneDecimal(75.5).toDoubleOrNull())
            assertEquals("75.5", formatOneDecimal(75.5).keepDigits(decimal = true))
        } finally {
            Locale.setDefault(original)
        }
    }
}
