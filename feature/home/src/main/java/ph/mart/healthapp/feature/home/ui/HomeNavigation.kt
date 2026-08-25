package ph.mart.healthapp.feature.home.ui

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import ph.mart.healthapp.core.navigation.route.HomeRoute

/** [onAddPhoto] is how the photo-reminder CTA reaches the Add photo sheet without this module
 * importing `:feature:progress` — AppScaffold already hosts that sheet, so it wires the callback
 * (same shape as `foodEntries(onExitCapture = ...)`). */
fun EntryProviderScope<NavKey>.homeEntries(onAddPhoto: () -> Unit) {
    entry<HomeRoute> { HomeScreen(onAddPhoto = onAddPhoto) }
}
