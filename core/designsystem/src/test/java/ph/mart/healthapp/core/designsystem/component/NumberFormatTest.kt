package ph.mart.healthapp.core.designsystem.component

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The app's one decimal formatter. Nine copies of it used to sit across five modules, each one
 * formatting in the default locale while the fields that showed the result parsed ASCII.
 */
class NumberFormatTest {

    @Test
    fun `a whole number is left whole`() {
        assertEquals("75", formatOneDecimal(75.0))
        assertEquals("0", formatOneDecimal(0.0))
        assertEquals("-3", formatOneDecimal(-3.0))
    }

    @Test
    fun `a fraction keeps one place`() {
        assertEquals("75.5", formatOneDecimal(75.5))
        assertEquals("75.5", formatOneDecimal(75.49))
        assertEquals("0.1", formatOneDecimal(0.1))
    }

    @Test
    fun `zero decimals rounds rather than truncating`() {
        assertEquals("26", formatDecimals(25.6, decimals = 0))
        assertEquals("25", formatDecimals(25.4, decimals = 0))
        assertEquals("0.85", formatDecimals(0.847, decimals = 2))
    }

    /**
     * The rule the whole file exists for. A device in de-DE, fr-FR, es-ES, pt-BR, id-ID or ru-RU
     * writes `75,5` from `"%.1f".format(v)` — and `toDoubleOrNull`, which is what reads these
     * figures back out of every stepper and ruler field, answers null to that.
     */
    @Test
    fun `the separator is ASCII whatever the device speaks`() {
        val original = Locale.getDefault()
        try {
            for (locale in listOf(Locale.GERMANY, Locale.FRANCE, Locale.forLanguageTag("ru-RU"))) {
                Locale.setDefault(locale)
                assertEquals("$locale", "75.5", formatOneDecimal(75.5))
                assertEquals("$locale", "0.85", formatDecimals(0.847, decimals = 2))
            }
        } finally {
            Locale.setDefault(original)
        }
    }
}
