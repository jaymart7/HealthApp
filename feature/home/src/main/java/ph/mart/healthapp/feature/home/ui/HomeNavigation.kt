package ph.mart.healthapp.feature.home.ui

import androidx.compose.foundation.ScrollState
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import ph.mart.healthapp.core.navigation.route.HomeRoute

/** [onAddPhoto] is how the photo-reminder CTA reaches the Add photo sheet without this module
 * importing `:feature:progress` — AppScaffold already hosts that sheet, so it wires the callback
 * (same shape as `foodEntries(onExitCapture = ...)`).
 *
 * [scrollState] is hoisted for the same reason: the FAB's scroll-collapse and tap-active-tab-to-
 * scroll-to-top both live in AppScaffold, which can't see a ScrollState created inside the screen. */
fun EntryProviderScope<NavKey>.homeEntries(scrollState: ScrollState, onAddPhoto: () -> Unit) {
    entry<HomeRoute> { HomeScreen(onAddPhoto = onAddPhoto, scrollState = scrollState) }
}
