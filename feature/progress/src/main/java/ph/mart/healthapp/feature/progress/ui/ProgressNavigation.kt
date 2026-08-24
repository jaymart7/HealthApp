package ph.mart.healthapp.feature.progress.ui

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import ph.mart.healthapp.core.navigation.route.ProgressRoute

fun EntryProviderScope<NavKey>.progressEntries() {
    entry<ProgressRoute> { ProgressScreen() }
}
