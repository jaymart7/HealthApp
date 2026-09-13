package ph.mart.healthapp.feature.coach.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import ph.mart.healthapp.core.data.coach.ChatMessage

/**
 * Which answer offers to be re-asked.
 *
 * The rule is narrow on purpose: re-asking an old turn appends a fresh pair at the bottom and
 * buries the answer the user was looking at, so only the newest one offers it — and never while a
 * turn is in flight, since that is the same send the locked input bar is refusing.
 */
class AskAgainTest {

    private fun message(id: Long, fromUser: Boolean, text: String = "m$id") =
        ChatMessage(id = id, fromUser = fromUser, text = text, sentAtMillis = id)

    private val conversation = CoachUiState(
        loaded = true,
        messages = listOf(
            message(1, true, "How am I doing?"),
            message(2, false),
            message(3, true, "What should I eat tonight?"),
            message(4, false),
        ),
    )

    @Test
    fun `the newest answer re-asks the question above it`() {
        assertEquals("What should I eat tonight?", conversation.askAgainQuestion(3))
    }

    @Test
    fun `an older answer does not`() {
        assertNull(conversation.askAgainQuestion(1))
    }

    /** The user's own bubble has no menu at all, so its index never qualifies. */
    @Test
    fun `a question is not something to re-ask from`() {
        assertNull(conversation.askAgainQuestion(2))
    }

    @Test
    fun `nothing re-asks while a turn is in flight`() {
        assertNull(conversation.copy(pending = "Anything else?").askAgainQuestion(3))
    }

    /** An answer with no question above it is an unpaired row — nothing to send again. */
    @Test
    fun `an answer with no question above it offers nothing`() {
        val orphan = CoachUiState(loaded = true, messages = listOf(message(1, false)))
        assertNull(orphan.askAgainQuestion(0))
    }
}
