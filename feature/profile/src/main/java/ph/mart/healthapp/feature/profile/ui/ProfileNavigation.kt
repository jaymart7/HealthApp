package ph.mart.healthapp.feature.profile.ui

import androidx.compose.foundation.ScrollState
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import ph.mart.healthapp.core.navigation.route.ProfileRoute
import ph.mart.healthapp.feature.profile.ui.health.HealthConnectionScreen

/**
 * The Google Health disclosure and connection, one level above the Profile tab. A route rather
 * than a sheet: the in-app disclosure has to be a screen the user lands on in the normal flow,
 * and NavDisplay's own back is then what returns them to Profile.
 */
@Serializable
data object HealthConnectionRoute : NavKey

fun EntryProviderScope<NavKey>.profileEntries(
    scrollState: ScrollState,
    onOpenHealth: () -> Unit,
    onExitFlow: () -> Unit,
) {
    entry<ProfileRoute> { ProfileScreen(scrollState = scrollState, onOpenHealth = onOpenHealth) }
    entry<HealthConnectionRoute> { HealthConnectionScreen(onBack = onExitFlow) }
}
