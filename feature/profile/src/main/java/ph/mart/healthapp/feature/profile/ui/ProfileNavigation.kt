package ph.mart.healthapp.feature.profile.ui

import androidx.compose.foundation.ScrollState
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import ph.mart.healthapp.core.navigation.route.ProfileRoute
import ph.mart.healthapp.feature.profile.ui.health.HealthConnectionScreen
import ph.mart.healthapp.feature.profile.ui.layout.HomeLayoutScreen
import ph.mart.healthapp.feature.profile.ui.library.FoodLibraryScreen
import ph.mart.healthapp.feature.profile.ui.profile.ProfileScreen
import ph.mart.healthapp.feature.profile.ui.routine.RoutinesScreen
import ph.mart.healthapp.feature.profile.ui.supplement.SupplementsScreen

/**
 * The Google Health disclosure and connection, one level above the Profile tab. A route rather
 * than a sheet: the in-app disclosure has to be a screen the user lands on in the normal flow,
 * and NavDisplay's own back is then what returns them to Profile.
 */
@Serializable
data object HealthConnectionRoute : NavKey

/** Saved meals and recipes, also one level above Profile. A route rather than a sheet: it is a
 * list that can outgrow a sheet, and NavDisplay's back is what returns to Profile. */
@Serializable
data object FoodLibraryRoute : NavKey

/** The workout routines, one level above Profile for the reasons [FoodLibraryRoute] is — it is
 * that screen's twin, one domain over. */
@Serializable
data object RoutinesRoute : NavKey

/** The supplement list, one level above Profile for the same reasons [FoodLibraryRoute] is: it can
 * outgrow a sheet, and NavDisplay's back is what returns to Profile. */
@Serializable
data object SupplementsRoute : NavKey

/** The Home card order and visibility, one level above Profile for the reasons [SupplementsRoute]
 * is: a thirteen-row list that outgrows a sheet, and NavDisplay's back returns to Profile. */
@Serializable
data object HomeLayoutRoute : NavKey

fun EntryProviderScope<NavKey>.profileEntries(
    scrollState: ScrollState,
    onOpenHealth: () -> Unit,
    onOpenLibrary: () -> Unit,
    onOpenRoutines: () -> Unit,
    onOpenSupplements: () -> Unit,
    onOpenHomeLayout: () -> Unit,
    onExitFlow: () -> Unit,
) {
    entry<ProfileRoute> {
        ProfileScreen(
            scrollState = scrollState,
            onOpenHealth = onOpenHealth,
            onOpenLibrary = onOpenLibrary,
            onOpenRoutines = onOpenRoutines,
            onOpenSupplements = onOpenSupplements,
            onOpenHomeLayout = onOpenHomeLayout,
        )
    }
    entry<HealthConnectionRoute> { HealthConnectionScreen(onBack = onExitFlow) }
    entry<FoodLibraryRoute> { FoodLibraryScreen() }
    entry<RoutinesRoute> { RoutinesScreen() }
    entry<SupplementsRoute> { SupplementsScreen() }
    entry<HomeLayoutRoute> { HomeLayoutScreen() }
}
