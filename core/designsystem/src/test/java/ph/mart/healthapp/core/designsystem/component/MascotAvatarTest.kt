package ph.mart.healthapp.core.designsystem.component

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

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

    /** Everything the mascot does is built from this, and this is why a channel is staggered by
     * harmonic rather than by a phase offset: an offset would leave a sparkle or a thinking dot
     * somewhere arbitrary at rest. */
    @Test
    fun `every harmonic is neutral at both ends of the cycle`() {
        HARMONICS.forEach { harmonic ->
            assertEquals("harmonic $harmonic at 0f", 0f, pulse(0f, harmonic), 0.001f)
            assertEquals("harmonic $harmonic at 1f", 0f, pulse(1f, harmonic), 0.001f)
        }
    }

    @Test
    fun `the harmonics are not in step with each other`() {
        // Not a claim about any one instant — two sine waves coincide at plenty of them, and phase
        // 0.125 is one where harmonics 1 and 3 agree exactly. What has to hold is that they are
        // three *different* waves, so three sparkles never pulse as one.
        val samples = (0..200).map { step -> HARMONICS.map { pulse(step / 200f, it) } }
        HARMONICS.indices.forEach { a ->
            (a + 1..HARMONICS.lastIndex).forEach { b ->
                assertTrue(
                    "harmonics ${HARMONICS[a]} and ${HARMONICS[b]} never separate",
                    samples.any { abs(it[a] - it[b]) > 0.5f },
                )
            }
        }
    }

    /** The guard that matters most: a state added later cannot quietly skip the rest-pose rule. */
    @Test
    fun `every state rests in the identity pose`() {
        MascotState.entries.forEach { state ->
            val motion = mascotMotion(state, 1f)
            assertEquals("$state translate", 0f, motion.translateFraction, 0.001f)
            assertEquals("$state rotation", 0f, motion.rotationDeg, 0.001f)
            assertEquals("$state scaleX", 1f, motion.scaleX, 0.001f)
            assertEquals("$state scaleY", 1f, motion.scaleY, 0.001f)
        }
    }

    /** Full character, not an unbounded one: a hop is a tenth of the avatar and a tilt is degrees,
     * so a 24dp chat avatar performs the same routine without leaving its row. */
    @Test
    fun `no state leaves its envelope`() {
        MascotState.entries.forEach { state ->
            (0..200).forEach { step ->
                val motion = mascotMotion(state, step / 200f)
                assertTrue(
                    "$state translated to ${motion.translateFraction}",
                    motion.translateFraction in -0.11f..0.05f,
                )
                assertTrue("$state rotated to ${motion.rotationDeg}", motion.rotationDeg in -6f..6f)
                assertTrue("$state scaleX ${motion.scaleX}", motion.scaleX in 0.94f..1.06f)
                assertTrue("$state scaleY ${motion.scaleY}", motion.scaleY in 0.94f..1.06f)
            }
        }
    }

    /** Celebrating hops *up*, and stretches while it is in the air rather than squashing on the
     * ground — the ground is where phase `1f` leaves it, and the ground has to be neutral. */
    @Test
    fun `celebrating leaves the floor and comes back to it`() {
        val apex = mascotMotion(MascotState.Celebrating, 0.25f)
        assertTrue("apex was ${apex.translateFraction}", apex.translateFraction < -0.09f)
        assertTrue("apex scaleY was ${apex.scaleY}", apex.scaleY > 1f)
        assertEquals(0f, mascotMotion(MascotState.Celebrating, 0.5f).translateFraction, 0.001f)
    }

    /** The sleepy "z" is absent at rest, not frozen halfway up. */
    @Test
    fun `the sleep z fades to nothing at both ends`() {
        assertEquals(0f, zAlpha(0f), 0.001f)
        assertEquals(0f, zAlpha(1f), 0.001f)
        assertTrue(zAlpha(0.5f) > 0.9f)
    }

    private companion object {
        /** The three the sparkles, the thinking dots and the head tilt actually ride. */
        val HARMONICS = listOf(1, 2, 3)
    }
}
