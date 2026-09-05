package ph.mart.healthapp.core.data.exercise

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The names come from [java.text.DateFormatSymbols] rather than a resource array, so there is
 * nothing to translate — but the app counts from Monday and the symbols table counts from Sunday,
 * which is the one thing that can silently rotate every routine's plan by a day.
 */
class WeekdayNamesTest {

    @Test
    fun `the week starts on Monday and ends on Sunday`() {
        Locale.setDefault(Locale.US)
        assertEquals(7, weekdayNames().size)
        assertEquals("Monday", weekdayNames().first())
        assertEquals("Sunday", weekdayNames().last())
        assertEquals("Mon", weekdayShort().first())
        assertEquals(listOf("M", "T", "W", "T", "F", "S", "S"), weekdayInitials())
    }
}
