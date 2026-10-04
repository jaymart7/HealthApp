package ph.mart.healthapp.feature.food.ui.diary.components

import org.junit.Assert.assertEquals
import org.junit.Test
import ph.mart.healthapp.core.data.Phrase
import ph.mart.healthapp.core.data.phrase
import ph.mart.healthapp.core.designsystem.component.formatEpochDay
import ph.mart.healthapp.feature.food.R

class DiaryDateHeaderTest {

    private val today = 20_000L

    @Test
    fun `the selected day is Today`() {
        assertEquals(phrase(R.string.food_today), diaryDateLabel(today, today))
    }

    @Test
    fun `one day back is Yesterday`() {
        assertEquals(phrase(R.string.food_yesterday), diaryDateLabel(today - 1, today))
    }

    @Test
    fun `anything older falls back to a calendar date`() {
        assertEquals(Phrase.Raw(formatEpochDay(today - 2)), diaryDateLabel(today - 2, today))
    }
}
