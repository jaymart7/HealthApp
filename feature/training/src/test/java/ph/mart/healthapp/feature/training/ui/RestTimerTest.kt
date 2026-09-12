package ph.mart.healthapp.feature.training.ui

import org.junit.Assert.assertEquals
import org.junit.Test
import ph.mart.healthapp.feature.training.ui.components.formatRest
import ph.mart.healthapp.feature.training.ui.components.remainingSeconds

/** The rest timer's two pure functions. [formatRest] is a label that stays in Kotlin, and this is
 * what earns it that — the rule the localization gate asks for. */
class RestTimerTest {

    @Test
    fun `a rest rounds up, so it reads its full length for the whole of its first second`() {
        assertEquals(90, remainingSeconds(endAtMillis = 90_000, nowMillis = 0))
        assertEquals(90, remainingSeconds(endAtMillis = 90_000, nowMillis = 1))
        assertEquals(89, remainingSeconds(endAtMillis = 90_000, nowMillis = 1_000))
    }

    @Test
    fun `an end time already past is not a rest`() {
        assertEquals(0, remainingSeconds(endAtMillis = 90_000, nowMillis = 90_000))
        assertEquals(0, remainingSeconds(endAtMillis = 90_000, nowMillis = 400_000))
    }

    @Test
    fun `the clock pads its seconds and counts its minutes`() {
        assertEquals("1:30", formatRest(90))
        assertEquals("0:05", formatRest(5))
        assertEquals("0:00", formatRest(0))
        assertEquals("10:00", formatRest(600))
    }

    @Test
    fun `a negative figure floors rather than printing a minus`() {
        assertEquals("0:00", formatRest(-3))
    }
}
