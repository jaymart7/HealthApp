package ph.mart.healthapp.feature.profile.ui.scan

import android.graphics.Bitmap
import ph.mart.healthapp.core.data.supplement.Supplement
import ph.mart.healthapp.core.data.supplement.SupplementScanResult

sealed interface SupplementScanEvent {
    data class OnPhotoCaptured(val photo: Bitmap) : SupplementScanEvent
    data object OnCancelRead : SupplementScanEvent

    /** The whole supplement, not the fields it was read from: the sheet is free to have been
     * edited, and what is saved is what is on screen. */
    data class OnSave(val supplement: Supplement) : SupplementScanEvent
}

sealed interface SupplementScanSideEffect {
    data class ReadFinished(val result: SupplementScanResult) : SupplementScanSideEffect
    data object Saved : SupplementScanSideEffect
}
