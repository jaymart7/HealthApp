package ph.mart.healthapp.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import ph.mart.healthapp.core.designsystem.component.BottomNavBar
import ph.mart.healthapp.core.designsystem.component.BottomNavItem
import ph.mart.healthapp.core.designsystem.component.DockedFab
import ph.mart.healthapp.core.designsystem.icon.AppIcons
import ph.mart.healthapp.core.designsystem.icon.DualStateIcon
import ph.mart.healthapp.core.navigation.route.TopLevelBackStack
import ph.mart.healthapp.core.navigation.route.TopLevelDestination
import ph.mart.healthapp.feature.food.ui.FoodCaptureRoute
import ph.mart.healthapp.feature.food.ui.foodEntries
import ph.mart.healthapp.feature.home.ui.homeEntries
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

/** The FAB's overlay sheet — Log weight/Add photo are real [ph.mart.healthapp.core.designsystem.component.AppBottomSheet]s
 * shown here (same shape as [QuickActionSheet] itself), not [androidx.navigation3.runtime.NavKey]
 * routes: predictive back needs to close the sheet without replacing the screen underneath it. */
private enum class ActiveSheet { None, QuickAction, LogWeight, AddPhoto }

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
fun AppScaffold(modifier: Modifier = Modifier) {
    val topLevelBackStack = remember { TopLevelBackStack<NavKey>(TopLevelDestination.Home.route) }
    var activeSheet by rememberSaveable { mutableStateOf(ActiveSheet.None) }
    // The photo-capture flow shows neither nav bar nor FAB (appScaffold.js).
    val showChrome = topLevelBackStack.backStack.lastOrNull() != FoodCaptureRoute

    Box(modifier = modifier.fillMaxSize()) {
        Scaffold(
            bottomBar = {
                if (showChrome) {
                    BottomNavBar(
                        items = TopLevelDestination.entries.map { BottomNavItem(it.icon(), it.label) },
                        selectedIndex = TopLevelDestination.entries.indexOfFirst { it.route == topLevelBackStack.topLevelKey },
                        onSelect = { index -> topLevelBackStack.addTopLevel(TopLevelDestination.entries[index].route) },
                    )
                }
            },
            floatingActionButton = {
                if (showChrome) {
                    DockedFab(
                        onClick = { activeSheet = ActiveSheet.QuickAction },
                    )
                }
            },
        ) { innerPadding ->
            NavDisplay(
                backStack = topLevelBackStack.backStack,
                onBack = { topLevelBackStack.removeLast() },
                entryProvider = entryProvider {
                    homeEntries(onAddPhoto = { activeSheet = ActiveSheet.AddPhoto })
                    foodEntries(onExitCapture = { topLevelBackStack.removeLast() })
                    progressEntries()
                    profileEntries()
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
                onLogWeight = { activeSheet = ActiveSheet.LogWeight },
                onAddPhoto = { activeSheet = ActiveSheet.AddPhoto },
            )
            ActiveSheet.LogWeight -> LogWeightSheet(onDismiss = { activeSheet = ActiveSheet.None })
            ActiveSheet.AddPhoto -> AddPhotoSheet(onDismiss = { activeSheet = ActiveSheet.None })
            ActiveSheet.None -> Unit
        }
    }
}
