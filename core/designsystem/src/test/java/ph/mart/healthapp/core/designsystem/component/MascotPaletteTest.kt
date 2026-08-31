package ph.mart.healthapp.core.designsystem.component

import org.junit.Assert.assertEquals
import org.junit.Test

/** The colour half of the mascot's appearance. Its pairs need a composition to resolve, so what a
 * JVM test can cover is the path a restored export or a newer build's name actually takes. */
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
}
