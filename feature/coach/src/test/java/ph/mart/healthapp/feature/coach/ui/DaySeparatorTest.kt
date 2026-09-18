package ph.mart.healthapp.feature.coach.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import ph.mart.healthapp.core.data.coach.ChatMessage
import ph.mart.healthapp.core.data.epochDayOf
import ph.mart.healthapp.core.data.epochDayStartMillis
import ph.mart.healthapp.core.data.todayEpochDay

/**
 * Where a day label goes in a transcript.
 *
 * The rule is an off-by-one waiting to happen and the screen is the worst place to find one — a
 * separator drawn under the wrong bubble is a label nobody reads as wrong, they just read the
 * conversation wrong. Timestamps are built from [epochDayStartMillis] rather than from
 * `day * 86_400_000`, so the test asks the same question about local midnight the production code
 * does and stays right across a DST boundary.
 */
class DaySeparatorTest {

    private fun message(id: Long, epochDay: Long, hour: Int = 9) = ChatMessage(
        id = id,
        fromUser = id % 2 == 1L,
        text = "m$id",
        sentAtMillis = epochDayStartMillis(epochDay) + hour * 3_600_000L,
    )

    @Test
    fun `the first message always opens a day`() {
        val today = todayEpochDay()
        val messages = listOf(message(1, today))
        assertEquals(today, daySeparatorAt(messages, 0))
    }

    @Test
    fun `a message on the same day as the one above it opens nothing`() {
        val today = todayEpochDay()
        val messages = listOf(message(1, today, hour = 8), message(2, today, hour = 21))
        assertNull(daySeparatorAt(messages, 1))
    }

    @Test
    fun `a message on a new day opens it`() {
        val today = todayEpochDay()
        val messages = listOf(
            message(1, today - 3),
            message(2, today - 3),
            message(3, today),
        )
        assertEquals(today - 3, daySeparatorAt(messages, 0))
        assertNull(daySeparatorAt(messages, 1))
        assertEquals(today, daySeparatorAt(messages, 2))
    }

    /** Minutes either side of local midnight are two days, not one — which is the case a naive
     * "same rounded millis" comparison gets wrong. */
    @Test
    fun `midnight is a boundary`() {
        val today = todayEpochDay()
        val justBefore = ChatMessage(1, true, "late", epochDayStartMillis(today) - 60_000L)
        val justAfter = ChatMessage(2, false, "early", epochDayStartMillis(today) + 60_000L)
        val messages = listOf(justBefore, justAfter)
        assertEquals(epochDayOf(justBefore.sentAtMillis), daySeparatorAt(messages, 0))
        assertEquals(today, daySeparatorAt(messages, 1))
    }

    @Test
    fun `an index off the end is null rather than a crash`() {
        assertNull(daySeparatorAt(emptyList(), 0))
        assertNull(daySeparatorAt(listOf(message(1, todayEpochDay())), 4))
    }
}
