package ph.mart.healthapp.feature.food.ui.voice

import org.junit.Assert.assertEquals
import org.junit.Test

/** The join [withSpoken] makes — the one rule on this screen that can be silently wrong. */
class VoiceSentenceTest {

    @Test
    fun `the first phrase is the sentence`() {
        assertEquals("two scrambled eggs", withSpoken("", "two scrambled eggs"))
    }

    @Test
    fun `a second phrase is added, not substituted`() {
        assertEquals(
            "two scrambled eggs, a black coffee",
            withSpoken("two scrambled eggs", "a black coffee"),
        )
    }

    @Test
    fun `a sentence that already ends in a comma does not collect a second one`() {
        assertEquals(
            "two scrambled eggs, a black coffee",
            withSpoken("two scrambled eggs, ", "a black coffee"),
        )
    }

    @Test
    fun `an empty transcript leaves the sentence exactly as it was`() {
        assertEquals("two scrambled eggs", withSpoken("two scrambled eggs", "   "))
    }

    /** A dialog that heard nothing at all against a field nobody has typed in. */
    @Test
    fun `nothing said into nothing typed is still nothing`() {
        assertEquals("", withSpoken("", ""))
    }
}
