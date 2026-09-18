package ph.mart.healthapp.core.data.note

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NoteTest {

    @Test
    fun `surrounding whitespace is trimmed off`() {
        assertEquals("Long day on site.", "  Long day on site.\n".toNoteText())
    }

    @Test
    fun `a note of nothing but whitespace is a cleared one`() {
        assertTrue("   \n\t ".toNoteText().isEmpty())
    }

    @Test
    fun `a note longer than the cap is cut to it`() {
        val long = "a".repeat(NOTE_MAX_CHARS + 50)
        assertEquals(NOTE_MAX_CHARS, long.toNoteText().length)
    }

    /** The trim happens first, so padding cannot spend the budget a note is measured against. */
    @Test
    fun `the cap counts the trimmed text`() {
        val padded = " " + "a".repeat(NOTE_MAX_CHARS) + "    "
        assertEquals(NOTE_MAX_CHARS, padded.toNoteText().length)
    }
}
