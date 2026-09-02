package ph.mart.healthapp.wear

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import ph.mart.healthapp.core.today.TodaySnapshot
import ph.mart.healthapp.wear.ui.WearTodayUiState
import ph.mart.healthapp.wear.ui.isStale

/**
 * The watch's one piece of arithmetic: deciding whether what the phone last pushed still
 * describes today. Everything else on this screen is a field read off the snapshot.
 */
class WearTodayStateTest {

    @Test
    fun `a snapshot for today is not stale`() {
        val state = WearTodayUiState(loaded = true, snapshot = TodaySnapshot(dateEpochDay = 20_000))
        assertFalse(state.isStale(20_000))
    }

    @Test
    fun `a snapshot from yesterday is stale`() {
        val state = WearTodayUiState(loaded = true, snapshot = TodaySnapshot(dateEpochDay = 19_999))
        assertTrue(state.isStale(20_000))
    }

    /** Nothing pushed is the "open FitPulse on your phone" state, not a stale one — the note
     * would sit under numbers that aren't there. */
    @Test
    fun `no snapshot is never reported as stale`() {
        assertFalse(WearTodayUiState(loaded = true, snapshot = null).isStale(20_000))
    }
}
