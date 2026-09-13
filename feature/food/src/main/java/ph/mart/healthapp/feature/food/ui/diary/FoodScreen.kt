package ph.mart.healthapp.feature.food.ui.diary

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import org.koin.androidx.compose.koinViewModel
import org.orbitmvi.orbit.compose.collectAsState
import ph.mart.healthapp.core.data.food.FoodEntry
import ph.mart.healthapp.core.data.food.FoodSuggestion
import ph.mart.healthapp.core.data.food.MealType
import ph.mart.healthapp.core.data.food.SavedMeal
import ph.mart.healthapp.core.data.food.SavedMealItem
import ph.mart.healthapp.core.data.profile.DailyTargets
import ph.mart.healthapp.core.designsystem.component.CalendarPanel
import ph.mart.healthapp.core.designsystem.component.DockedFabContentPadding
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.food.ui.diary.components.DiaryBody
import ph.mart.healthapp.feature.food.ui.diary.components.DiarySheets

/** Roughly one diary row. Past this the summary is no longer the thing being looked at. */
private val SUMMARY_COLLAPSE_THRESHOLD = 24.dp

/**
 * The calendar pane's width, fixed rather than weighted — the one place this tab departs from
 * `ProgressContent`'s two panes, and for the reason that tab's own weights are documented with:
 * what is in the pane decides. A month grid is seven fixed 44dp cells (`CalendarPanel`'s `DayCell`),
 * so a weighted pane spends every extra pixel spreading them apart, while Progress's grid of cards
 * and its charts both use the width they are given. 7 × 44 = 308, plus the padding around it.
 */
private val CalendarPaneWidth = 320.dp

@Composable
fun FoodScreen(
    onScanBarcode: (Long) -> Unit,
    onSpeakFood: (Long) -> Unit,
    onCapturePhoto: (Long) -> Unit,
    onOpenHistory: (Long, String) -> Unit,
    onNewRecipe: () -> Unit,
    onOpenStrength: (Long, Long) -> Unit,
    onLogExercise: (Long, Long) -> Unit,
    onAskCoach: (String) -> Unit,
    scrollState: ScrollState = rememberScrollState(),
    twoPane: Boolean = false,
    viewModel: FoodViewModel = koinViewModel(),
) {
    val uiState by viewModel.collectAsState()
    val state = rememberFoodScreenState()
    FoodContent(
        uiState = uiState,
        state = state,
        onEvent = viewModel::handleEvent,
        onScanBarcode = onScanBarcode,
        onSpeakFood = onSpeakFood,
        onCapturePhoto = onCapturePhoto,
        onOpenHistory = onOpenHistory,
        onNewRecipe = onNewRecipe,
        onOpenStrength = onOpenStrength,
        onLogExercise = onLogExercise,
        onAskCoach = onAskCoach,
        scrollState = scrollState,
        twoPane = twoPane,
    )
}

@Composable
private fun FoodContent(
    uiState: FoodUiState,
    state: FoodScreenState,
    onEvent: (FoodEvent) -> Unit,
    onScanBarcode: (Long) -> Unit,
    onSpeakFood: (Long) -> Unit,
    onCapturePhoto: (Long) -> Unit,
    onOpenHistory: (Long, String) -> Unit,
    onNewRecipe: () -> Unit,
    onOpenStrength: (Long, Long) -> Unit,
    onLogExercise: (Long, Long) -> Unit,
    onAskCoach: (String) -> Unit,
    scrollState: ScrollState = rememberScrollState(),
    twoPane: Boolean = false,
) {
    // Back off a past day returns to today rather than leaving the tab — one level, same rule the
    // sheets and the calendar swap-in follow. On today no handler is registered at all.
    if (uiState.selectedDate != uiState.today) {
        val navigationState = rememberNavigationEventState(currentInfo = NavigationEventInfo.None)
        NavigationBackHandler(
            state = navigationState,
            onBackCompleted = { onEvent(FoodEvent.OnSelectDate(uiState.today)) },
        )
    }

    // The sheet and the pane are the same calendar in two presentations, so only one of them may
    // ever be showing. Opening the sheet on a phone and unfolding into a tablet would otherwise
    // draw it over a calendar that is already on screen.
    LaunchedEffect(twoPane) { if (twoPane) state.calendarOpen = false }

    val snackbarHostState = remember { SnackbarHostState() }

    // The summary bar's scrolled state. Derived here rather than inside the bar because the scroll
    // it watches is hoisted all the way to AppScaffold — the same ScrollState the FAB's
    // expand/collapse and the tap-the-active-tab-to-scroll-to-top gesture already read.
    //
    // Asymmetric on purpose: it collapses once the day's first row is genuinely being read past,
    // and comes back **only at the very top**. A threshold that restored on any upward scroll would
    // have the summary reappear and push the list down every time someone corrected an overshoot
    // mid-diary, which is the one thing a pinned block must never do. Not reset on a day change,
    // because the scroll position is not either.
    val threshold = with(LocalDensity.current) { SUMMARY_COLLAPSE_THRESHOLD.roundToPx() }
    var summaryCollapsed by remember { mutableStateOf(false) }
    LaunchedEffect(scrollState, threshold) {
        // snapshotFlow rather than a read in composition: scrollState.value changes every frame
        // and this boolean flips twice a screen.
        snapshotFlow { scrollState.value }.collect { offset ->
            summaryCollapsed = when {
                offset == 0 -> false
                offset > threshold -> true
                else -> summaryCollapsed
            }
        }
    }

    Surface(color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize()) {
            val body = @Composable { modifier: Modifier ->
                DiaryBody(
                    uiState = uiState,
                    state = state,
                    onEvent = onEvent,
                    onScanBarcode = onScanBarcode,
                    onSpeakFood = onSpeakFood,
                    onCapturePhoto = onCapturePhoto,
                    onOpenHistory = onOpenHistory,
                    onOpenStrength = onOpenStrength,
                    onLogExercise = onLogExercise,
                    onAskCoach = onAskCoach,
                    snackbarHostState = snackbarHostState,
                    modifier = modifier,
                    scrollState = scrollState,
                    summaryCollapsed = summaryCollapsed,
                    twoPane = twoPane,
                )
            }
            // The calendar the date header opens in a sheet, drawn beside the day it picks instead
            // — the pane `DECISIONS.md` said would earn itself. Nothing else about the diary moves:
            // it is still one scrolling day, and the sheets still cover both panes.
            if (twoPane) {
                Row(modifier = Modifier.fillMaxSize()) {
                    CalendarPane(
                        selectedDate = uiState.selectedDate,
                        today = uiState.today,
                        onSelectDate = { date -> onEvent(FoodEvent.OnSelectDate(date)) },
                        modifier = Modifier.width(CalendarPaneWidth),
                    )
                    VerticalDivider()
                    body(Modifier.weight(1f))
                }
            } else {
                body(Modifier)
            }

            DiarySheets(
                uiState = uiState,
                state = state,
                onEvent = onEvent,
                onNewRecipe = onNewRecipe,
            )

            // Above the docked FAB, so an Undo is never the thing hidden behind it.
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = DockedFabContentPadding),
            )
        }
    }
}

/**
 * Scrolls, because the window that is wide is often short: a six-week month at a large font scale
 * is taller than a landscape foldable, and a clipped calendar puts days out of reach.
 */
@Composable
private fun CalendarPane(
    selectedDate: Long,
    today: Long,
    onSelectDate: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        CalendarPanel(
            selectedDate = selectedDate,
            // No dots, for the reason the sheet gives: which days have entries would cost a query
            // the diary otherwise never makes. Add it if the pane starts feeling blind.
            markedDates = emptySet(),
            maxDate = today,
            onSelectDate = onSelectDate,
            // Beside the day rather than over it — nothing to go back to.
            onBack = null,
        )
    }
}

/** One day, shared by both layout previews — a `val preview*` so `checkUiLiterals` reads it as
 * the debug-only fixture it is. */
private val previewUiState = FoodUiState(
    entries = listOf(
        FoodEntry(id = 1, name = "Greek yogurt", mealType = MealType.Breakfast, portionAmount = 1.0, portionUnit = "cup", calories = 150, proteinG = 20, carbsG = 8, fatG = 4),
        FoodEntry(id = 2, name = "Grilled chicken breast", mealType = MealType.Lunch, portionAmount = 150.0, portionUnit = "g", calories = 210, proteinG = 32, carbsG = 2, fatG = 8),
    ),
    targets = DailyTargets(calories = 1941, proteinG = 146, carbsG = 194, fatG = 65, floor = 1500),
    suggestions = listOf(
        FoodSuggestion("Greek yogurt", 1.0, "cup", 150, 20, 8, 4, isFavorite = true),
    ),
    savedMeals = listOf(
        SavedMeal(
            id = 1,
            name = "Usual breakfast",
            items = listOf(SavedMealItem("Greek yogurt", 1.0, "cup", 150, 20, 8, 4)),
        ),
    ),
)

@PreviewLightDark
@PreviewScreenSizes
@Composable
private fun FoodScreenPreview() {
    AppTheme {
        FoodContent(
            uiState = previewUiState,
            state = FoodScreenState(),
            onEvent = {},
            onScanBarcode = {},
            onSpeakFood = {},
            onCapturePhoto = {},
            onOpenHistory = { _, _ -> },
            onNewRecipe = {},
            onOpenStrength = { _, _ -> },
            onLogExercise = { _, _ -> },
            onAskCoach = {},
        )
    }
}

/** The expanded-width layout: the calendar beside the day, and a header that opens nothing. */
@PreviewLightDark
@PreviewScreenSizes
@Composable
private fun FoodScreenTwoPanePreview() {
    AppTheme {
        FoodContent(
            uiState = previewUiState,
            state = FoodScreenState(),
            onEvent = {},
            onScanBarcode = {},
            onSpeakFood = {},
            onCapturePhoto = {},
            onOpenHistory = { _, _ -> },
            onNewRecipe = {},
            onOpenStrength = { _, _ -> },
            onLogExercise = { _, _ -> },
            onAskCoach = {},
            twoPane = true,
        )
    }
}
