package ph.mart.healthapp.feature.food.ui.label

import android.graphics.Bitmap
import ph.mart.healthapp.core.data.food.FoodEntry
import ph.mart.healthapp.core.data.food.FoodSuggestion
import ph.mart.healthapp.core.data.food.LabelScanResult

sealed interface LabelScanEvent {
    data class OnPhotoCaptured(val photo: Bitmap) : LabelScanEvent
    data object OnCancelRead : LabelScanEvent

    /** [keepAsFood] is the switch's answer, carried on the same event as the entry rather than sent
     * separately: logging and keeping are one tap, and a second event would let the two land in
     * either order. Null when the switch was off or there was nothing worth keeping. */
    data class OnLogEntry(val entry: FoodEntry, val keepAsFood: FoodSuggestion?) : LabelScanEvent
}

sealed interface LabelScanSideEffect {
    data class ReadFinished(val result: LabelScanResult) : LabelScanSideEffect
    data object EntryLogged : LabelScanSideEffect
}
