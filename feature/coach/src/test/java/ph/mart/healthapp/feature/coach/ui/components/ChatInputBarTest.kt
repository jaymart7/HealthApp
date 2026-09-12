package ph.mart.healthapp.feature.coach.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

/** The mic's one rule: what the field holds after a phrase comes back. */
class ChatInputBarTest {

    @Test
    fun `an empty draft takes the phrase as it is`() {
        assertEquals("how am I doing today", withSpoken("", "how am I doing today"))
    }

    @Test
    fun `a typed draft keeps its words and the phrase follows`() {
        assertEquals("what about dinner tonight", withSpoken("what about", "dinner tonight"))
    }

    @Test
    fun `a draft already ending in a space does not double it`() {
        assertEquals("what about dinner", withSpoken("what about ", "dinner"))
    }

    @Test
    fun `a blank draft is not treated as typing`() {
        assertEquals("dinner ideas", withSpoken("   ", "dinner ideas"))
    }
}
