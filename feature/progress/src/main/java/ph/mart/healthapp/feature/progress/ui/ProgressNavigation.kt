package ph.mart.healthapp.feature.progress.ui

import androidx.compose.foundation.ScrollState
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import ph.mart.healthapp.core.navigation.route.ProgressRoute
import ph.mart.healthapp.feature.progress.ui.progress.ProgressScreen

fun EntryProviderScope<NavKey>.progressEntries(scrollState: ScrollState) {
    entry<ProgressRoute> { ProgressScreen(scrollState = scrollState) }
}
