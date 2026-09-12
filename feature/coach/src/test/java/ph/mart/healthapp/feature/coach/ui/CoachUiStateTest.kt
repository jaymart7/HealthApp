package ph.mart.healthapp.feature.coach.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import ph.mart.healthapp.core.data.coach.ChatMessage

/**
 * The one rule a Room emission has to get right while a send is in flight: when the streamed
 * bubbles go. Too early and the finished turn blinks off screen for the frames between the write
 * and the invalidation; never, and the input bar stays locked.
 */
class CoachUiStateTest {

    private fun message(id: Long, fromUser: Boolean) =
        ChatMessage(id = id, fromUser = fromUser, text = "m$id", sentAtMillis = id)

    private val inFlight = CoachUiState(
        loaded = true,
        messages = listOf(message(1, true), message(2, false)),
        pending = "What should I eat tonight?",
        streaming = "You have 600 kcal left and",
    )

    @Test
    fun `the pair landing in Room retires the streamed bubbles`() {
        val next = inFlight.withMessages(
            inFlight.messages + listOf(message(3, true), message(4, false)),
            request = null,
        )
        assertNull(next.pending)
        assertNull(next.streaming)
        assertEquals(4, next.messages.size)
    }

    /** The write has happened but the invalidation has not arrived; the same list comes through
     * for some other reason. Dropping the bubbles here is the blink this guards. */
    @Test
    fun `an unchanged list leaves the turn in flight standing`() {
        val next = inFlight.withMessages(inFlight.messages, request = null)
        assertEquals("What should I eat tonight?", next.pending)
        assertEquals("You have 600 kcal left and", next.streaming)
    }

    /** A clear mid-send shrinks the list. It counts too, or the input bar never re-enables. */
    @Test
    fun `a clear mid-send retires them as well`() {
        val next = inFlight.withMessages(emptyList(), request = null)
        assertNull(next.pending)
        assertNull(next.streaming)
    }

    @Test
    fun `the first emission only marks the conversation loaded`() {
        val next = CoachUiState().withMessages(emptyList(), request = null)
        assertTrue(next.loaded)
        assertNull(next.pending)
        assertNull(next.streaming)
    }
}
