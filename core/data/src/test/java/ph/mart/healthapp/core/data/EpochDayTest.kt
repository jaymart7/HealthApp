package ph.mart.healthapp.core.data

import java.util.Calendar
import java.util.TimeZone
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Two things are worth guarding here.
 *
 * [todayFlow] is a timer, so there is the wait it computes: a zero or negative delay turns the
 * loop into a busy-spin that emits forever and pins a core — and it is computed by subtracting
 * `now` from a `Calendar` walked forward a day, which is exactly the sort of arithmetic that lands
 * on the wrong side of the boundary.
 *
 * [epochDayOf] is the key every dated table in the module uses, so there is the property that
 * makes it a key at all: **one local day, one value, and the next day the next value.** That held
 * by accident until a zone whose standard offset is UTC+0 observed DST — in Europe/London the plain
 * `local midnight / 86_400_000` gave 2026-03-29 and 2026-03-30 the same answer, and `weight_entry`
 * is keyed on `date`, so the 30th's weigh-in overwrote the 29th's. The zone list below is the
 * guard; the suite's own default zone is not, which is how it was missed.
 */
class EpochDayTest {

    private val original: TimeZone = TimeZone.getDefault()

    @After
    fun restoreZone() {
        TimeZone.setDefault(original)
    }

    private val zones =
        listOf("UTC", "Asia/Manila", "America/New_York", "Asia/Kathmandu", "Europe/London")

    private fun noonOn(year: Int, month: Int, day: Int): Calendar =
        Calendar.getInstance().apply {
            clear()
            set(year, month, day, 12, 0)
        }

    @Test
    fun `the wait until the next local midnight is always in the future`() {
        val wait = epochDayStartMillis(todayEpochDay() + 1) - System.currentTimeMillis()
        assertTrue("expected a positive wait, was $wait", wait > 0)
        // A day, plus room for the DST transition that makes one 25 hours long.
        assertTrue("expected under 26h, was $wait", wait <= 26 * 60 * 60 * 1000L)
    }

    @Test
    fun `todayFlow emits today before it waits for anything`() = runBlocking {
        assertEquals(todayEpochDay(), todayFlow().first())
    }

    @Test
    fun `an epoch day round-trips through local midnight`() {
        zones.forEach { id ->
            TimeZone.setDefault(TimeZone.getTimeZone(id))
            val today = todayEpochDay()
            (-400L..400 step 37).forEach { offset ->
                assertEquals(
                    "$id offset $offset",
                    today + offset,
                    epochDayOf(epochDayStartMillis(today + offset)),
                )
            }
        }
    }

    /**
     * Walked a day at a time with [Calendar.add] across a whole year, so both transitions are
     * inside it. At noon, because that is the one hour of the day that exists in every zone on
     * every date.
     */
    @Test
    fun `a local day is one key, and the next day is the next key`() {
        zones.forEach { id ->
            TimeZone.setDefault(TimeZone.getTimeZone(id))
            val day = noonOn(2026, Calendar.JANUARY, 1)
            var previous = epochDayOf(day.timeInMillis)
            repeat(365) {
                day.add(Calendar.DAY_OF_YEAR, 1)
                val key = epochDayOf(day.timeInMillis)
                assertEquals("$id day ${day.get(Calendar.DAY_OF_YEAR)}", previous + 1, key)
                previous = key
            }
        }
    }

    /**
     * The two transitions named explicitly, because a walk that is off by one from the start would
     * still be consecutive. Spring forward: the day after must be a different day. Fall back: the
     * key between them must not be skipped.
     */
    @Test
    fun `the days either side of a transition are distinct and adjacent`() {
        TimeZone.setDefault(TimeZone.getTimeZone("Europe/London"))

        val springBefore = epochDayOf(noonOn(2026, Calendar.MARCH, 29).timeInMillis)
        val springAfter = epochDayOf(noonOn(2026, Calendar.MARCH, 30).timeInMillis)
        assertEquals("spring forward", springBefore + 1, springAfter)

        val autumnBefore = epochDayOf(noonOn(2026, Calendar.OCTOBER, 25).timeInMillis)
        val autumnAfter = epochDayOf(noonOn(2026, Calendar.OCTOBER, 26).timeInMillis)
        assertEquals("fall back", autumnBefore + 1, autumnAfter)
    }

    /**
     * The weekday a key falls on has to survive the same two days: [weekdayIndex] is what every
     * routine's day mask and the week budget read, and it is built on [epochDayStartMillis].
     */
    @Test
    fun `weekdayIndex advances by one across a transition`() {
        TimeZone.setDefault(TimeZone.getTimeZone("Europe/London"))
        listOf(noonOn(2026, Calendar.MARCH, 29), noonOn(2026, Calendar.OCTOBER, 25)).forEach { day ->
            val key = epochDayOf(day.timeInMillis)
            assertEquals((weekdayIndex(key) + 1) % DAYS_IN_WEEK, weekdayIndex(key + 1))
        }
    }
}
