package ph.mart.healthapp.core.data

import java.util.Locale
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReplyLanguageTest {

    @Test
    fun `English needs no line`() {
        assertNull(replyLanguageLine(Locale.US))
        assertNull(replyLanguageLine(Locale.UK))
    }

    @Test
    fun `Filipino names the language and keeps units and saved names English`() {
        val line = replyLanguageLine(Locale.forLanguageTag("fil-PH"))!!
        assertTrue(line, "Filipino" in line)
        assertTrue(line, "portion unit" in line)
        assertTrue(line, "saved food" in line)
    }

    /** `tl` is how older devices and every Google library name the same language. */
    @Test
    fun `Tagalog reads as an instruction too`() {
        assertTrue(replyLanguageLine(Locale.forLanguageTag("tl"))!!.contains("Tagalog"))
    }
}
