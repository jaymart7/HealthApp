package ph.mart.healthapp.core.data.insight

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** The model's answer is the one string in this app that comes from outside and is rendered
 * verbatim, so every way it can be wrong is checked here. */
class InsightTest {

    @Test
    fun `a plain sentence passes through`() {
        assertEquals(
            "You're 320 kcal under your target — a solid dinner still fits.",
            sanitizeInsight("You're 320 kcal under your target — a solid dinner still fits."),
        )
    }

    @Test
    fun `surrounding quotes and stray whitespace are stripped`() {
        assertEquals("Protein is lagging today.", sanitizeInsight("  \"Protein is lagging today.\"\n"))
    }

    @Test
    fun `a wrapped answer collapses to one line`() {
        assertEquals(
            "Nine days logged in a row — keep it going.",
            sanitizeInsight("Nine days logged\n  in a row — keep it going."),
        )
    }

    /** The prompt's own escape hatch: nothing worth saying must not displace the rule-based line. */
    @Test
    fun `NONE is no insight`() {
        assertNull(sanitizeInsight("NONE"))
        assertNull(sanitizeInsight("none."))
    }

    @Test
    fun `nothing at all is no insight`() {
        assertNull(sanitizeInsight(null))
        assertNull(sanitizeInsight("   \n "))
    }

    /** A model that wrote a paragraph is rejected rather than truncated — half a sentence reads
     * as a bug, and the line it would displace is always complete. */
    @Test
    fun `an answer past the cap is rejected`() {
        assertNull(sanitizeInsight("a".repeat(MAX_INSIGHT_CHARS + 1)))
        assertEquals("b".repeat(MAX_INSIGHT_CHARS), sanitizeInsight("b".repeat(MAX_INSIGHT_CHARS)))
    }
}
