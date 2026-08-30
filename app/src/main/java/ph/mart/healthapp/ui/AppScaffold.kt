package ph.mart.healthapp.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import kotlinx.coroutines.launch
import ph.mart.healthapp.core.designsystem.component.BottomNavBar
import ph.mart.healthapp.core.designsystem.component.BottomNavItem
import ph.mart.healthapp.core.designsystem.component.DockedFab
import ph.mart.healthapp.core.designsystem.component.rememberFabExpanded
import ph.mart.healthapp.core.designsystem.icon.AppIcons
import ph.mart.healthapp.core.designsystem.icon.DualStateIcon
import ph.mart.healthapp.core.navigation.route.TopLevelBackStack
import ph.mart.healthapp.core.navigation.route.TopLevelDestination
import ph.mart.healthapp.feature.food.ui.BarcodeScanRoute
import ph.mart.healthapp.feature.food.ui.FoodCaptureRoute
import ph.mart.healthapp.feature.food.ui.RecipeBuilderRoute
import ph.mart.healthapp.feature.food.ui.exercise.LogExerciseSheet
import ph.mart.healthapp.feature.food.ui.foodEntries
import ph.mart.healthapp.feature.home.ui.homeEntries
import ph.mart.healthapp.feature.profile.ui.HealthConnectionRoute
import ph.mart.healthapp.feature.profile.ui.profileEntries
import ph.mart.healthapp.feature.progress.ui.AddPhotoSheet
import ph.mart.healthapp.feature.progress.ui.LogWeightSheet
import ph.mart.healthapp.feature.progress.ui.progressEntries

private fun TopLevelDestination.icon(): DualStateIcon = when (this) {
    TopLevelDestination.Home -> AppIcons.Home
    TopLevelDestination.Food -> AppIcons.Food
    TopLevelDestination.Progress -> AppIcons.Progress
    TopLevelDestination.Profile -> AppIcons.Profile
}

/** The FAB's overlay sheet — Log exercise/Log weight/Add photo are real [ph.mart.healthapp.core.designsystem.component.AppBottomSheet]s
 * shown here (same shape as [QuickActionSheet] itself), not [androidx.navigation3.runtime.NavKey]
 * routes: predictive back needs to close the sheet without replacing the screen underneath it. */
private enum class ActiveSheet { None, QuickAction, LogExercise, LogWeight, AddPhoto }

/**
 * Bottom nav (4 tabs) + docked FAB + quick-action sheet. This is the only place in the app that
 * depends on every `:feature:*` module and `:core:navigation` at once, so it's the only place
 * real navigation wiring can live — see the Phase 2 plan's "flagged architectural decision."
 *
 * [Scaffold] owns the window insets: it measures [BottomNavBar] (which consumes the navigation-bar
 * inset itself) and hands the destinations a `PaddingValues` that already clears the status bar,
 * any landscape cutout, and the nav bar. The sheets sit *outside* the Scaffold because it draws
 * the bottom bar and FAB after its content — a sheet nested inside would have both on top of its
 * scrim.
 */
@Composable
fun AppScaffold(
    modifier: Modifier = Modifier,
    startTab: TopLevelDestination = TopLevelDestination.Home,
) {
    // [startTab] is how a tapped reminder notification lands where the user would act on it — see
    // MainActivity. It only seeds the initial stack; nothing re-routes an already-running app.
    val topLevelBackStack = remember { TopLevelBackStack<NavKey>(startTab.route) }
    var activeSheet by rememberSaveable { mutableStateOf(ActiveSheet.None) }
    val scope = rememberCoroutineScope()

    // Hoisted out of the screens so the FAB can watch the active tab's scroll and re-tapping a tab
    // can drive it back to the top. Free side effect: NavDisplay disposes the off-screen entry, so
    // owning the state here is also what preserves each tab's scroll position across tab switches.
    val homeScroll = rememberScrollState()
    val foodScroll = rememberScrollState()
    val progressScroll = rememberScrollState()
    val profileScroll = rememberScrollState()
    val currentScroll = when (topLevelBackStack.topLevelKey) {
        TopLevelDestination.Food.route -> foodScroll
        TopLevelDestination.Progress.route -> progressScroll
        TopLevelDestination.Profile.route -> profileScroll
        else -> homeScroll
    }

    // The camera flows are full-bleed surfaces: neither nav bar nor FAB, and they draw under both
    // system bars (appScaffold.js). Every other route is a tab and stops at the bars.
    val showChrome = topLevelBackStack.backStack.lastOrNull()
        .let { it != FoodCaptureRoute && it !is BarcodeScanRoute }

    Box(modifier = modifier.fillMaxSize()) {
        Scaffold(
            contentWindowInsets = if (showChrome) ScaffoldDefaults.contentWindowInsets else WindowInsets(0),
            bottomBar = {
                if (showChrome) {
                    BottomNavBar(
                        items = TopLevelDestination.entries.map { BottomNavItem(it.icon(), it.label) },
                        selectedIndex = TopLevelDestination.entries.indexOfFirst { it.route == topLevelBackStack.topLevelKey },
                        onSelect = { index ->
                            val destination = TopLevelDestination.entries[index]
                            if (destination.route != topLevelBackStack.topLevelKey) {
                                topLevelBackStack.addTopLevel(destination.route)
                            } else if (topLevelBackStack.backStack.last() == destination.route) {
                                // Re-tapping the active tab scrolls it to the top — but only when
                                // its root is what's actually showing; scrolling a hidden screen
                                // would be a no-op at best.
                                scope.launch { currentScroll.animateScrollTo(0) }
                            }
                        },
                    )
                }
            },
            floatingActionButton = {
                if (showChrome) {
                    DockedFab(
                        onClick = { activeSheet = ActiveSheet.QuickAction },
                        expanded = rememberFabExpanded(currentScroll),
                    )
                }
            },
        ) { innerPadding ->
            NavDisplay(
                backStack = topLevelBackStack.backStack,
                onBack = { topLevelBackStack.removeLast() },
                entryProvider = entryProvider {
                    homeEntries(scrollState = homeScroll, onAddPhoto = { activeSheet = ActiveSheet.AddPhoto })
                    foodEntries(
                        scrollState = foodScroll,
                        onScanBarcode = { date -> topLevelBackStack.add(BarcodeScanRoute(date)) },
                        onNewRecipe = { topLevelBackStack.add(RecipeBuilderRoute) },
                        onExitFlow = { topLevelBackStack.removeLast() },
                    )
                    progressEntries(scrollState = progressScroll)
                    profileEntries(
                        scrollState = profileScroll,
                        onOpenHealth = { topLevelBackStack.add(HealthConnectionRoute) },
                        onExitFlow = { topLevelBackStack.removeLast() },
                    )
                },
                // Only the bar is cleared here — clearance for the FAB on top of it is
                // [DockedFabContentPadding], added inside each destination's scroll container.
                modifier = Modifier.padding(innerPadding),
            )
        }

        when (activeSheet) {
            ActiveSheet.QuickAction -> QuickActionSheet(
                onDismiss = { activeSheet = ActiveSheet.None },
                onLogFood = {
                    activeSheet = ActiveSheet.None
                    topLevelBackStack.add(FoodCaptureRoute)
                },
                onLogExercise = { activeSheet = ActiveSheet.LogExercise },
                onLogWeight = { activeSheet = ActiveSheet.LogWeight },
                onAddPhoto = { activeSheet = ActiveSheet.AddPhoto },
            )
            ActiveSheet.LogExercise -> LogExerciseSheet(onDismiss = { activeSheet = ActiveSheet.None })
            ActiveSheet.LogWeight -> LogWeightSheet(onDismiss = { activeSheet = ActiveSheet.None })
            ActiveSheet.AddPhoto -> AddPhotoSheet(onDismiss = { activeSheet = ActiveSheet.None })
            ActiveSheet.None -> Unit
        }
    }
}
