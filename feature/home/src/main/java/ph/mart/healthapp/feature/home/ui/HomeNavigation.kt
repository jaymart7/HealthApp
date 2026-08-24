package ph.mart.healthapp.feature.home.ui

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import ph.mart.healthapp.core.navigation.route.HomeRoute

fun EntryProviderScope<NavKey>.homeEntries() {
    entry<HomeRoute> { HomePlaceholderScreen() }
}
