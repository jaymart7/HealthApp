package ph.mart.healthapp.core.designsystem.component

import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** The colour half of the mascot's appearance. The five theme pairs need a composition to resolve,
 * so what a JVM test can cover is the path a restored export takes and — since the colour became a
 * table of thirty hue angles — whether those angles still draw a face anyone can see. */
class MascotPaletteTest {

    @Test
    fun `every palette round-trips through its own name`() {
        MascotPalette.entries.forEach { palette ->
            assertEquals(palette, mascotPaletteOf(palette.name))
        }
    }

    @Test
    fun `an absent or unrecognised name is the default`() {
        // Null is every install that predates the colour picker, and every profile untouched since.
        assertEquals(MascotPalette.Soft, mascotPaletteOf(null))
        assertEquals(MascotPalette.Soft, mascotPaletteOf(""))
        assertEquals(MascotPalette.Soft, mascotPaletteOf("Nonexistent"))
        assertEquals(MascotPalette.Soft, mascotPaletteOf("soft"))
    }

    /**
     * The bug this table shipped with once: thirty hues evenly spaced is 12° a step, and twelve
     * degrees of a pale fill is a swatch nobody can tell from the one beside it — a whole row of
     * the picker read as one colour.
     *
     * Declaration order is the grid's order, so "next to each other" is literally `entries` in
     * sequence. Two neighbours have to differ by a whole tier or by a family's worth of hue; the
     * table is built as fifteen families of two, alternating, so every pair satisfies one or the
     * other. This fails the moment someone packs a thirty-first colour in between.
     */
    @Test
    fun `no two colours next to each other in the grid look alike`() {
        val hues = MascotPalette.entries.filter { it.hue != null }
        assertEquals(30, hues.size)
        hues.zipWithNext { a, b ->
            val apart = hueDistance(a.hue!!, b.hue!!)
            assertTrue(
                "${a.name} and ${b.name} are the same tier and only $apart° apart",
                a.vivid != b.vivid || apart >= MIN_HUE_STEP,
            )
        }
    }

    /** And the families themselves are spread, so the fifteen pale ones read as fifteen colours
     * rather than a gradient — same for the fifteen vivid. */
    @Test
    fun `each tier spreads its fifteen hues around the wheel`() {
        MascotPalette.entries.filter { it.hue != null }.groupBy { it.vivid }.forEach { (_, tier) ->
            assertEquals(15, tier.size)
            tier.zipWithNext { a, b ->
                val apart = hueDistance(a.hue!!, b.hue!!)
                assertTrue("${a.name} and ${b.name} are only $apart° apart", apart >= MIN_HUE_STEP)
            }
        }
    }

    /**
     * The other thing the hue table can break silently. Every body carries the same near-black
     * face, which only works while no tier goes dark — and a saturated blue *is* dark: at the
     * vivid tier's first lightness, `Red`, `Blue` and `Indigo` all fell under 4.5:1 and nothing on
     * screen said so louder than a slightly muddy buddy.
     *
     * The check is also what the picker's tick mark rides on: `mascotFeatureColor` draws it over
     * the swatch, so the ratio asserted here is the ratio that mark is legible at.
     */
    @Test
    fun `every colour draws a face that clears 4_5 to 1 against its body`() {
        MascotPalette.entries.filter { it.hue != null }.forEach { palette ->
            val colors = hueColors(palette.hue!!, palette.vivid)
            val ratio = contrastRatio(colors.body.luminance(), colors.feature.luminance())
            assertTrue("${palette.name} only reaches $ratio:1", ratio >= 4.5f)
        }
    }

    /** A missed string here is a `Resources$NotFoundException` on the Profile screen, not a compile
     * error — three of those shipped once already. */
    @Test
    fun `every palette names itself`() {
        MascotPalette.entries.forEach { palette ->
            assertNotEquals("${palette.name} has no label", 0, palette.labelRes)
        }
    }

    private fun contrastRatio(a: Float, b: Float): Float =
        (maxOf(a, b) + 0.05f) / (minOf(a, b) + 0.05f)

    /** Degrees apart the short way round, so 350° and 5° are 15 apart and not 345. */
    private fun hueDistance(a: Float, b: Float): Float {
        val raw = kotlin.math.abs(a - b) % 360f
        return minOf(raw, 360f - raw)
    }

    private companion object {
        /** Below this two fills of the same tier are one colour to the eye. */
        const val MIN_HUE_STEP = 15f
    }
}
