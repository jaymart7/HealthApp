package ph.mart.healthapp.feature.profile.ui

import androidx.compose.foundation.ScrollState
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import ph.mart.healthapp.core.navigation.route.ProfileRoute

fun EntryProviderScope<NavKey>.profileEntries(scrollState: ScrollState) {
    entry<ProfileRoute> { ProfileScreen(scrollState = scrollState) }
}
