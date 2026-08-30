package ph.mart.healthapp.feature.progress.ui.progress

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import org.koin.androidx.compose.koinViewModel
import org.orbitmvi.orbit.compose.collectAsState
import ph.mart.healthapp.core.data.food.DayNutrition
import ph.mart.healthapp.core.data.health.SleepNight
import ph.mart.healthapp.core.data.mood.MoodDay
import ph.mart.healthapp.core.data.profile.DailyTargets
import ph.mart.healthapp.core.data.profile.Goal
import ph.mart.healthapp.core.data.profile.UnitSystem
import ph.mart.healthapp.core.data.progress.WeightEntry
import ph.mart.healthapp.core.designsystem.component.SegmentedToggle
import ph.mart.healthapp.core.designsystem.component.todayEpochDay
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.progress.ui.measurement.AddMeasurementSheet
import ph.mart.healthapp.feature.progress.ui.measurement.components.MeasurementsTabContent
import ph.mart.healthapp.feature.progress.ui.mood.components.MoodTabContent
import ph.mart.healthapp.feature.progress.ui.nutrition.components.NutritionTabContent
import ph.mart.healthapp.feature.progress.ui.photo.components.PhotoComparisonScreen
import ph.mart.healthapp.feature.progress.ui.photo.components.PhotosTabContent
import ph.mart.healthapp.feature.progress.ui.progress.components.ScrollingTab
import ph.mart.healthapp.feature.progress.ui.progress.components.WeeklyRecapCard
import ph.mart.healthapp.feature.progress.ui.sleep.components.SleepTabContent
import ph.mart.healthapp.feature.progress.ui.weight.components.WeightTabContent

@Composable
fun ProgressScreen(scrollState: ScrollState = rememberScrollState(), viewModel: ProgressViewModel = koinViewModel()) {
    val uiState by viewModel.collectAsState()
    val state = rememberProgressScreenState()
    ProgressContent(uiState = uiState, state = state, scrollState = scrollState)
}

@Composable
private fun ProgressContent(uiState: ProgressUiState, state: ProgressScreenState, scrollState: ScrollState = rememberScrollState()) {
    Surface(color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 16.dp)) {
                Text(
                    text = "Progress",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 16.dp),
                )
                // Above the sub-tab toggle, not inside a tab: the recap spans nutrition, weight
                // and consistency at once. Null (nothing logged this week) omits it entirely
                // rather than rendering an all-zero card on day one.
                val recap = weeklyRecap(
                    dailyNutrition = uiState.dailyNutrition,
                    activeDays = uiState.activeDays,
                    weightEntries = uiState.weightEntries,
                    moodDays = uiState.moodDays,
                    targets = uiState.targets,
                    todayEpochDay = todayEpochDay(),
                )
                if (recap != null) {
                    WeeklyRecapCard(
                        recap = recap,
                        goal = uiState.goal,
                        unit = uiState.preferredUnit,
                        modifier = Modifier.padding(bottom = 12.dp),
                    )
                }
                SegmentedToggle(
                    options = ProgressTab.entries.map { it.label },
                    selectedIndex = ProgressTab.entries.indexOf(state.tab),
                    onSelect = { index -> state.tab = ProgressTab.entries[index] },
                )
                // Photos scrolls itself (LazyVerticalGrid) — nesting it in the verticalScroll
                // Column measures it with infinite height and throws. The other two tabs are
                // plain Columns and need the shared scroll.
                when (state.tab) {
                    ProgressTab.Photos -> PhotosTabContent(uiState, state)
                    ProgressTab.Weight -> ScrollingTab(scrollState) { WeightTabContent(uiState, state) }
                    ProgressTab.Nutrition -> ScrollingTab(scrollState) { NutritionTabContent(uiState, state) }
                    ProgressTab.Measurements -> ScrollingTab(scrollState) { MeasurementsTabContent(uiState, state) }
                    ProgressTab.Mood -> ScrollingTab(scrollState) { MoodTabContent(uiState, state) }
                    ProgressTab.Sleep -> ScrollingTab(scrollState) { SleepTabContent(uiState, state) }
                }
            }

            val selectedPhotos = uiState.photos.filter { it.id in state.selectedPhotoIds }
            if (selectedPhotos.size == 2) {
                val (older, newer) = selectedPhotos.sortedBy { it.dateEpochDay }
                PhotoComparisonScreen(
                    photoA = older,
                    photoB = newer,
                    unit = uiState.preferredUnit,
                    onClose = { state.selectedPhotoIds = emptyList() },
                )
            }

            if (state.activeMeasurementSheet) {
                AddMeasurementSheet(
                    trackedParts = uiState.measurements.keys,
                    preselectedPart = state.measurementSheetPart,
                    unit = uiState.preferredUnit,
                    onDismiss = state::closeMeasurementSheet,
                )
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun ProgressScreenPreview() {
    val today = todayEpochDay()
    val entries = listOf(
        WeightEntry(dateEpochDay = today - 9, weightKg = 78.0),
        WeightEntry(dateEpochDay = today - 4, weightKg = 77.5),
        WeightEntry(dateEpochDay = today, weightKg = 76.9),
    )
    AppTheme {
        ProgressContent(
            uiState = ProgressUiState(
                weightEntries = entries,
                goalWeightKg = 72.0,
                goal = Goal.Lose,
                preferredUnit = UnitSystem.Metric,
                dailyNutrition = listOf(1850, 2100, 0, 1720, 2340).mapIndexed { index, calories ->
                    DayNutrition(todayEpochDay() - 4 + index, calories, calories / 16, calories / 10, calories / 30)
                },
                activeDays = (todayEpochDay() - 4..todayEpochDay()).toSet(),
                moodDays = listOf(4 to 3, 5 to 4, 3 to 2, 4 to 4).mapIndexed { index, (mood, energy) ->
                    MoodDay(todayEpochDay() - 4 + index, mood, energy)
                },
                sleepNights = listOf(432, 401, 512, 388).mapIndexed { index, minutes ->
                    SleepNight(todayEpochDay() - 4 + index, minutes)
                },
                targets = DailyTargets(calories = 1941, proteinG = 146, carbsG = 194, fatG = 65, floor = 1500),
            ),
            state = ProgressScreenState(),
        )
    }
}
