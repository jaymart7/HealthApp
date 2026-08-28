package ph.mart.healthapp.feature.food.ui

import androidx.compose.foundation.ScrollState
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import ph.mart.healthapp.core.navigation.route.FoodRoute

/** The FAB's "Log food" destination — the real 6(+1)-state photo-logging flow (Phase 5). */
@Serializable
data object FoodCaptureRoute : NavKey

fun EntryProviderScope<NavKey>.foodEntries(scrollState: ScrollState, onExitCapture: () -> Unit) {
    entry<FoodRoute> { FoodScreen(scrollState = scrollState) }
    entry<FoodCaptureRoute> { PhotoCaptureScreen(onExit = onExitCapture) }
}
