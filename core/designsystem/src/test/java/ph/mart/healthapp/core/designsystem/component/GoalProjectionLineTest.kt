package ph.mart.healthapp.core.designsystem.component

import org.junit.Assert.assertEquals
import org.junit.Test

class GoalProjectionLineTest {

    @Test
    fun `arrival wins over any date`() {
        assertEquals(
            "You're at your goal weight.",
            goalProjectionLine("72 kg", targetEpochDay = 20_100, reached = true, windowDays = 30),
        )
    }

    @Test
    fun `a date names the window it was fitted over`() {
        // The date itself is formatted by formatEpochDay, which DateFormatTest already pins —
        // what matters here is that the window and the goal both reach the sentence.
        val line = goalProjectionLine("72 kg", targetEpochDay = 20_100, reached = false, windowDays = 30)
        assertEquals("On the last 30 days' trend, 72 kg around ${formatEpochDay(20_100)}.", line)
    }

    @Test
    fun `no date says so rather than guessing one`() {
        assertEquals(
            "No date to project at this pace.",
            goalProjectionLine("159 lb", targetEpochDay = null, reached = false, windowDays = 30),
        )
    }
}
