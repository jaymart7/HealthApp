package ph.mart.healthapp.feature.food.ui.diary

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
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
import ph.mart.healthapp.core.designsystem.component.DockedFabContentPadding
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.food.ui.diary.components.DiaryBody
import ph.mart.healthapp.feature.food.ui.diary.components.DiarySheets

/** Roughly one diary row. Past this the summary is no longer the thing being looked at. */
private val SUMMARY_COLLAPSE_THRESHOLD = 24.dp

@Composable
fun FoodScreen(
    onScanBarcode: (Long) -> Unit,
    onSpeakFood: (Long) -> Unit,
    onCapturePhoto: (Long) -> Unit,
    onNewRecipe: () -> Unit,
    onOpenStrength: (Long, Long) -> Unit,
    scrollState: ScrollState = rememberScrollState(),
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
        onNewRecipe = onNewRecipe,
        onOpenStrength = onOpenStrength,
        scrollState = scrollState,
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
    onNewRecipe: () -> Unit,
    onOpenStrength: (Long, Long) -> Unit,
    scrollState: ScrollState = rememberScrollState(),
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
            DiaryBody(
                uiState = uiState,
                state = state,
                onEvent = onEvent,
                onScanBarcode = onScanBarcode,
                onSpeakFood = onSpeakFood,
                onCapturePhoto = onCapturePhoto,
                onOpenStrength = onOpenStrength,
                snackbarHostState = snackbarHostState,
                scrollState = scrollState,
                summaryCollapsed = summaryCollapsed,
            )

            DiarySheets(
                uiState = uiState,
                state = state,
                onEvent = onEvent,
                onNewRecipe = onNewRecipe,
                onOpenStrength = onOpenStrength,
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

@PreviewLightDark
@PreviewScreenSizes
@Composable
private fun FoodScreenPreview() {
    val entries = listOf(
        FoodEntry(id = 1, name = "Greek yogurt", mealType = MealType.Breakfast, portionAmount = 1.0, portionUnit = "cup", calories = 150, proteinG = 20, carbsG = 8, fatG = 4),
        FoodEntry(id = 2, name = "Grilled chicken breast", mealType = MealType.Lunch, portionAmount = 150.0, portionUnit = "g", calories = 210, proteinG = 32, carbsG = 2, fatG = 8),
    )
    val targets = DailyTargets(calories = 1941, proteinG = 146, carbsG = 194, fatG = 65, floor = 1500)
    AppTheme {
        FoodContent(
            uiState = FoodUiState(
                entries = entries,
                targets = targets,
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
            ),
            state = FoodScreenState(),
            onEvent = {},
            onScanBarcode = {},
            onSpeakFood = {},
            onCapturePhoto = {},
            onNewRecipe = {},
            onOpenStrength = { _, _ -> },
        )
    }
}
