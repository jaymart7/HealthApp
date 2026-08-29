package ph.mart.healthapp.feature.home.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test
import ph.mart.healthapp.core.data.profile.UnitSystem
import ph.mart.healthapp.core.data.streak.StreakStats

class StreakCardTest {

    @Test
    fun `the caption counts down to the next badge from the current run`() {
        assertEquals(
            "2 days to your 14-day badge.",
            captionFor(StreakStats(current = 12, best = 31, totalDaysLogged = 74)),
        )
        assertEquals(
            "1 day to your 3-day badge.",
            captionFor(StreakStats(current = 2, best = 2, totalDaysLogged = 2)),
        )
    }

    @Test
    fun `a broken streak asks for one entry, and a maxed one reports the best run`() {
        assertEquals(
            "Log anything today to start a streak.",
            captionFor(StreakStats(current = 0, best = 4, totalDaysLogged = 9)),
        )
        assertEquals(
            "Best: 118 days.",
            captionFor(StreakStats(current = 118, best = 118, totalDaysLogged = 118)),
        )
    }

    @Test
    fun `the weight line names the earned badge, and converts to the display unit`() {
        assertEquals("5.2 kg toward your goal · 5.0 kg badge earned.", weightLineFor(5.2, UnitSystem.Metric))
        assertEquals("1.5 kg toward your goal.", weightLineFor(1.5, UnitSystem.Metric))
        assertEquals("11.5 lb toward your goal · 11.0 lb badge earned.", weightLineFor(5.2, UnitSystem.Imperial))
    }
}
