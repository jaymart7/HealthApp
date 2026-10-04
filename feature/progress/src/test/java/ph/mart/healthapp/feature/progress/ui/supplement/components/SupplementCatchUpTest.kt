package ph.mart.healthapp.feature.progress.ui.supplement.components

import org.junit.Assert.assertEquals
import org.junit.Test
import ph.mart.healthapp.core.data.Phrase
import ph.mart.healthapp.core.data.phrase
import ph.mart.healthapp.core.designsystem.component.formatEpochDay
import ph.mart.healthapp.feature.progress.R

/** Which of the three a day gets; the two relative words are `progress_catchup_*`. */
class SupplementCatchUpTest {

    private val today = 20_000L

    @Test
    fun `today is named`() {
        assertEquals(phrase(R.string.progress_catchup_today), catchUpDateLabel(today, today))
    }

    @Test
    fun `yesterday is named`() {
        assertEquals(phrase(R.string.progress_catchup_yesterday), catchUpDateLabel(today - 1, today))
    }

    @Test
    fun `anything older is an absolute date`() {
        assertEquals(Phrase.Raw(formatEpochDay(today - 2)), catchUpDateLabel(today - 2, today))
    }
}
