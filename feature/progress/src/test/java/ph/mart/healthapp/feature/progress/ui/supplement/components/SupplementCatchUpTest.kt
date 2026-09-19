package ph.mart.healthapp.feature.progress.ui.supplement.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/** The two relative words are what keep `catchUpDateLabel` in Kotlin — the exemption a label
 * earns by having a test over its wording, the reading `diaryDateLabel` got. */
class SupplementCatchUpTest {

    private val today = 20_000L

    @Test
    fun `today is named`() {
        assertEquals("Today", catchUpDateLabel(today, today))
    }

    @Test
    fun `yesterday is named`() {
        assertEquals("Yesterday", catchUpDateLabel(today - 1, today))
    }

    @Test
    fun `anything older is an absolute date`() {
        val label = catchUpDateLabel(today - 2, today)
        assertNotEquals("Today", label)
        assertNotEquals("Yesterday", label)
        assertEquals(true, label.any { it.isDigit() })
    }
}
