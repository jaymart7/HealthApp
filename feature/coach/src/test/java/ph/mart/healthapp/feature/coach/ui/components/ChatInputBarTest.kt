package ph.mart.healthapp.feature.coach.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** The bar's two pure rules: what the field holds after a phrase comes back, and when a send is
 * available at all — the second is what the button and the keyboard's action key share. */
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

    @Test
    fun `a typed draft with no turn running can be sent`() {
        assertTrue(canSend("how am I doing today", sending = false))
    }

    @Test
    fun `an empty or blank draft cannot`() {
        assertFalse(canSend("", sending = false))
        assertFalse(canSend("   ", sending = false))
    }

    /** The keyboard is still up while an answer streams, and the button it would race has been
     * replaced by Stop. Without this the return key starts a second turn. */
    @Test
    fun `a draft typed while a turn runs cannot`() {
        assertFalse(canSend("and dinner?", sending = true))
    }
}
