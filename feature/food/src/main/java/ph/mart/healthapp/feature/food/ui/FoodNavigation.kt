package ph.mart.healthapp.feature.food.ui

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import ph.mart.healthapp.core.navigation.route.FoodRoute

/** Stub target for the FAB's "Log food" action — real capture flow arrives in Phase 5. */
@Serializable
data object FoodCaptureStubRoute : NavKey

fun EntryProviderScope<NavKey>.foodEntries(onCloseCaptureStub: () -> Unit) {
    entry<FoodRoute> { FoodPlaceholderScreen() }
    entry<FoodCaptureStubRoute> { FoodCaptureStubScreen(onClose = onCloseCaptureStub) }
}
