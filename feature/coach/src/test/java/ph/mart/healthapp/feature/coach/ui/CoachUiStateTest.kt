package ph.mart.healthapp.feature.coach.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import ph.mart.healthapp.core.data.coach.ChatMessage
import ph.mart.healthapp.core.data.coach.CoachAction

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

    private val awaitingTap = inFlight.copy(
        pending = "Log a glass of water",
        streaming = "Sure — here it is:",
        proposal = CoachAction.LogWater(glasses = 1),
    )

    /**
     * A proposal is retired by the same emission the other two are, because the user's tap is what
     * eventually causes the write: confirm or dismiss, the pair lands and the whole turn in flight
     * goes at once. Retiring it any earlier would drop the card out from under the finger.
     */
    @Test
    fun `the pair landing retires the proposal too`() {
        val next = awaitingTap.withMessages(
            awaitingTap.messages + listOf(message(3, true), message(4, false)),
            request = null,
        )
        assertNull(next.proposal)
        assertNull(next.pending)
        assertNull(next.streaming)
    }

    /** Nothing has been written while the card is up — that is the whole point of a proposal — so
     * an unrelated emission must leave it standing. */
    @Test
    fun `an unchanged list leaves the proposal standing`() {
        val next = awaitingTap.withMessages(awaitingTap.messages, request = null)
        assertEquals(CoachAction.LogWater(glasses = 1), next.proposal)
    }

    /** A clear while the card is up shrinks the list. It takes the card with it, or the input bar
     * stays locked behind a proposal whose conversation is gone. */
    @Test
    fun `a clear mid-proposal retires it as well`() {
        val next = awaitingTap.withMessages(emptyList(), request = null)
        assertNull(next.proposal)
        assertNull(next.pending)
    }

    /**
     * The stop button, and a proposal dismissed with no prose behind it: both end a turn that
     * wrote nothing, so all three in-flight fields have to go at once or the input bar stays
     * locked with no conversation under it.
     */
    @Test
    fun `abandoning a turn clears every in-flight field`() {
        val next = awaitingTap.withTurnAbandoned()
        assertNull(next.pending)
        assertNull(next.streaming)
        assertNull(next.proposal)
        assertEquals(awaitingTap.messages, next.messages)
    }

    @Test
    fun `the first emission only marks the conversation loaded`() {
        val next = CoachUiState().withMessages(emptyList(), request = null)
        assertTrue(next.loaded)
        assertNull(next.pending)
        assertNull(next.streaming)
        assertNull(next.proposal)
    }
}
