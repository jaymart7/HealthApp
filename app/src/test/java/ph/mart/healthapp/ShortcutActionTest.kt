package ph.mart.healthapp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** The launcher shortcut's extra resolves here and nowhere else, so this is the whole of what a
 * bad or stale extra can do. */
class ShortcutActionTest {

    @Test
    fun `a known name resolves`() {
        assertEquals(ShortcutAction.AddWater, shortcutActionOf("AddWater"))
        assertEquals(ShortcutAction.SpeakFood, shortcutActionOf("SpeakFood"))
    }

    /** A shortcut pinned by an older build, or a typo in the resource: null, never a crash — the
     * reading `mascotCharacterOf` gives an unknown `Profile.mascotName`. */
    @Test
    fun `an unknown name and a missing extra both degrade to null`() {
        assertNull(shortcutActionOf("LogSomethingElse"))
        assertNull(shortcutActionOf("addwater"))
        assertNull(shortcutActionOf(null))
    }
}
