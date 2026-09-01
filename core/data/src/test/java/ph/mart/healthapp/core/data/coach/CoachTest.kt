package ph.mart.healthapp.core.data.coach

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** The coach's reply is rendered verbatim into a bubble, so every way it can be wrong is checked
 * here — the same reason `InsightTest` exists for the one-line version. */
class CoachTest {

    @Test
    fun `a plain answer passes through`() {
        assertEquals(
            "You're 320 kcal under target, so dinner has room.",
            sanitizeReply("You're 320 kcal under target, so dinner has room."),
        )
    }

    @Test
    fun `surrounding quotes and stray whitespace are stripped`() {
        assertEquals("Protein is lagging today.", sanitizeReply("  \"Protein is lagging today.\"\n"))
    }

    /** Unlike the one-line insight, a paragraph break is a legitimate part of an answer — only
     * runs of spaces and tabs collapse. */
    @Test
    fun `line breaks survive but runs of spaces do not`() {
        assertEquals(
            "You have 600 kcal left.\nA chicken bowl would fit.",
            sanitizeReply("You have   600 kcal left.  \n   A chicken bowl would fit."),
        )
    }

    @Test
    fun `nothing at all is no reply`() {
        assertNull(sanitizeReply(null))
        assertNull(sanitizeReply("   \n "))
    }

    /** Rejected rather than truncated, for the reason the insight's cap gives: half a sentence
     * reads as a bug, and the rule-based line it falls back to is always complete. */
    @Test
    fun `an answer past the cap is rejected`() {
        assertNull(sanitizeReply("a".repeat(MAX_REPLY_CHARS + 1)))
        assertEquals("b".repeat(MAX_REPLY_CHARS), sanitizeReply("b".repeat(MAX_REPLY_CHARS)))
    }
}
