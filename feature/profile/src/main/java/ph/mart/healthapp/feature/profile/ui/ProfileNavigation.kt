package ph.mart.healthapp.feature.profile.ui

import androidx.compose.foundation.ScrollState
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import ph.mart.healthapp.core.designsystem.component.FullScreenState
import ph.mart.healthapp.core.designsystem.component.MascotAvatar
import ph.mart.healthapp.core.designsystem.component.MascotState
import ph.mart.healthapp.core.navigation.route.ProfileRoute
import ph.mart.healthapp.feature.profile.R
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

/** The food library — the user's own foods, saved meals and recipes — also one level above Profile. A route rather than a sheet: it is a
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

/**
 * What the detail pane shows before a row has been tapped. Only ever drawn on a window wide enough
 * to hold two panes — on a phone the detail simply isn't there yet, which is a state with nothing to
 * render. Deliberately not a call to action: every one of these five is one tap away in the list
 * beside it, and naming one would make it the recommended one.
 */
@Composable
private fun ProfileDetailPlaceholder() {
    FullScreenState(
        icon = { MascotAvatar(state = MascotState.Idle, size = 64.dp) },
        heading = stringResource(R.string.profile_pick_heading),
        body = stringResource(R.string.profile_pick_body),
    )
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
fun EntryProviderScope<NavKey>.profileEntries(
    scrollState: ScrollState,
    onOpenHealth: () -> Unit,
    onOpenLibrary: () -> Unit,
    onOpenRoutines: () -> Unit,
    onOpenSupplements: () -> Unit,
    onOpenHomeLayout: () -> Unit,
    onExitFlow: () -> Unit,
) {
    // Profile is the list and its five sub-routes are the detail, so at expanded width they draw
    // beside it rather than over it. `AppScaffold` owns the width rule and only hands `NavDisplay`
    // the strategy once there is room; the metadata is inert at every narrower width.
    entry<ProfileRoute>(
        metadata = ListDetailSceneStrategy.listPane(detailPlaceholder = { ProfileDetailPlaceholder() }),
    ) {
        ProfileScreen(
            scrollState = scrollState,
            onOpenHealth = onOpenHealth,
            onOpenLibrary = onOpenLibrary,
            onOpenRoutines = onOpenRoutines,
            onOpenSupplements = onOpenSupplements,
            onOpenHomeLayout = onOpenHomeLayout,
        )
    }
    val detail = ListDetailSceneStrategy.detailPane()
    entry<HealthConnectionRoute>(metadata = detail) { HealthConnectionScreen(onBack = onExitFlow) }
    entry<FoodLibraryRoute>(metadata = detail) { FoodLibraryScreen() }
    entry<RoutinesRoute>(metadata = detail) { RoutinesScreen() }
    entry<SupplementsRoute>(metadata = detail) { SupplementsScreen() }
    entry<HomeLayoutRoute>(metadata = detail) { HomeLayoutScreen() }
}
