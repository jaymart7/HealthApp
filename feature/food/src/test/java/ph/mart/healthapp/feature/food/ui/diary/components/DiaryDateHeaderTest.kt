package ph.mart.healthapp.feature.food.ui.diary.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DiaryDateHeaderTest {

    private val today = 20_000L

    @Test
    fun `the selected day is Today`() {
        assertEquals("Today", diaryDateLabel(today, today))
    }

    @Test
    fun `one day back is Yesterday`() {
        assertEquals("Yesterday", diaryDateLabel(today - 1, today))
    }

    @Test
    fun `anything older falls back to a calendar date`() {
        // Not asserted verbatim: the format follows the device locale.
        val label = diaryDateLabel(today - 2, today)
        assertTrue(label, label.any { it.isDigit() })
        assertTrue(label, label !in setOf("Today", "Yesterday"))
    }
}
