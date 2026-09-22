package ph.mart.healthapp.ui

import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import androidx.window.core.layout.WindowSizeClass
import kotlinx.coroutines.launch
import ph.mart.healthapp.R
import ph.mart.healthapp.ShortcutAction
import ph.mart.healthapp.core.data.exercise.EARNED_MIN_KCAL
import ph.mart.healthapp.core.data.exercise.earnedSavedLine
import ph.mart.healthapp.core.data.food.MealIdea
import ph.mart.healthapp.core.designsystem.component.AppTopBar
import ph.mart.healthapp.core.designsystem.component.BottomNavBar
import ph.mart.healthapp.core.designsystem.component.BottomNavItem
import ph.mart.healthapp.core.designsystem.component.DockedFab
import ph.mart.healthapp.core.designsystem.component.DockedFabContentPadding
import ph.mart.healthapp.core.designsystem.component.HomeCard
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
import ph.mart.healthapp.feature.food.ui.LabelScanRoute
import ph.mart.healthapp.feature.food.ui.MealIdeasRoute
import ph.mart.healthapp.feature.food.ui.FoodHistoryRoute
import ph.mart.healthapp.feature.food.ui.RecipeBuilderRoute
import ph.mart.healthapp.feature.food.ui.VoiceLogRoute
import ph.mart.healthapp.feature.food.ui.foodEntries
import ph.mart.healthapp.feature.home.ui.homeEntries
import ph.mart.healthapp.feature.profile.ui.AboutYouRoute
import ph.mart.healthapp.feature.profile.ui.FoodLibraryRoute
import ph.mart.healthapp.feature.profile.ui.HealthConnectionRoute
import ph.mart.healthapp.feature.profile.ui.HomeLayoutRoute
import ph.mart.healthapp.feature.profile.ui.RemindersRoute
import ph.mart.healthapp.feature.profile.ui.RoutinesRoute
import ph.mart.healthapp.feature.profile.ui.SettingsRoute
import ph.mart.healthapp.feature.profile.ui.SupplementScanRoute
import ph.mart.healthapp.feature.profile.ui.SupplementsRoute
import ph.mart.healthapp.feature.profile.ui.profileEntries
import ph.mart.healthapp.feature.progress.ui.AddPhotoPreviewRoute
import ph.mart.healthapp.feature.progress.ui.AddPhotoRoute
import ph.mart.healthapp.feature.progress.ui.PhotoComparisonRoute
import ph.mart.healthapp.feature.progress.ui.ProgressSubjectRoutes
import ph.mart.healthapp.feature.progress.ui.RecapRoute
import ph.mart.healthapp.feature.progress.ui.TimelapseRoute
import ph.mart.healthapp.core.data.recap.ReportSection
import ph.mart.healthapp.feature.progress.ui.progress.Subject
import ph.mart.healthapp.feature.progress.ui.progressEntries
import ph.mart.healthapp.feature.progress.ui.route
import ph.mart.healthapp.feature.progress.ui.weight.LogWeightSheet
import ph.mart.healthapp.feature.training.ui.LogExerciseSheet
import ph.mart.healthapp.feature.training.ui.StrengthWorkoutRoute
import ph.mart.healthapp.feature.training.ui.trainingEntries

/** What the toolbar says on each route a level above a tab. It lives here rather than on the route
 * types because `:core:navigation` is a leaf module and this is already the one place that sees
 * every feature's routes at once. */
/** The tab's name. It lives here rather than on the enum for the reason its icon does. */
@StringRes
private fun TopLevelDestination.label(): Int = when (this) {
    TopLevelDestination.Home -> R.string.app_tab_home
    TopLevelDestination.Food -> R.string.app_tab_food
    TopLevelDestination.Progress -> R.string.app_tab_progress
    TopLevelDestination.Profile -> R.string.app_tab_profile
}

@Composable
private fun NavKey?.title(): String = when (this) {
    RecipeBuilderRoute -> stringResource(R.string.app_title_new_recipe)
    is StrengthWorkoutRoute -> stringResource(
        if (this.editingId > 0) R.string.app_title_edit_workout else R.string.app_title_strength_workout,
    )
    is VoiceLogRoute -> stringResource(R.string.app_title_voice_log)
    is MealIdeasRoute -> stringResource(R.string.app_title_meal_ideas)
    HealthConnectionRoute -> stringResource(R.string.app_title_google_health)
    FoodLibraryRoute -> stringResource(R.string.app_title_food_library)
    RoutinesRoute -> stringResource(R.string.app_title_routines)
    SupplementsRoute -> stringResource(R.string.app_title_supplements)
    HomeLayoutRoute -> stringResource(R.string.app_title_home_layout)
    SettingsRoute -> stringResource(R.string.app_title_settings)
    AboutYouRoute -> stringResource(R.string.app_title_about_you)
    RemindersRoute -> stringResource(R.string.app_title_reminders)
    is PhotoComparisonRoute -> stringResource(R.string.app_title_compare)
    TimelapseRoute -> stringResource(R.string.app_title_timelapse)
    RecapRoute -> stringResource(R.string.app_title_recap)
    else -> ""
}

/**
 * The Progress subject a Home card is a door to — tapping a card's body opens that subject's page.
 *
 * It maps to [Subject] rather than to fourteen route types because [Subject.route] is already the
 * one place a subject becomes a `NavKey`, and a second table naming the same routes is a second
 * thing to keep in step. It lives here for the reason [ProfileDetailRoutes] and `title()` do:
 * `:feature:home` cannot import `:feature:progress`, and this is the one file that sees both.
 *
 * Water is the only null: it is the one card with no subject page behind it, so its card takes no
 * tap at all — `HomeCardContent` never hands it an `onClick`, and this returns null for the same
 * reason. The `when` is exhaustive, so a card added later has to answer the question here.
 */
internal fun HomeCard.subject(): Subject? = when (this) {
    // All three nutrition cards open the same page rather than switching to the Food tab: the card
    // reports the day or the week, and the page is where either sits in a series. One rule for all
    // fifteen.
    HomeCard.Calories, HomeCard.Macros, HomeCard.WeekBudget -> Subject.Nutrition
    HomeCard.Water -> null
    HomeCard.Streak -> Subject.Badges
    HomeCard.Weight -> Subject.Weight
    HomeCard.Steps -> Subject.Activity
    HomeCard.Sleep -> Subject.Sleep
    HomeCard.Heart -> Subject.Heart
    HomeCard.BloodPressure -> Subject.BloodPressure
    HomeCard.Fasting -> Subject.Fasting
    HomeCard.Mood -> Subject.Mood
    HomeCard.Supplements -> Subject.Supplements
    HomeCard.Cycle -> Subject.Cycle
    HomeCard.Workout -> Subject.Strength
    HomeCard.ProgressPhoto -> Subject.Photos
}

/**
 * The eight routes that draw *beside* Profile once the window is wide enough, rather than over it.
 * One list, read by both the pane metadata in `profileEntries` and by [showsTabChrome], so the
 * scene and the chrome can never disagree about which routes are panes.
 */
internal val ProfileDetailRoutes: Set<NavKey> = setOf(
    HealthConnectionRoute,
    FoodLibraryRoute,
    RoutinesRoute,
    SupplementsRoute,
    HomeLayoutRoute,
    SettingsRoute,
    AboutYouRoute,
    RemindersRoute,
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

/** The FAB's overlay sheet — Log exercise and Log weight are real [ph.mart.healthapp.core.designsystem.component.AppBottomSheet]s
 * shown here (same shape as [QuickActionSheet] itself), not [androidx.navigation3.runtime.NavKey]
 * routes: predictive back needs to close the sheet without replacing the screen underneath it.
 * Add photo used to be the third and is [AddPhotoRoute] now — a flow that opens on a viewfinder
 * wants the window, which is the one thing a sheet cannot hand it. */
private enum class ActiveSheet { None, QuickAction, LogExercise, LogWeight }

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
    // [ActiveSheet.LogExercise]'s two arguments. Beside the enum rather than inside it because
    // the sheet is `rememberSaveable` and an `ExerciseEntry` is not — the diary names the row it
    // wants corrected by id, and the sheet resolves it. 0/0 is a new activity, today.
    var sheetDate by rememberSaveable { mutableStateOf(0L) }
    var sheetEditingId by rememberSaveable { mutableStateOf(0L) }
    // An idea picked on [MealIdeasRoute], on its way to the diary's add-entry sheet — the seed
    // cannot be handed down the back stack, so it is handed across up here, beside the sheet
    // arguments above. The diary consumes it on its next composition and clears it.
    // ponytail: plain remember, not rememberSaveable — MealIdea has no Saver and the value lives
    // for the one frame between the pop and the sheet reopening. Write one if a rotation ever
    // manages to land inside that frame.
    var pendingIdea by remember { mutableStateOf<MealIdea?>(null) }

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
            // Also not a shortcut: the weekly recap notification. The recap is a route now, so
            // this pushes it — onto the Progress tab named explicitly rather than onto whichever
            // tab is showing, since the same intent's EXTRA_TAB switch is a separate effect and
            // this must not depend on having run after it.
            ShortcutAction.OpenRecap -> {
                topLevelBackStack.addTopLevel(TopLevelDestination.Progress.route)
                topLevelBackStack.add(RecapRoute)
            }
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
    val fullBleed = current is FoodCaptureRoute || current is BarcodeScanRoute ||
        current is LabelScanRoute || current is AddPhotoRoute || current is SupplementScanRoute

    // Routes that draw their own `AppTopBar`. The camera flows do it full-bleed, under the system
    // bars; every Progress subject page keeps the window's insets and wants the bar's `actions`
    // slot, which the call below cannot fill — a page's share needs its own data, and all this has
    // is a `NavKey`. Deliberately not folded into [fullBleed]: the two want opposite insets.
    //
    // The history search is here for a third reason: tapping a result opens the shared review
    // screen, which brings its own bar, and a bar drawn from out here would stack on top of it.
    //
    // The add-photo preview is the second half of a flow whose first half is full-bleed, and it
    // wants the insets rather than the window: a form under a top bar, exactly a subject page's
    // shape. Its viewfinder is in [fullBleed] above, which is the split this line is named for.
    //
    // A route in here never reaches `title()`, which is why none of them has a branch there.
    //
    // The coach is here for the `actions` slot too: its overflow holds "Clear chat", which needs
    // the screen's own confirmation dialog, and it draws a pinned offline strip directly under the
    // bar that only that screen knows about.
    val ownsTopBar = fullBleed || current is FoodHistoryRoute || current is AddPhotoPreviewRoute ||
        current is CoachRoute || current in ProgressSubjectRoutes

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
    // The app shell's snackbar, and it has two senders: a saved workout and a downloaded update.
    // It lives here rather than on a screen because the sheet and the strength route that raise it
    // are both hosted here, and the surface they close onto is whichever tab happens to be
    // underneath — and an update belongs to the app rather than to any one screen, so it has no
    // other host to ask for.
    val snackbarHostState = remember { SnackbarHostState() }
    // A confirmation of something the user just did, so it is silent when there is nothing to
    // confirm: the profile's credit switch is off, the save was a correction, or the burn is under
    // the floor `EarnedCalories.kt` argues for.
    val onWorkoutSaved: (Int) -> Unit = { creditedKcal ->
        if (creditedKcal >= EARNED_MIN_KCAL) {
            scope.launch { snackbarHostState.showSnackbar(earnedSavedLine(creditedKcal)) }
        }
    }
    val tabItems = TopLevelDestination.entries.map { BottomNavItem(it.icon(), stringResource(it.label())) }
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
                    if (!isTopLevel && !ownsTopBar) {
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
                            onAddPhoto = { topLevelBackStack.add(AddPhotoRoute) },
                            onOpenCoach = { topLevelBackStack.add(CoachRoute()) },
                            // Day 0 is today, the convention the FAB's own sheet uses — the plan card
                            // only ever starts today's workout.
                            onStartRoutine = { routineId ->
                                topLevelBackStack.add(StrengthWorkoutRoute(0, 0, routineId))
                            },
                            // The same entry Profile's own row opens — Home just makes it findable.
                            onOpenHomeLayout = { topLevelBackStack.add(HomeLayoutRoute) },
                            // Pushed onto the *Home* tab's stack, not the Progress tab's: back
                            // returns to the card that was tapped, which is the whole point of
                            // the tap. The page draws its own top bar either way — `ownsTopBar`
                            // reads the route, not the tab it was reached from.
                            onOpenCard = { card ->
                                card.subject()?.let { topLevelBackStack.add(it.route()) }
                            },
                        )
                        // Switching tabs rather than pushing a route: the diary *is* the Food
                        // tab, and it opens on today — which is why the door is only offered for a
                        // draft that landed there.
                        coachEntries(
                            onOpenDiary = {
                                topLevelBackStack.addTopLevel(TopLevelDestination.Food.route)
                            },
                            // Day 0 is today, the same convention Home's plan card follows — a
                            // drafted routine is only ever started now. Pushed above the coach
                            // rather than replacing it, so back returns to the conversation.
                            onStartRoutine = { routineId ->
                                topLevelBackStack.add(StrengthWorkoutRoute(0, 0, routineId))
                            },
                            // A report section through to the page that owns it. Pushed above the
                            // coach rather than switching tabs — the diary door switches because
                            // the diary *is* a tab, while these four are routes, and back has to
                            // return to the conversation with the card still on it.
                            onOpenSection = { section ->
                                reportSectionRoute(section)?.let { topLevelBackStack.add(it) }
                            },
                            onExitFlow = { topLevelBackStack.removeLast() },
                        )
                        foodEntries(
                            scrollState = foodScroll,
                            twoPane = twoPane,
                            onScanBarcode = { date -> topLevelBackStack.add(BarcodeScanRoute(date)) },
                            // Pushed from inside the barcode flow rather than from the diary: the
                            // label is what a not-found code leaves you with, and it inherits that
                            // flow's day so the entry still lands where the diary was looking.
                            onScanLabel = { date -> topLevelBackStack.add(LabelScanRoute(date)) },
                            onSpeakFood = { date -> topLevelBackStack.add(VoiceLogRoute(date)) },
                            onCapturePhoto = { date -> topLevelBackStack.add(FoodCaptureRoute(date)) },
                            onOpenHistory = { date, query ->
                                topLevelBackStack.add(FoodHistoryRoute(date, query))
                            },
                            onNewRecipe = { topLevelBackStack.add(RecipeBuilderRoute) },
                            // The gap is worked out by the diary and rides the key — see
                            // [MealIdeasRoute]. Picking an idea pops the route and hands the seed
                            // back through [pendingIdea]; backing out hands back nothing.
                            onGetIdeas = { request -> topLevelBackStack.add(MealIdeasRoute(request)) },
                            onSelectIdea = { idea ->
                                pendingIdea = idea
                                topLevelBackStack.removeLast()
                            },
                            pendingIdea = pendingIdea,
                            onIdeaConsumed = { pendingIdea = null },
                            onOpenStrength = { date, editingId ->
                                topLevelBackStack.add(StrengthWorkoutRoute(date, editingId))
                            },
                            // The same door Home's mascot card opens, carrying the question the
                            // day raised. `CoachRoute` fills the field with it and never sends.
                            onAskCoach = { question, source -> topLevelBackStack.add(CoachRoute(question, source)) },
                            onLogExercise = { date, editingId ->
                                sheetDate = date
                                sheetEditingId = editingId
                                activeSheet = ActiveSheet.LogExercise
                            },
                            onExitFlow = { topLevelBackStack.removeLast() },
                        )
                        trainingEntries(
                            onExitFlow = { topLevelBackStack.removeLast() },
                            onSaved = onWorkoutSaved,
                        )
                        progressEntries(
                            scrollState = progressScroll,
                            onOpenSubject = { subject -> topLevelBackStack.add(subject.route()) },
                            onCompare = { first, second ->
                                topLevelBackStack.add(PhotoComparisonRoute(first, second))
                            },
                            onOpenTimelapse = { topLevelBackStack.add(TimelapseRoute) },
                            // Pushed above the viewfinder rather than replacing it, so a retake is
                            // one back press — the step the flow's own handler used to dispatch.
                            onOpenPhotoPreview = { path ->
                                topLevelBackStack.add(AddPhotoPreviewRoute(path))
                            },
                            onOpenRecap = { topLevelBackStack.add(RecapRoute) },
                            onAskCoach = { question, source -> topLevelBackStack.add(CoachRoute(question, source)) },
                            onExitFlow = { topLevelBackStack.removeLast() },
                        )
                        profileEntries(
                            scrollState = profileScroll,
                            onOpenSettings = { topLevelBackStack.add(SettingsRoute) },
                            onOpenAboutYou = { topLevelBackStack.add(AboutYouRoute) },
                            onOpenReminders = { topLevelBackStack.add(RemindersRoute) },
                            onOpenHealth = { topLevelBackStack.add(HealthConnectionRoute) },
                            onOpenLibrary = { topLevelBackStack.add(FoodLibraryRoute) },
                            onOpenRoutines = { topLevelBackStack.add(RoutinesRoute) },
                            onOpenSupplements = { topLevelBackStack.add(SupplementsRoute) },
                            onOpenSupplementScan = { topLevelBackStack.add(SupplementScanRoute) },
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
                onScanBarcode = {
                    activeSheet = ActiveSheet.None
                    topLevelBackStack.add(BarcodeScanRoute(0))
                },
                onLogFood = {
                    activeSheet = ActiveSheet.None
                    topLevelBackStack.add(FoodCaptureRoute(0))
                },
                onLogExercise = {
                    sheetDate = 0
                    sheetEditingId = 0
                    activeSheet = ActiveSheet.LogExercise
                },
                onLogWeight = { activeSheet = ActiveSheet.LogWeight },
                onAddPhoto = {
                    activeSheet = ActiveSheet.None
                    topLevelBackStack.add(AddPhotoRoute)
                },
            )
            ActiveSheet.LogExercise -> LogExerciseSheet(
                onDismiss = {
                    activeSheet = ActiveSheet.None
                    sheetDate = 0
                    sheetEditingId = 0
                },
                onSaved = onWorkoutSaved,
                // The FAB's sheet carries no day, so the workout screen it opens gets 0 too —
                // which the repository stamps as today, exactly as the sheet's own save would.
                // The diary's does carry one, and the row being corrected rides with it.
                onOpenStrength = { date ->
                    val editingId = sheetEditingId
                    activeSheet = ActiveSheet.None
                    sheetDate = 0
                    sheetEditingId = 0
                    topLevelBackStack.add(StrengthWorkoutRoute(date, editingId))
                },
                dateEpochDay = sheetDate,
                editingId = sheetEditingId,
            )
            ActiveSheet.LogWeight -> LogWeightSheet(onDismiss = { activeSheet = ActiveSheet.None })
            ActiveSheet.None -> Unit
        }

        // Above the docked FAB, the placement `FoodScreen` already uses for its undo — a
        // confirmation hidden behind the button that raised it is no confirmation.
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = DockedFabContentPadding),
        )

        AppUpdatePrompt(snackbarHostState)
    }
}

/**
 * A report-card section, to the Progress page that owns it.
 *
 * `:app` is the only module that can see both ends — `ReportSection` is `:core:data`'s and the
 * four pages are `:feature:progress`'s — which is why the coach hands out a `name` and this is
 * where it becomes a route. Through [Subject] and its existing `route()` rather than naming the
 * four `NavKey`s directly, so this cannot drift out of step with `ProgressSubjectRoutes`.
 *
 * Null only on a name from a build that knew a section this one does not. The card would have to
 * come from a transcript written by a newer install, which no path produces today — but a crash
 * is the wrong answer to it either way, and nothing happening is the same shrug the diary door
 * already gives a draft it cannot honestly point at.
 */
private fun reportSectionRoute(section: String): NavKey? = when (section) {
    ReportSection.Nutrition.name -> Subject.Nutrition.route()
    ReportSection.Steps.name -> Subject.Activity.route()
    ReportSection.Training.name -> Subject.Strength.route()
    ReportSection.Weight.name -> Subject.Weight.route()
    else -> null
}
