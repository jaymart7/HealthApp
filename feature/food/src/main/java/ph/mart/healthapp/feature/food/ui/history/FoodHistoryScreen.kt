package ph.mart.healthapp.feature.food.ui.history

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.orbitmvi.orbit.compose.collectAsState
import ph.mart.healthapp.core.data.food.FoodEntry
import ph.mart.healthapp.core.data.food.MealType
import ph.mart.healthapp.core.data.todayEpochDay
import ph.mart.healthapp.core.designsystem.component.AppTopBar
import ph.mart.healthapp.core.designsystem.component.FullScreenState
import ph.mart.healthapp.core.designsystem.component.MascotAvatar
import ph.mart.healthapp.core.designsystem.component.MascotState
import ph.mart.healthapp.core.designsystem.component.SecondaryButton
import ph.mart.healthapp.core.designsystem.component.SegmentedToggle
import ph.mart.healthapp.core.designsystem.icon.AppIcons
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.food.R
import ph.mart.healthapp.feature.food.ui.history.components.HistoryAgeBand
import ph.mart.healthapp.feature.food.ui.history.components.HistoryCountLine
import ph.mart.healthapp.feature.food.ui.history.components.HistoryDayHeader
import ph.mart.healthapp.feature.food.ui.history.components.HistoryRecentQueries
import ph.mart.healthapp.feature.food.ui.history.components.HistoryRow
import ph.mart.healthapp.feature.food.ui.history.components.HistorySearchField
import ph.mart.healthapp.feature.food.ui.history.components.HistorySkeletonRow
import ph.mart.healthapp.feature.food.ui.shared.AddEntryForm
import ph.mart.healthapp.feature.food.ui.shared.components.ScanConfirmationScreen
import ph.mart.healthapp.feature.food.ui.shared.toFoodEntry

/**
 * Search everything ever logged, by name — the one screen that reads the diary across days, where
 * the diary's own filter only ever narrows the day that is open.
 *
 * A route rather than an overlay on the diary: meal ideas is an overlay because it is built off
 * state the diary already holds, and this is the opposite — its own query, its own read, its own
 * ViewModel, which is what `CLAUDE.md` says earns a package.
 *
 * [dateEpochDay] is the day the diary was showing, carried the way `BarcodeScanRoute` carries it,
 * and it is where a re-logged row lands. [query] is whatever was already typed into the day's
 * filter, so walking up from a day that had nothing doesn't cost the user their word twice. It
 * seeds the field once; the search that follows is also what loads the opening list, since a blank
 * query is simply the newest rows.
 *
 * **Two levels, and back walks them.** Tapping a result opens it for review rather than logging it
 * — the meal, the portion and the figures are all wrong sometimes, and this screen's confirmation
 * carries no Undo — so back closes the review and returns to the list. On the list there is nothing
 * of its own to dismiss and no handler is registered at all, which leaves system back to pop the
 * route: the rule `FoodScreen` follows for a past day. [onExit] is the toolbar arrow's twin, and
 * exists because this route draws its own `AppTopBar` — the review screen behind the tap brings one
 * of its own, and two stacked bars is what `AppScaffold` drawing it for us would mean.
 */
@Composable
fun FoodHistoryScreen(
    dateEpochDay: Long,
    query: String,
    onExit: () -> Unit,
    viewModel: FoodHistoryViewModel = koinViewModel(),
) {
    val uiState by viewModel.collectAsState()
    LaunchedEffect(Unit) { viewModel.handleEvent(FoodHistoryEvent.OnQueryChange(query)) }
    FoodHistoryContent(
        uiState = uiState,
        dateEpochDay = dateEpochDay,
        onEvent = viewModel::handleEvent,
        onExit = onExit,
    )
}

@Composable
private fun FoodHistoryContent(
    uiState: FoodHistoryUiState,
    dateEpochDay: Long,
    onEvent: (FoodHistoryEvent) -> Unit,
    onExit: () -> Unit,
    state: FoodHistoryScreenState = rememberFoodHistoryScreen(),
    listState: LazyListState = rememberLazyListState(),
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    // The confirmation is built in a coroutine, outside composition, so the meal's name is
    // resolved through the context rather than with `stringResource` — DiaryBody's rule.
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    // Opening this screen *is* the intent to type, so the field arrives focused — the same
    // handover the diary's filter icon makes.
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    // Except over an empty diary, where the keyboard would cover the sentence explaining why the
    // screen is empty. Only knowable once the first read is back, so the focus is taken away
    // rather than never given: the alternative is a frame with no field focus at all on every
    // other run.
    val nothingLogged = uiState.searched && !uiState.searching &&
        uiState.results.isEmpty() && uiState.query.isBlank() && uiState.mealFilter == null
    LaunchedEffect(nothingLogged) { if (nothingLogged) focusManager.clearFocus() }

    // Back closes the review and returns to the list. On the list nothing is registered at all, so
    // back pops the route — the conditional shape `FoodScreen` uses for a past day.
    if (state.form != null) {
        val navigationState = rememberNavigationEventState(currentInfo = NavigationEventInfo.None)
        NavigationBackHandler(state = navigationState, onBackCompleted = state::dismiss)
    }

    Surface(
        color = MaterialTheme.colorScheme.surface,
        // This route owns its top bar, so `AppScaffold` draws none and pads the window's top inset
        // instead. Consumed here, once, rather than passed as `WindowInsets(0)` to two bars: the
        // review screen is shared with the barcode and photo flows, which are full-bleed and do
        // apply it themselves.
        modifier = Modifier.fillMaxSize().consumeWindowInsets(WindowInsets.statusBars),
    ) {
        val form = state.form
        if (form != null) ScanConfirmationScreen(
            form = form,
            // A logged row's portion is the one that was eaten, not a per-100 g database row —
            // the value `PortionControl` names an edit of a logged meal as passing. It hides
            // the gram presets and drops the per-100 g caveat, both of which would be claims
            // about arithmetic nobody did.
            manualEntry = true,
            onFormChange = { state.form = it },
            onMealTypeSelect = state::selectMealType,
            onLogEntry = {
                // The diary's day, not today — the rule this screen's route carries the date for.
                val logged = form.toFoodEntry(dateEpochDay)
                onEvent(FoodHistoryEvent.OnLog(logged))
                state.dismiss()
                scope.launch {
                    snackbarHostState.showSnackbar(
                        message = context.getString(
                            R.string.food_history_logged,
                            logged.name,
                            context.getString(logged.mealType.labelRes),
                        ),
                        duration = SnackbarDuration.Short,
                    )
                }
            },
            // Nothing to confirm: the edits are seconds old and the row they came from is one
            // tap away in the list behind this.
            onDiscard = state::dismiss,
        ) else Box(modifier = Modifier.fillMaxSize()) {
            // The field arrives focused, so the keyboard is up before the first result is: without
            // this the list runs on behind it.
            Column(modifier = Modifier.fillMaxSize().imePadding()) {
                HistoryTopBar(
                    query = uiState.query,
                    // Once the list has moved, the title is a word the user can already see in the
                    // field below it, and the field is what they scrolled past. The bar becomes the
                    // query so it stays legible, and clearing stays reachable without scrolling back.
                    collapsed = listState.canScrollBackward,
                    onClear = { onEvent(FoodHistoryEvent.OnQueryChange("")) },
                    onExit = onExit,
                )
                HistorySearchField(
                    value = uiState.query,
                    onValueChange = { onEvent(FoodHistoryEvent.OnQueryChange(it)) },
                    searching = uiState.searching,
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .focusRequester(focusRequester),
                )
                MealFilterRow(
                    selected = uiState.mealFilter,
                    onSelect = { onEvent(FoodHistoryEvent.OnMealFilterChange(it)) },
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp),
                )
                // Only under an empty field — see HistoryRecentQueries for why.
                if (uiState.query.isBlank() && uiState.recentQueries.isNotEmpty()) {
                    HistoryRecentQueries(
                        queries = uiState.recentQueries,
                        onSelect = { onEvent(FoodHistoryEvent.OnQueryChange(it)) },
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp),
                    )
                }
                // Nothing is drawn until the first read comes back, or the empty page flashes over
                // a list that is one frame away.
                if (uiState.results.isEmpty()) {
                    if (uiState.searched && !uiState.searching) {
                        EmptyHistory(
                            query = uiState.query,
                            filtered = uiState.mealFilter != null,
                            onClear = {
                                onEvent(FoodHistoryEvent.OnMealFilterChange(null))
                                onEvent(FoodHistoryEvent.OnQueryChange(""))
                            },
                        )
                    }
                } else {
                    HistoryCountLine(
                        matches = uiState.matchCount,
                        days = uiState.dayCount,
                        query = uiState.query,
                        searching = uiState.searching,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                    HistoryList(
                        uiState = uiState,
                        listState = listState,
                        onLoadMore = { onEvent(FoodHistoryEvent.OnLoadMore) },
                        onSelect = { entry ->
                            // The word that found this row is the one worth keeping.
                            onEvent(FoodHistoryEvent.OnQueryUsed)
                            state.review(entry)
                        },
                    )
                }
            }
            SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))
        }
    }
}

/**
 * The bar, in its two forms.
 *
 * `AppTopBar` in both, rather than a second bar of this screen's own: what changes is the title
 * slot and one action, which is what that component's parameters are for. The rule beneath the
 * collapsed form is what separates it from the field it is standing in for.
 */
@Composable
private fun HistoryTopBar(
    query: String,
    collapsed: Boolean,
    onClear: () -> Unit,
    onExit: () -> Unit,
) {
    val collapsedBar = collapsed && query.isNotBlank()
    val clearLabel = stringResource(R.string.food_history_clear)
    Column {
        AppTopBar(
            title = stringResource(R.string.food_history_title),
            onBack = onExit,
            titleStyle = if (collapsedBar) {
                MaterialTheme.typography.bodyLarge
            } else {
                MaterialTheme.typography.titleLarge
            },
            actions = {
                if (collapsedBar) {
                    IconButton(onClick = onClear) {
                        Icon(
                            imageVector = AppIcons.Close,
                            contentDescription = clearLabel,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            },
            // The window's top inset is consumed above; applying it again would gap the bar.
            windowInsets = WindowInsets(0),
        )
        if (collapsedBar) {
            HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}

/**
 * All / Breakfast / Lunch / Dinner / Snacks.
 *
 * `SegmentedToggle`, not a row of chips: this app has no chip idiom at all, and the toggle is
 * already the single-select row that scrolls rather than squeezing when its options outgrow the
 * width — which five of them do. A second idiom here would be a second thing to keep in step.
 *
 * It costs 52dp of the most expensive space on this screen. What buys it is that the filter runs
 * in the query rather than over its results, so narrowing to Lunch reaches back through the whole
 * diary instead of through whatever the cap had already handed over.
 */
@Composable
private fun MealFilterRow(
    selected: MealType?,
    onSelect: (MealType?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val options = listOf(stringResource(R.string.food_history_filter_all)) +
        MealType.entries.map { stringResource(it.labelRes) }
    SegmentedToggle(
        options = options,
        // Index 0 is "All", so a meal's index is its ordinal plus one.
        selectedIndex = selected?.let { MealType.entries.indexOf(it) + 1 } ?: 0,
        onSelect = { index -> onSelect(if (index == 0) null else MealType.entries[index - 1]) },
        modifier = modifier.fillMaxWidth(),
    )
}

/**
 * The results, in the three levels they are scanned by: band, day, row.
 *
 * Day headers stick, which is what keeps the date visible while a long day is read past. Which one
 * is *stuck* is worked out from the running item index rather than asked of the list, because a
 * `stickyHeader` is not told: it is the last header the list has scrolled past.
 *
 * Header keys are the day's first row id rather than its date. The folds above only merge
 * *adjacent* rows, so two groups could share a date if the query ever stopped ordering by it — and
 * a duplicate key is a crash, not a glitch. An id is unique; the prefix keeps it from colliding
 * with the row that owns it.
 */
@Composable
private fun HistoryList(
    uiState: FoodHistoryUiState,
    listState: LazyListState,
    onLoadMore: () -> Unit,
    onSelect: (FoodEntry) -> Unit,
) {
    val today = todayEpochDay()
    val bands = remember(uiState.results, today) { uiState.results.bandedGroups(today) }
    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(bottom = 12.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        var index = 0
        bands.forEach { band ->
            index++
            item(key = "band-${band.days.first().entries.first().id}") { HistoryAgeBand(label = band.label) }
            band.days.forEach { day ->
                val headerIndex = index++
                stickyHeader(key = "day-${day.entries.first().id}") {
                    HistoryDayHeader(
                        dateEpochDay = day.dateEpochDay,
                        today = today,
                        totalKcal = uiState.dayTotals[day.dateEpochDay],
                        pinned = listState.firstVisibleItemIndex > headerIndex,
                    )
                }
                index += day.entries.size
                items(items = day.entries, key = { it.id }) { entry ->
                    HistoryRow(entry = entry, query = uiState.query, onSelect = { onSelect(entry) })
                }
            }
        }
        // At the tail, never over the list — see HistorySkeletonRow. The same two rows stand for
        // a new search and for the next page: both are rows about to arrive at the bottom.
        if (uiState.searching || uiState.appending) {
            items(items = listOf(0, 1), key = { "skeleton-$it" }) { HistorySkeletonRow() }
        }
    }
    // Reaching the bottom asks for the next page. The flag falls back to false as the page lands
    // and re-arms; at the end of the list the state stops changing and the ask is dropped, and
    // `endReached` drops it again in the ViewModel. The shape FoodSearchScreen uses over its own
    // scroll state.
    // ponytail: fires at the literal bottom, so the skeletons are seen for a frame. Trigger on
    // `layoutInfo.visibleItemsInfo.last().index >= totalItemsCount - 5` if that ever reads as a
    // stall rather than as loading.
    LaunchedEffect(listState) {
        snapshotFlow { !listState.canScrollForward }
            .distinctUntilChanged()
            .collect { atBottom -> if (atBottom) onLoadMore() }
    }
}

/**
 * Two pages, not one: a query that matched nothing is a different fact from a diary with nothing in
 * it yet, and the second is the one a first run needs to hear.
 *
 * Only the first gets an action. "Clear search" is a way forward when there is something to clear —
 * a word, a filter, or both — and on a diary that has never been written to there is nothing to
 * clear and nowhere to go from here but back.
 */
@Composable
private fun EmptyHistory(query: String, filtered: Boolean, onClear: () -> Unit) {
    val narrowed = query.isNotBlank() || filtered
    FullScreenState(
        icon = { MascotAvatar(state = MascotState.Sleepy, size = 64.dp) },
        heading = if (narrowed) {
            stringResource(R.string.food_history_empty_query, query)
        } else {
            stringResource(R.string.food_history_empty_diary)
        },
        body = if (narrowed) {
            stringResource(R.string.food_history_empty_query_body)
        } else {
            stringResource(R.string.food_history_empty_body)
        },
        actions = if (narrowed) {
            { SecondaryButton(modifier = Modifier.fillMaxWidth(), label = stringResource(R.string.food_history_clear), onClick = onClear) }
        } else {
            null
        },
    )
}

private val previewEntries = listOf(
    FoodEntry(id = 1, name = "Grilled chicken breast", dateEpochDay = 20_000L, mealType = MealType.Lunch, portionAmount = 150.0, portionUnit = "g", calories = 412, proteinG = 38, carbsG = 0, fatG = 9),
    FoodEntry(id = 2, name = "Chicken & rice bowl", dateEpochDay = 20_000L, mealType = MealType.Dinner, portionAmount = 1.0, portionUnit = "bowl", calories = 520, proteinG = 34, carbsG = 61, fatG = 14),
    FoodEntry(id = 3, name = "Chicken thigh, roasted", dateEpochDay = 19_998L, mealType = MealType.Dinner, portionAmount = 120.0, portionUnit = "g", calories = 245, proteinG = 26, carbsG = 0, fatG = 13),
    FoodEntry(id = 4, name = "Chicken satay skewers", dateEpochDay = 19_998L, mealType = MealType.Snacks, portionAmount = 2.0, portionUnit = "skewers", calories = 198, proteinG = 18, carbsG = 6, fatG = 11),
    FoodEntry(id = 5, name = "Chicken caesar salad", dateEpochDay = 19_970L, mealType = MealType.Lunch, portionAmount = 1.0, portionUnit = "bowl", calories = 385, proteinG = 29, carbsG = 12, fatG = 24),
)

// The counts are set rather than derived, because they are: a page's rows say nothing about how
// many matched in total, which is the whole point of the count line's own read.
private fun previewState(vararg entries: FoodEntry) = FoodHistoryUiState(
    query = "chick",
    searched = true,
    results = entries.toList(),
    dayTotals = mapOf(20_000L to 1_806, 19_998L to 2_104, 19_970L to 1_952),
    matchCount = entries.size,
    dayCount = entries.distinctBy { it.dateEpochDay }.size,
)

@PreviewLightDark
@Composable
private fun FoodHistoryScreenPreview() {
    AppTheme {
        FoodHistoryContent(
            uiState = previewState(*previewEntries.toTypedArray()),
            dateEpochDay = 20_000L,
            onEvent = {},
            onExit = {},
        )
    }
}

/** The opening state: a blank query, the newest rows, and the words that have worked before. */
@PreviewLightDark
@Composable
private fun FoodHistoryOpeningPreview() {
    AppTheme {
        FoodHistoryContent(
            uiState = previewState(*previewEntries.toTypedArray()).copy(
                query = "",
                recentQueries = listOf("chicken", "oats", "protein bar"),
            ),
            dateEpochDay = 20_000L,
            onEvent = {},
            onExit = {},
        )
    }
}

/** In flight, with the list held: the rows stay, two skeletons say more is coming. */
@PreviewLightDark
@Composable
private fun FoodHistorySearchingPreview() {
    AppTheme {
        FoodHistoryContent(
            uiState = previewState(previewEntries[0], previewEntries[1]).copy(searching = true),
            dateEpochDay = 20_000L,
            onEvent = {},
            onExit = {},
        )
    }
}

/**
 * Appending: the same two skeletons, but the field's progress line is dark and the count line
 * still reports the total rather than "Searching…". That difference is the whole reason
 * `appending` is not `searching`.
 */
@PreviewLightDark
@Composable
private fun FoodHistoryAppendingPreview() {
    AppTheme {
        FoodHistoryContent(
            uiState = previewState(*previewEntries.toTypedArray()).copy(appending = true, matchCount = 47, dayCount = 12),
            dateEpochDay = 20_000L,
            onEvent = {},
            onExit = {},
        )
    }
}

/** The second level: a row picked out of the list, ready to be corrected before it is written. */
@PreviewLightDark
@Composable
private fun FoodHistoryReviewPreview() {
    AppTheme {
        FoodHistoryContent(
            uiState = previewState(previewEntries[0]),
            dateEpochDay = 20_000L,
            onEvent = {},
            onExit = {},
            state = remember { FoodHistoryScreenState().apply { review(previewEntries[0]) } },
        )
    }
}

/** A query that matched nothing — the page with a way forward on it. */
@PreviewLightDark
@Composable
private fun FoodHistoryNoMatchPreview() {
    AppTheme {
        FoodHistoryContent(
            uiState = FoodHistoryUiState(query = "quinoa", searched = true),
            dateEpochDay = 20_000L,
            onEvent = {},
            onExit = {},
        )
    }
}

/** A diary with nothing in it yet — no count line, no action, and no keyboard over the sentence. */
@PreviewLightDark
@Composable
private fun FoodHistoryEmptyDiaryPreview() {
    AppTheme {
        FoodHistoryContent(
            uiState = FoodHistoryUiState(searched = true),
            dateEpochDay = 20_000L,
            onEvent = {},
            onExit = {},
        )
    }
}
