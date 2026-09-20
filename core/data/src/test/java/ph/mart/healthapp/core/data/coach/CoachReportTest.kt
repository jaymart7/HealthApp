package ph.mart.healthapp.core.data.coach

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test
import ph.mart.healthapp.core.data.recap.REPORT_DAYS

/**
 * The trust boundary on `show_report`, which is smaller than every other tool's and still worth a
 * test: it is the one call that puts a *surface* on screen rather than a sentence or a draft, and
 * the window it names is the whole of what the model gets to decide.
 *
 * The rest of the card cannot be wrong in a way a JVM test can see — every figure on it comes out
 * of `recap()`, which `RecapTest` already holds.
 */
class CoachReportTest {

    private fun args(vararg pairs: Pair<String, JsonElement>) = mapOf(*pairs)

    @Test
    fun `both windows parse`() {
        REPORT_DAYS.forEach { days ->
            assertEquals(days, parseShowReport(args("days" to JsonPrimitive(days))))
        }
    }

    /**
     * Not clamped to the nearest legal window, which is the point of the test rather than a
     * detail of it: rounding 14 to 7 invents an intent, and the turn failing is what the user
     * sees instead of a card headed with a window they did not ask for.
     */
    @Test
    fun `a window between the two is not rounded to either`() {
        assertNull(parseShowReport(args("days" to JsonPrimitive(14))))
    }

    @Test
    fun `a year is out of reach`() {
        // The span `get_history` cannot read either — see REPORT_DAYS.
        assertNull(parseShowReport(args("days" to JsonPrimitive(365))))
    }

    @Test
    fun `zero, a negative and a missing window all fail`() {
        assertNull(parseShowReport(args("days" to JsonPrimitive(0))))
        assertNull(parseShowReport(args("days" to JsonPrimitive(-7))))
        assertNull(parseShowReport(emptyMap()))
    }

    /**
     * The quoting tolerance every other tool's integers get, pinned here too: Gemini returns
     * numbers as JSON numbers but has been known to quote them, and `"30"` is a formatting wobble
     * rather than a wrong answer. A word is still a word.
     */
    @Test
    fun `a quoted window is read, a word is not`() {
        assertEquals(30, parseShowReport(args("days" to JsonPrimitive("30"))))
        assertNull(parseShowReport(args("days" to JsonPrimitive("a month"))))
    }

    /**
     * The whole reason the repository's loop can treat it apart from both other kinds: it is not
     * a draft, so nothing about it reaches a Confirm, and `parseAction` has no branch for it.
     */
    @Test
    fun `it is not a write tool and drafts no action`() {
        assertFalse(TOOL_SHOW_REPORT in WRITE_TOOLS)
        assertNull(parseAction(TOOL_SHOW_REPORT, args("days" to JsonPrimitive(30)), today = 20_000L))
    }
}
