package ph.mart.healthapp.ui

import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.navigation.BackNavigationBehavior
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import androidx.window.core.layout.WindowSizeClass
import kotlinx.coroutines.launch
import ph.mart.healthapp.ShortcutAction
import ph.mart.healthapp.core.designsystem.component.AppTopBar
import ph.mart.healthapp.core.designsystem.component.BottomNavBar
import ph.mart.healthapp.core.designsystem.component.BottomNavItem
import ph.mart.healthapp.core.designsystem.component.DockedFab
import ph.mart.healthapp.core.designsystem.component.NavRail
import ph.mart.healthapp.core.designsystem.component.rememberFabExpanded
import ph.mart.healthapp.core.designsystem.icon.AppIcons
import ph.mart.healthapp.core.designsystem.icon.DualStateIcon
import ph.mart.healthapp.core.navigation.route.ProfileRoute
import ph.mart.healthapp.core.navigation.route.TopLevelBackStack
import ph.mart.healthapp.core.navigation.route.TopLevelDestination
import ph.mart.healthapp.feature.coach.ui.CoachRoute
import ph.mart.healthapp.feature.coach.ui.coachEntries
import ph.mart.healthapp.feature.food.ui.BarcodeScanRoute
import ph.mart.healthapp.feature.food.ui.FoodCaptureRoute
import ph.mart.healthapp.feature.food.ui.RecipeBuilderRoute
import ph.mart.healthapp.feature.food.ui.StrengthWorkoutRoute
import ph.mart.healthapp.feature.food.ui.VoiceLogRoute
import ph.mart.healthapp.feature.food.ui.exercise.LogExerciseSheet
import ph.mart.healthapp.feature.food.ui.foodEntries
import ph.mart.healthapp.feature.home.ui.homeEntries
import ph.mart.healthapp.feature.profile.ui.FoodLibraryRoute
import ph.mart.healthapp.feature.profile.ui.HomeLayoutRoute
import ph.mart.healthapp.feature.profile.ui.RoutinesRoute
import ph.mart.healthapp.feature.profile.ui.SupplementsRoute
import ph.mart.healthapp.feature.profile.ui.HealthConnectionRoute
import ph.mart.healthapp.feature.profile.ui.profileEntries
import ph.mart.healthapp.feature.progress.ui.photo.AddPhotoSheet
import ph.mart.healthapp.feature.progress.ui.progressEntries
import ph.mart.healthapp.feature.progress.ui.weight.LogWeightSheet

/** What the toolbar says on each route a level above a tab. It lives here rather than on the route
 * types because `:core:navigation` is a leaf module and this is already the one place that sees
 * every feature's routes at once. */
private fun NavKey?.title(): String = when (this) {
    CoachRoute -> "Coach"
    RecipeBuilderRoute -> "New recipe"
    is StrengthWorkoutRoute -> if (this.editingId > 0) "Edit workout" else "Strength workout"
    is VoiceLogRoute -> "Say what you ate"
    HealthConnectionRoute -> "Google Health"
    FoodLibraryRoute -> "Saved meals & recipes"
    RoutinesRoute -> "Workout routines"
    SupplementsRoute -> "Supplements"
    HomeLayoutRoute -> "Home layout"
    else -> ""
}

/**
 * The five routes that draw *beside* Profile once the window is wide enough, rather than over it.
 * One list, read by both the pane metadata in `profileEntries` and by [showsTabChrome], so the
 * scene and the chrome can never disagree about which routes are panes.
 */
internal val ProfileDetailRoutes: Set<NavKey> = setOf(
    HealthConnectionRoute,
    FoodLibraryRoute,
    RoutinesRoute,
    SupplementsRoute,
    HomeLayoutRoute,
)

/**
 * Whether the window wears the tab chrome — the rail or the bar, and the FAB.
 *
 * A tab always does. So does a Profile detail at two-pane width, because its tab root is still on
 * screen beside it: the back stack moved, the screen underneath did not, and taking the navigation
 * away from a window that has room for it would be the wrong answer to more space. [beneath] is what
 * keeps that honest — the same routes reached from another tab (Health Connect's rationale intent
 * lands on whichever tab is showing) have no Profile beside them and stay single-pane.
 */
internal fun showsTabChrome(current: NavKey?, beneath: NavKey?, twoPane: Boolean): Boolean =
    TopLevelDestination.entries.any { it.route == current } ||
        (twoPane && current in ProfileDetailRoutes && beneath == ProfileRoute)

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
 * Tab navigation (4 tabs) + docked FAB + quick-action sheet. This is the only place in the app that
 * depends on every `:feature:*` module and `:core:navigation` at once, so it's the only place
 * real navigation wiring can live — see the Phase 2 plan's "flagged architectural decision."
 *
 * It is also **the only place in the app that reads the window's width**. Two breakpoints, and they
 * answer different questions: at medium the bottom bar becomes a [NavRail], because a window that is
 * wide is usually also short and a bar spends the height it hasn't got; at expanded the Progress tab
 * and the Profile tab draw two panes. Everything downstream is handed a plain `Boolean`, so there is
 * one definition of "wide" by construction and no feature module needs the adaptive artifact.
 *
 * [Scaffold] owns the window insets: it measures [BottomNavBar] (which consumes the navigation-bar
 * inset itself) and hands the destinations a `PaddingValues` that already clears the status bar,
 * any landscape cutout, and the nav bar. The sheets sit *outside* the Scaffold because it draws
 * the bottom bar and FAB after its content — a sheet nested inside would have both on top of its
 * scrim.
 */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun AppScaffold(
    modifier: Modifier = Modifier,
    tabRequest: TopLevelDestination? = null,
    onTabRequestHandled: () -> Unit = {},
    shortcutRequest: ShortcutAction? = null,
    onShortcutRequestHandled: () -> Unit = {},
) {
    // The list comes from Nav3's own saveable holder, so the whole navigator survives an Activity
    // recreation — rotation, a font-scale or locale change, process death. Its Android overload
    // serializes the `@Serializable` route types by reflection, so a new route needs no
    // registration here. On a restore it ignores the seed and returns what was saved, which is
    // what the user was actually looking at.
    val saved = rememberNavBackStack((tabRequest ?: TopLevelDestination.Home).route)
    val topLevelBackStack = remember(saved) { TopLevelBackStack(saved) }

    // [tabRequest] is how a tapped reminder lands where the user would act on it — see
    // MainActivity. On a cold start it seeded the stack above and this switch is a no-op; the
    // case it exists for is a notification arriving while the app is already up. Clearing it is
    // what lets a second reminder for the same tab land after the user has navigated away.
    LaunchedEffect(tabRequest) {
        tabRequest?.let {
            topLevelBackStack.addTopLevel(it.route)
            onTabRequestHandled()
        }
    }
    var activeSheet by rememberSaveable { mutableStateOf(ActiveSheet.None) }

    val windowSizeClass = currentWindowAdaptiveInfo().windowSizeClass
    val rail = windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND)
    val twoPane = windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND)

    // `shouldHandleSinglePaneLayout = false` plus the gate below is what makes a phone render through
    // exactly the path it rendered through before this existed. Gating on `twoPane` rather than
    // handing the strategy its own PaneScaffoldDirective is also what keeps the two breakpoints above
    // the app's only width rule — the strategy's default directive would split at medium, where a
    // rail plus two panes leaves each one narrower than a Profile stepper row.
    //
    // PopLatest, not the default PopUntilScaffoldValueChange: closing a Profile detail leaves the
    // list beside its placeholder, which is *still* a two-pane value, so the default keeps popping
    // and back walks out of the tab entirely. One press is one entry here, as it is everywhere else
    // — `NavDisplay`'s onBack is TopLevelBackStack.removeLast(), which pops exactly one.
    val listDetail = rememberListDetailSceneStrategy<NavKey>(
        shouldHandleSinglePaneLayout = false,
        backNavigationBehavior = BackNavigationBehavior.PopLatest,
    )

    // The recap notification's request, held here rather than consumed inside the effect below:
    // the Progress tab may not be composed yet when the intent lands, so this has to survive until
    // the screen that owns the overlay can act on it. Cleared on consumption like [tabRequest],
    // which is what lets a second Sunday re-open a recap the user has since closed.
    var openRecapRequest by rememberSaveable { mutableStateOf(false) }

    // A launcher shortcut is the FAB's sheet with the tap already made, so every branch here is a
    // line QuickActionSheet's own wiring already runs — day 0 included, for the reason the FAB
    // passes it. Cleared on consumption like [tabRequest], which is what lets the same shortcut
    // land twice.
    LaunchedEffect(shortcutRequest) {
        when (shortcutRequest) {
            ShortcutAction.SpeakFood -> topLevelBackStack.add(VoiceLogRoute(0))
            ShortcutAction.LogFood -> topLevelBackStack.add(FoodCaptureRoute(0))
            ShortcutAction.LogWeight -> activeSheet = ActiveSheet.LogWeight
            // Not a shortcut: Health Connect's rationale tap, which has to land on the screen that
            // explains what FitPulse reads. That is the same route Profile's own row opens.
            ShortcutAction.HealthSync -> topLevelBackStack.add(HealthConnectionRoute)
            // Also not a shortcut: the weekly recap notification. The tab switch is already free —
            // the same intent carries EXTRA_TAB — so all this does is ask the screen to open its
            // overlay once it is there.
            ShortcutAction.OpenRecap -> openRecapRequest = true
            // A write, not a destination — MainActivity handles it.
            ShortcutAction.AddWater, null -> Unit
        }
        shortcutRequest?.let { onShortcutRequestHandled() }
    }
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

    // A tab wears the nav bar or rail and the FAB; anything a level above wears a toolbar with back
    // instead. The two used to be one boolean and now diverge: a Profile detail drawn as a pane keeps
    // the tab chrome (its tab is still on screen beside it) *and* keeps the toolbar, because back is
    // the only way to dismiss the pane again.
    val current = topLevelBackStack.backStack.lastOrNull()
    val beneath = topLevelBackStack.backStack.getOrNull(topLevelBackStack.backStack.lastIndex - 1)
    val showTabChrome = showsTabChrome(current = current, beneath = beneath, twoPane = twoPane)
    val isTopLevel = TopLevelDestination.entries.any { it.route == current }

    // The camera flows are the one exemption: full-bleed surfaces that draw under both system bars
    // (appScaffold.js) and dispatch back per capture state, so a generic toolbar would break both.
    val fullBleed = current is FoodCaptureRoute || current is BarcodeScanRoute

    // Tapping the arrow has to run the same handler chain system back runs — the recipe builder
    // asks before discarding, and popping the stack here would walk straight past that question.
    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher

    // One handler for both presentations of the same four tabs: switch, or scroll the active one
    // back to the top. It is passed to whichever of the bar and the rail is drawn.
    val onSelectTab: (Int) -> Unit = { index ->
        val destination = TopLevelDestination.entries[index]
        if (destination.route != topLevelBackStack.topLevelKey) {
            topLevelBackStack.addTopLevel(destination.route)
        } else if (topLevelBackStack.backStack.last() == destination.route) {
            // Re-tapping the active tab scrolls it to the top — but only when its root is what's
            // actually showing; scrolling a hidden screen would be a no-op at best.
            scope.launch { currentScroll.animateScrollTo(0) }
        }
    }
    val tabItems = TopLevelDestination.entries.map { BottomNavItem(it.icon(), it.label) }
    val selectedTab = TopLevelDestination.entries.indexOfFirst { it.route == topLevelBackStack.topLevelKey }

    Box(modifier = modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxSize()) {
            if (rail && showTabChrome) {
                NavRail(
                    items = tabItems,
                    selectedIndex = selectedTab,
                    onSelect = onSelectTab,
                    // Collapsed, always: an extended FAB does not fit an 80dp rail, and
                    // rememberFabExpanded is a docked-bar affordance with nothing to say here.
                    fab = { DockedFab(onClick = { activeSheet = ActiveSheet.QuickAction }, expanded = false) },
                )
            }
            // ponytail: with a rail drawn, the Scaffold still applies the window's start inset the
            // rail is already sitting in, so content clears a landscape cutout twice. A few dp of
            // slack, nothing obscured; consumeWindowInsets around the rail is the fix if it shows.
            Scaffold(
                contentWindowInsets = if (fullBleed) WindowInsets(0) else ScaffoldDefaults.contentWindowInsets,
                topBar = {
                    if (!isTopLevel && !fullBleed) {
                        AppTopBar(
                            title = current.title(),
                            onBack = { backDispatcher?.onBackPressed() },
                        )
                    }
                },
                bottomBar = {
                    if (showTabChrome && !rail) {
                        BottomNavBar(items = tabItems, selectedIndex = selectedTab, onSelect = onSelectTab)
                    }
                },
                floatingActionButton = {
                    if (showTabChrome && !rail) {
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
                    sceneStrategies = if (twoPane) listOf(listDetail) else emptyList(),
                    entryProvider = entryProvider {
                        homeEntries(
                            scrollState = homeScroll,
                            onAddPhoto = { activeSheet = ActiveSheet.AddPhoto },
                            onOpenCoach = { topLevelBackStack.add(CoachRoute) },
                            // Day 0 is today, the convention the FAB's own sheet uses — the plan card
                            // only ever starts today's workout.
                            onStartRoutine = { routineId ->
                                topLevelBackStack.add(StrengthWorkoutRoute(0, 0, routineId))
                            },
                        )
                        coachEntries()
                        foodEntries(
                            scrollState = foodScroll,
                            onScanBarcode = { date -> topLevelBackStack.add(BarcodeScanRoute(date)) },
                            onSpeakFood = { date -> topLevelBackStack.add(VoiceLogRoute(date)) },
                            onCapturePhoto = { date -> topLevelBackStack.add(FoodCaptureRoute(date)) },
                            onNewRecipe = { topLevelBackStack.add(RecipeBuilderRoute) },
                            onOpenStrength = { date, editingId ->
                                topLevelBackStack.add(StrengthWorkoutRoute(date, editingId))
                            },
                            onExitFlow = { topLevelBackStack.removeLast() },
                        )
                        progressEntries(
                            scrollState = progressScroll,
                            twoPane = twoPane,
                            openRecap = openRecapRequest,
                            onOpenRecapHandled = { openRecapRequest = false },
                        )
                        profileEntries(
                            scrollState = profileScroll,
                            onOpenHealth = { topLevelBackStack.add(HealthConnectionRoute) },
                            onOpenLibrary = { topLevelBackStack.add(FoodLibraryRoute) },
                            onOpenRoutines = { topLevelBackStack.add(RoutinesRoute) },
                            onOpenSupplements = { topLevelBackStack.add(SupplementsRoute) },
                            onOpenHomeLayout = { topLevelBackStack.add(HomeLayoutRoute) },
                            onExitFlow = { topLevelBackStack.removeLast() },
                        )
                    },
                    // Only the bar is cleared here — clearance for the FAB on top of it is
                    // [DockedFabContentPadding], added inside each destination's scroll container.
                    modifier = Modifier.padding(innerPadding),
                )
            }
        }

        when (activeSheet) {
            ActiveSheet.QuickAction -> QuickActionSheet(
                onDismiss = { activeSheet = ActiveSheet.None },
                // Day 0 is today — the FAB carries no diary date, the convention
                // StrengthWorkoutRoute already uses from here.
                onSpeakFood = {
                    activeSheet = ActiveSheet.None
                    topLevelBackStack.add(VoiceLogRoute(0))
                },
                onLogFood = {
                    activeSheet = ActiveSheet.None
                    topLevelBackStack.add(FoodCaptureRoute(0))
                },
                onLogExercise = { activeSheet = ActiveSheet.LogExercise },
                onLogWeight = { activeSheet = ActiveSheet.LogWeight },
                onAddPhoto = { activeSheet = ActiveSheet.AddPhoto },
            )
            ActiveSheet.LogExercise -> LogExerciseSheet(
                onDismiss = { activeSheet = ActiveSheet.None },
                // The FAB's sheet carries no day, so the workout screen it opens gets 0 too —
                // which the repository stamps as today, exactly as the sheet's own save would.
                onOpenStrength = { date ->
                    activeSheet = ActiveSheet.None
                    topLevelBackStack.add(StrengthWorkoutRoute(date, 0))
                },
            )
            ActiveSheet.LogWeight -> LogWeightSheet(onDismiss = { activeSheet = ActiveSheet.None })
            ActiveSheet.AddPhoto -> AddPhotoSheet(onDismiss = { activeSheet = ActiveSheet.None })
            ActiveSheet.None -> Unit
        }
    }
}
