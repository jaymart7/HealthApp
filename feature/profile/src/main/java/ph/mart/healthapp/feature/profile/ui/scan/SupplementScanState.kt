package ph.mart.healthapp.feature.profile.ui.scan

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import ph.mart.healthapp.core.data.supplement.Supplement
import ph.mart.healthapp.core.data.supplement.SupplementLabelReading

/**
 * [Unreadable] is the photo with no panel in it and [Retry] is the call that didn't work — kept
 * apart for the reason `SupplementScanResult` keeps them apart, and they say different things.
 *
 * There is no manual-entry state, which is the one place this diverges from the label flow: the
 * screen that adds a supplement by hand is the list this one was opened from, one back press away,
 * and a second copy of it here would be a second save path to the same table.
 */
enum class SupplementFlow { Capture, Reading, Confirmation, Unreadable, Retry, Offline, PermissionDenied }

@Composable
internal fun rememberSupplementScanScreen(): SupplementScanScreenState =
    remember { SupplementScanScreenState() }

/** Screen-local flow state, plain `remember` for `LabelScanScreenState`'s reason: a half-finished
 * scan isn't meaningful to restore across process death. */
internal class SupplementScanScreenState(flow: SupplementFlow = SupplementFlow.Capture) {
    var flow: SupplementFlow by mutableStateOf(flow)

    /** What the sheet is seeded with, and null until a panel has been read. `id` is 0 throughout:
     * this flow only ever adds. */
    var reading: Supplement? by mutableStateOf(null)

    fun applyReading(read: SupplementLabelReading) {
        reading = Supplement(
            // A panel photographed on its own prints no product name. Empty and typable is the
            // honest seed — the sheet already refuses to save a nameless supplement.
            name = read.name.orEmpty(),
            dose = read.dose.orEmpty(),
            // The label's own directions where it states them, and the app's default of once
            // where it doesn't. Not a guess either way: a bottle that says nothing about
            // frequency is a supplement the user will set themselves.
            timesPerDay = read.timesPerDay ?: 1,
            nutrients = read.nutrients,
            panel = read.panel,
        )
        flow = SupplementFlow.Confirmation
    }

    fun recapture() {
        reading = null
        flow = SupplementFlow.Capture
    }
}
