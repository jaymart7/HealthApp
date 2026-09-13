package ph.mart.healthapp.feature.progress.ui.progress

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import org.koin.androidx.compose.koinViewModel
import org.orbitmvi.orbit.compose.collectAsState
import ph.mart.healthapp.core.data.food.DayNutrition
import ph.mart.healthapp.core.data.mood.MoodDay
import ph.mart.healthapp.core.data.profile.DailyTargets
import ph.mart.healthapp.core.data.profile.Goal
import ph.mart.healthapp.core.data.profile.UnitSystem
import ph.mart.healthapp.core.data.progress.WeightEntry
import ph.mart.healthapp.core.data.progress.goalProjection
import ph.mart.healthapp.core.data.todayEpochDay
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.progress.ui.cycle.LogCycleSheet
import ph.mart.healthapp.feature.progress.ui.pressure.LogBloodPressureSheet
import ph.mart.healthapp.feature.progress.ui.progress.components.ProgressOverview
import ph.mart.healthapp.feature.progress.ui.shared.DEFAULT_RECAP_PERIOD
import ph.mart.healthapp.feature.progress.ui.shared.recap

/**
 * The tab itself: the overview, and the two log sheets its empty-card hints can raise.
 *
 * Every subject page is a route now, pushed by `AppScaffold` — so a tap here is a plain callback
 * rather than a field written for an effect to consume, and this screen has no navigator of its own
 * left. The comparison and the timelapse are not among the callbacks: both are reached from the
 * Photos page, which is itself a route and raises them directly.
 */
@Composable
fun ProgressScreen(
    scrollState: ScrollState = rememberScrollState(),
    onOpenSubject: (Subject) -> Unit = {},
    onOpenRecap: () -> Unit = {},
    viewModel: ProgressViewModel = koinViewModel(),
) {
    val uiState by viewModel.collectAsState()
    ProgressContent(
        uiState = uiState,
        state = rememberProgressScreenState(),
        scrollState = scrollState,
        onOpenSubject = onOpenSubject,
        onOpenRecap = onOpenRecap,
    )
}

/**
 * One column at every width.
 *
 * This tab used to draw two panes at ≥840dp, a `Row` over the `selectedSubject` a swap-in already
 * had. There is no `selectedSubject` any more: a subject page is a route, and a route drawn beside
 * the tab that pushed it would need a `ListDetailSceneStrategy` scene the Progress overview has
 * never been shaped for. See `DECISIONS.md` -> **Adaptive layout**.
 */
@Composable
private fun ProgressContent(
    uiState: ProgressUiState,
    state: ProgressScreenState,
    scrollState: ScrollState = rememberScrollState(),
    onOpenSubject: (Subject) -> Unit = {},
    onOpenRecap: () -> Unit = {},
) {
    val today = todayEpochDay()
    // Above everything and inside nothing: the recap spans nutrition, weight and consistency at
    // once. Null (nothing logged this week) omits the card entirely rather than rendering an
    // all-zero one on day one — and takes the share door with it, since there is then nothing to
    // report. Always the week here; the longer periods are the recap screen's, which folds its own.
    val weekRecap = recap(
        period = DEFAULT_RECAP_PERIOD,
        dailyNutrition = uiState.dailyNutrition,
        activeDays = uiState.activeDays,
        weightEntries = uiState.weightEntries,
        moodDays = uiState.moodDays,
        targets = uiState.targets,
        todayEpochDay = today,
    )
    val projection = goalProjection(
        weightEntries = uiState.weightEntries,
        goalWeightKg = uiState.goalWeightKg,
        goal = uiState.goal,
        todayEpochDay = today,
    )
    Surface(color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize()) {
            ProgressOverview(
                uiState = uiState,
                state = state,
                weekRecap = weekRecap,
                projection = projection,
                scrollState = scrollState,
                onOpenSubject = onOpenSubject,
                onOpenRecap = onOpenRecap,
            )

            if (state.activeBloodPressureSheet) {
                LogBloodPressureSheet(onDismiss = state::closeBloodPressureSheet)
            }

            // Handed the days it seeds from — the sheet reads the same combined state the page
            // does, so opening a day twice shows what it says rather than a blank form.
            if (state.activeCycleSheet) {
                LogCycleSheet(days = uiState.cycleDays, onDismiss = state::closeCycleSheet)
            }
        }
    }
}

private fun previewState(): ProgressUiState {
    val today = todayEpochDay()
    return ProgressUiState(
        weightEntries = (0..8).map {
            WeightEntry(dateEpochDay = today - (8 - it) * 3, weightKg = 84.8 - it * 0.26)
        },
        goalWeightKg = 82.0,
        goal = Goal.Lose,
        preferredUnit = UnitSystem.Metric,
        dailyNutrition = listOf(1850, 2100, 0, 1720, 2340, 1610, 1490).mapIndexed { index, calories ->
            DayNutrition(today - 6 + index, calories, calories / 16, calories / 10, calories / 30)
        },
        activeDays = (today - 6..today).toSet(),
        moodDays = listOf(4 to 3, 5 to 4, 3 to 2, 4 to 4).mapIndexed { index, (mood, energy) ->
            MoodDay(today - 4 + index, mood, energy)
        },
        targets = DailyTargets(calories = 2261, proteinG = 170, carbsG = 226, fatG = 75, floor = 1500),
    )
}

@PreviewLightDark
@Composable
private fun ProgressScreenPreview() {
    AppTheme {
        ProgressContent(uiState = previewState(), state = ProgressScreenState())
    }
}

