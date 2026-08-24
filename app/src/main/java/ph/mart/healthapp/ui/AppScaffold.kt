package ph.mart.healthapp.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
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
import ph.mart.healthapp.feature.food.ui.FoodCaptureStubRoute
import ph.mart.healthapp.feature.food.ui.foodEntries
import ph.mart.healthapp.feature.home.ui.homeEntries
import ph.mart.healthapp.feature.profile.ui.profileEntries
import ph.mart.healthapp.feature.progress.ui.AddPhotoStubRoute
import ph.mart.healthapp.feature.progress.ui.LogWeightStubRoute
import ph.mart.healthapp.feature.progress.ui.progressEntries

private fun TopLevelDestination.icon(): DualStateIcon = when (this) {
    TopLevelDestination.Home -> AppIcons.Home
    TopLevelDestination.Food -> AppIcons.Food
    TopLevelDestination.Progress -> AppIcons.Progress
    TopLevelDestination.Profile -> AppIcons.Profile
}

/**
 * Bottom nav (4 tabs) + docked FAB + quick-action sheet. This is the only place in the app that
 * depends on every `:feature:*` module and `:core:navigation` at once, so it's the only place
 * real navigation wiring can live — see the Phase 2 plan's "flagged architectural decision."
 */
@Composable
fun AppScaffold(modifier: Modifier = Modifier) {
    val topLevelBackStack = remember { TopLevelBackStack<NavKey>(TopLevelDestination.Home.route) }
    var sheetOpen by rememberSaveable { mutableStateOf(false) }

    Surface(color = MaterialTheme.colorScheme.background, modifier = modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                NavDisplay(
                    backStack = topLevelBackStack.backStack,
                    onBack = { topLevelBackStack.removeLast() },
                    entryProvider = entryProvider {
                        homeEntries()
                        foodEntries(onCloseCaptureStub = { topLevelBackStack.removeLast() })
                        progressEntries(onCloseStub = { topLevelBackStack.removeLast() })
                        profileEntries()
                    },
                    modifier = Modifier.weight(1f),
                )
                BottomNavBar(
                    items = TopLevelDestination.entries.map { BottomNavItem(it.icon(), it.label) },
                    selectedIndex = TopLevelDestination.entries.indexOfFirst { it.route == topLevelBackStack.topLevelKey },
                    onSelect = { index -> topLevelBackStack.addTopLevel(TopLevelDestination.entries[index].route) },
                )
            }

            DockedFab(
                onClick = { sheetOpen = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .padding(end = 16.dp, bottom = 48.dp),
            )

            if (sheetOpen) {
                QuickActionSheet(
                    onDismiss = { sheetOpen = false },
                    onLogFood = {
                        sheetOpen = false
                        topLevelBackStack.add(FoodCaptureStubRoute)
                    },
                    onLogWeight = {
                        sheetOpen = false
                        topLevelBackStack.add(LogWeightStubRoute)
                    },
                    onAddPhoto = {
                        sheetOpen = false
                        topLevelBackStack.add(AddPhotoStubRoute)
                    },
                )
            }
        }
    }
}
