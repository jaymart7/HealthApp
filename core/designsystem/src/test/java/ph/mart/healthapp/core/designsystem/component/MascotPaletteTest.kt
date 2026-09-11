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
     * The one thing the hue table can break silently. Body and feature are the same hue at two
     * lightnesses, and HSL lightness is not perceptual — yellow around 54° is far brighter at
     * `0.72` than blue is at the same number, so the two constants that look safe on a green buddy
     * are the ones that flatten a yellow one's eyes into its face.
     *
     * The check is also what the picker's tick mark rides on: `mascotFeatureColor` draws it over
     * the swatch, so the ratio asserted here is the ratio that mark is legible at.
     */
    @Test
    fun `every hue draws a face that clears 4_5 to 1 against its body`() {
        val hues = MascotPalette.entries.mapNotNull { it.hue }
        assertEquals(30, hues.size)
        hues.forEach { hue ->
            val colors = hueColors(hue)
            val ratio = contrastRatio(colors.body.luminance(), colors.feature.luminance())
            assertTrue("hue $hue only reaches $ratio:1", ratio >= 4.5f)
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
}
