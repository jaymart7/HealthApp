package ph.mart.healthapp.core.designsystem.component

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * An epoch day is local midnight divided by a day in millis — the same definition
 * `ph.mart.healthapp.core.data.epochDayOf` uses to key every dated table. `:core:designsystem`
 * cannot depend on `:core:data`, so that contract is restated here as [epochDayOfContract] and
 * these tests are what stop the two drifting apart again.
 *
 * Run under a forced default zone: at UTC every wrong convention still looks right, which is
 * exactly how the original mismatch survived.
 */
class DateFormatTest {

    private val original: TimeZone = TimeZone.getDefault()
    private val originalLocale: Locale = Locale.getDefault()

    @After
    fun restoreZone() {
        TimeZone.setDefault(original)
        Locale.setDefault(originalLocale)
    }

    private fun epochDayOfContract(millis: Long): Long = Calendar.getInstance().apply {
        timeInMillis = millis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis / 86_400_000L

    private fun localDate(millis: Long): String =
        SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(java.util.Date(millis))

    private val zones = listOf("UTC", "Asia/Manila", "America/New_York", "Asia/Kathmandu")

    /**
     * Deliberately not `assertEquals(epochDayOfContract(now), todayEpochDay())`: at UTC+8 the old
     * implementation only disagreed between local 00:00 and 08:00, so that assertion passed or
     * failed depending on the hour the suite ran. This one has to hold at every hour.
     */
    @Test
    fun `today formats as today, at whatever hour the suite runs`() {
        zones.forEach { id ->
            TimeZone.setDefault(TimeZone.getTimeZone(id))
            val now = System.currentTimeMillis()
            assertEquals(id, localDate(now), formatEpochDay(epochDayOfContract(now)))
            assertEquals(id, localDate(now), formatEpochDay(todayEpochDay()))
        }
    }

    @Test
    fun `formatEpochDay renders the local date that epoch day stands for`() {
        zones.forEach { id ->
            TimeZone.setDefault(TimeZone.getTimeZone(id))
            val now = System.currentTimeMillis()
            // A year either side, so a DST transition is inside the range for the zones that have one.
            listOf(-365L, -90, -31, -2, -1, 0, 1, 45, 365).forEach { offset ->
                val instant = now + offset * 86_400_000L
                assertEquals(
                    "$id offset $offset",
                    localDate(instant),
                    formatEpochDay(epochDayOfContract(instant)),
                )
            }
        }
    }

    @Test
    fun `an epoch day survives a round trip through the calendar`() {
        zones.forEach { id ->
            TimeZone.setDefault(TimeZone.getTimeZone(id))
            val today = todayEpochDay()
            (-400L..400 step 37).forEach { offset ->
                assertEquals("$id offset $offset", today + offset, epochDayToCalendar(today + offset).toEpochDay())
            }
        }
    }

    /**
     * The year appears exactly when it says something. Checked in every zone the rest of this file
     * checks, because "which year is that epoch day in" is the same question the round-trip test
     * exists for and has the same trap in it.
     */
    @Test
    fun `formatMonthYear names the year only for a day outside this one`() {
        zones.forEach { id ->
            TimeZone.setDefault(TimeZone.getTimeZone(id))
            val today = todayEpochDay()

            assertFalse("$id", formatMonthYear(today).any { it.isDigit() })
            assertTrue("$id", formatMonthYear(today - 400).any { it.isDigit() })
        }
    }

    /**
     * The one failure this function can really have is a swapped or scaled field — 13:40 printing
     * as 1:40 AM, or as 1:40 on the 40th hour of the day. Pinned against a fixed locale so the
     * expected strings mean something, and away from the hour a spring-forward skips, which has no
     * wall clock to print in any zone.
     */
    @Test
    fun `formatMinuteOfDay prints the wall clock the minute stands for`() {
        val expected = mapOf(0 to "12:00 AM", 6 * 60 + 30 to "6:30 AM", 12 * 60 to "12:00 PM", 13 * 60 + 40 to "1:40 PM", 23 * 60 + 59 to "11:59 PM")
        Locale.setDefault(Locale.US)
        zones.forEach { id ->
            TimeZone.setDefault(TimeZone.getTimeZone(id))
            expected.forEach { (minute, clock) ->
                // Whitespace-normalised: JDK 20+ separates the AM/PM marker with U+202F, and
                // which space it is is not what this test is about.
                assertEquals("$id minute $minute", clock, formatMinuteOfDay(minute).replace('\u202f', ' ').replace('\u00a0', ' '))
            }
        }
    }
}
