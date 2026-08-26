package ph.mart.healthapp.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
 */
@Composable
fun AppScaffold(modifier: Modifier = Modifier) {
    val topLevelBackStack = remember { TopLevelBackStack<NavKey>(TopLevelDestination.Home.route) }
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

    Surface(color = MaterialTheme.colorScheme.background, modifier = modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                NavDisplay(
                    backStack = topLevelBackStack.backStack,
                    onBack = { topLevelBackStack.removeLast() },
                    entryProvider = entryProvider {
                        homeEntries(scrollState = homeScroll, onAddPhoto = { activeSheet = ActiveSheet.AddPhoto })
                        foodEntries(scrollState = foodScroll, onExitCapture = { topLevelBackStack.removeLast() })
                        progressEntries(scrollState = progressScroll)
                        profileEntries(scrollState = profileScroll)
                    },
                    modifier = Modifier.weight(1f),
                )
                BottomNavBar(
                    items = TopLevelDestination.entries.map { BottomNavItem(it.icon(), it.label) },
                    selectedIndex = TopLevelDestination.entries.indexOfFirst { it.route == topLevelBackStack.topLevelKey },
                    onSelect = { index ->
                        val destination = TopLevelDestination.entries[index]
                        if (destination.route != topLevelBackStack.topLevelKey) {
                            topLevelBackStack.addTopLevel(destination.route)
                        } else if (topLevelBackStack.backStack.last() == destination.route) {
                            // Re-tapping the active tab scrolls it to the top — but only when its
                            // root is what's actually showing. FoodCaptureRoute can sit on top of
                            // the Food tab, and scrolling a hidden screen would be a no-op at best.
                            scope.launch { currentScroll.animateScrollTo(0) }
                        }
                    },
                )
            }

            DockedFab(
                onClick = { activeSheet = ActiveSheet.QuickAction },
                expanded = rememberFabExpanded(currentScroll),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .padding(end = 16.dp, bottom = 48.dp),
            )

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
}
