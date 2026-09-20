package ph.mart.healthapp.feature.profile.ui.scan

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import ph.mart.healthapp.core.data.supplement.Supplement
import ph.mart.healthapp.core.data.supplement.SupplementLabelReading
import ph.mart.healthapp.core.data.supplement.appliedTo

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

    /**
     * Over a blank row, because this flow only ever adds — `appliedTo` in `:core:data` is the
     * mapping itself, shared with the name lookup and tested there. What it leaves standing is what
     * the blank row already says: a panel photographed on its own prints no product name, so the
     * name stays empty and typable, and a bottle whose directions state no frequency keeps the
     * app's default of once.
     */
    fun applyReading(read: SupplementLabelReading) {
        reading = read.appliedTo(Supplement(name = ""))
        flow = SupplementFlow.Confirmation
    }

    fun recapture() {
        reading = null
        flow = SupplementFlow.Capture
    }
}
