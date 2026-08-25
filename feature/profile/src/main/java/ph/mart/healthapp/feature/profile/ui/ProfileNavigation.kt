package ph.mart.healthapp.feature.profile.ui

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import ph.mart.healthapp.core.navigation.route.ProfileRoute

fun EntryProviderScope<NavKey>.profileEntries() {
    entry<ProfileRoute> { ProfileScreen() }
}
