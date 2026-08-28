package ph.mart.healthapp.feature.food.ui

import ph.mart.healthapp.core.data.food.BarcodeLookupResult
import ph.mart.healthapp.core.data.food.FoodEntry
import ph.mart.healthapp.core.data.food.MealType
import ph.mart.healthapp.core.data.food.ScannedProduct

sealed interface BarcodeScanEvent {
    data class OnBarcodeScanned(val barcode: String) : BarcodeScanEvent
    data object OnCancelLookup : BarcodeScanEvent
    data class OnLogEntry(val entry: FoodEntry) : BarcodeScanEvent
}

sealed interface BarcodeScanSideEffect {
    data class LookupFinished(val result: BarcodeLookupResult) : BarcodeScanSideEffect
    data object EntryLogged : BarcodeScanSideEffect
}

/**
 * Twin of [RecognizedFood.toAddEntryForm][ph.mart.healthapp.core.data.food.RecognizedFood].
 *
 * ponytail: the macros stay as looked up (per 100 g) when the user edits the portion — manual
 * entry doesn't rescale either. Scale here if that's ever reported as wrong.
 */
fun ScannedProduct.toAddEntryForm(mealType: MealType): AddEntryForm = AddEntryForm(
    mealType = mealType,
    name = name,
    portionAmount = portionAmount,
    portionUnit = portionUnit,
    calories = calories,
    proteinG = proteinG,
    carbsG = carbsG,
    fatG = fatG,
)
