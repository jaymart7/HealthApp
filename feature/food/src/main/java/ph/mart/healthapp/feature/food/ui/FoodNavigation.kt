package ph.mart.healthapp.feature.food.ui

import androidx.compose.foundation.ScrollState
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import ph.mart.healthapp.core.navigation.route.FoodRoute

/** The FAB's "Log food" destination — the real 6(+1)-state photo-logging flow (Phase 5). */
@Serializable
data object FoodCaptureRoute : NavKey

/** The food diary's barcode entry point — the scan/lookup/confirm flow. */
@Serializable
data object BarcodeScanRoute : NavKey

fun EntryProviderScope<NavKey>.foodEntries(
    scrollState: ScrollState,
    onScanBarcode: () -> Unit,
    onExitFlow: () -> Unit,
) {
    entry<FoodRoute> { FoodScreen(scrollState = scrollState, onScanBarcode = onScanBarcode) }
    entry<FoodCaptureRoute> { PhotoCaptureScreen(onExit = onExitFlow) }
    entry<BarcodeScanRoute> { BarcodeScanScreen(onExit = onExitFlow) }
}
