package ph.mart.healthapp.core.designsystem.component

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The mascot's idle loop. Both phases run 0f..1f and are pinned at `1f` when the user turns on
 * *Remove animations* — Compose skips an infinite transition to its end value and suspends there —
 * so the rest pose is what phase `1f` draws, not what phase `0f` does.
 */
class MascotAvatarTest {

    /** The regression: with animations off the mascot must sit exactly as it did before this loop
     * existed — eyes open, on the baseline — not parked mid-blink or floating. */
    @Test
    fun `the end of the cycle is the resting pose`() {
        assertFalse(isBlinking(1f))
        assertEquals(0f, bobOffset(1f), 0.001f)
    }

    @Test
    fun `the eyes are open for most of the cycle`() {
        assertFalse(isBlinking(0f))
        assertFalse(isBlinking(0.5f))
        assertTrue(isBlinking(0.96f))
    }

    @Test
    fun `the bob never leaves its amplitude`() {
        (0..100).forEach { step ->
            val offset = bobOffset(step / 100f)
            assertTrue("phase ${step / 100f} drifted to $offset", offset in -1f..1f)
        }
    }
}
