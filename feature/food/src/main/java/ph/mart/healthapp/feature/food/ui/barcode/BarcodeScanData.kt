package ph.mart.healthapp.feature.food.ui.barcode

import ph.mart.healthapp.core.data.food.BarcodeLookupResult
import ph.mart.healthapp.core.data.food.FoodEntry

sealed interface BarcodeScanEvent {
    data class OnBarcodeScanned(val barcode: String) : BarcodeScanEvent
    data object OnCancelLookup : BarcodeScanEvent
    data class OnLogEntry(val entry: FoodEntry) : BarcodeScanEvent
}

sealed interface BarcodeScanSideEffect {
    data class LookupFinished(val result: BarcodeLookupResult) : BarcodeScanSideEffect
    data object EntryLogged : BarcodeScanSideEffect
}
