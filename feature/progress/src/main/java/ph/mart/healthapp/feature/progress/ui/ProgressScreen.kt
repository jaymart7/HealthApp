package ph.mart.healthapp.feature.progress.ui

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import ph.mart.healthapp.core.data.food.averages
import ph.mart.healthapp.core.data.health.SleepNight
import ph.mart.healthapp.core.data.health.formatDuration
import ph.mart.healthapp.core.data.health.inRange
import ph.mart.healthapp.core.data.health.sleepAverages
import ph.mart.healthapp.core.data.mood.MOOD_SCALE
import ph.mart.healthapp.core.data.mood.MoodDay
import ph.mart.healthapp.core.data.mood.inRange
import ph.mart.healthapp.core.data.mood.moodAverages
import ph.mart.healthapp.core.data.profile.DailyTargets
import ph.mart.healthapp.core.data.profile.Goal
import ph.mart.healthapp.core.data.profile.UnitSystem
import ph.mart.healthapp.core.data.progress.ChartRange
import ph.mart.healthapp.core.data.progress.MeasurementPart
import ph.mart.healthapp.core.data.progress.WeightEntry
import ph.mart.healthapp.core.data.progress.inRange
import ph.mart.healthapp.core.data.progress.withMovingAverage
import ph.mart.healthapp.core.designsystem.component.DockedFabContentPadding
import ph.mart.healthapp.core.designsystem.component.FullScreenState
import ph.mart.healthapp.core.designsystem.component.MascotAvatar
import ph.mart.healthapp.core.designsystem.component.MascotState
import ph.mart.healthapp.core.designsystem.component.PrimaryButton
import ph.mart.healthapp.core.designsystem.component.SegmentedToggle
import ph.mart.healthapp.core.designsystem.component.todayEpochDay
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.progress.ui.components.GoalProjectionCard
import ph.mart.healthapp.feature.progress.ui.components.MeasurementRow
import ph.mart.healthapp.feature.progress.ui.components.NutritionAverageCard
import ph.mart.healthapp.feature.progress.ui.components.MoodTrendChart
import ph.mart.healthapp.feature.progress.ui.components.NutritionTrendChart
import ph.mart.healthapp.feature.progress.ui.components.PhotoComparisonScreen
import ph.mart.healthapp.feature.progress.ui.components.ProgressPhotoGrid
import ph.mart.healthapp.feature.progress.ui.components.SleepTrendChart
import ph.mart.healthapp.feature.progress.ui.components.WeightProgressChart
import ph.mart.healthapp.feature.progress.ui.components.WeeklyRecapCard
import ph.mart.healthapp.feature.progress.ui.components.StatCell
import ph.mart.healthapp.feature.progress.ui.components.WeightStatRow

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

@Composable
private fun ScrollingTab(scrollState: ScrollState, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(top = 16.dp, bottom = DockedFabContentPadding),
        content = content,
    )
}

@Composable
private fun WeightTabContent(uiState: ProgressUiState, state: ProgressScreenState) {
    if (uiState.weightEntries.isEmpty()) {
        FullScreenState(
            icon = { MascotAvatar(state = MascotState.Sleepy, size = 64.dp) },
            heading = "No weight logged yet",
            body = "Log your weight from the FAB to start tracking your trend.",
        )
        return
    }
    Column {
        SegmentedToggle(
            options = ChartRange.entries.map { it.label },
            selectedIndex = ChartRange.entries.indexOf(state.range),
            onSelect = { index -> state.range = ChartRange.entries[index] },
        )
        val filtered = uiState.weightEntries.inRange(state.range)
        val points = filtered.withMovingAverage()
        WeightProgressChart(
            points = points,
            goalWeightKg = uiState.goalWeightKg,
            unit = uiState.preferredUnit,
            modifier = Modifier.padding(top = 16.dp),
        )
        val sorted = uiState.weightEntries.sortedBy { it.dateEpochDay }
        val current = sorted.last().weightKg
        val prior = if (sorted.size >= 2) sorted[sorted.size - 2].weightKg else current
        WeightStatRow(
            currentKg = current,
            changeKg = current - prior,
            goal = uiState.goal,
            goalWeightKg = uiState.goalWeightKg,
            unit = uiState.preferredUnit,
            modifier = Modifier.padding(top = 16.dp),
        )
        // Under the row that says how far there is to go. Null (no goal weight, a Maintain
        // goal, or too little recent data to fit a rate) omits the card, same as the recap.
        goalProjection(
            weightEntries = uiState.weightEntries,
            goalWeightKg = uiState.goalWeightKg,
            goal = uiState.goal,
            todayEpochDay = todayEpochDay(),
        )?.let { projection ->
            GoalProjectionCard(
                projection = projection,
                unit = uiState.preferredUnit,
                modifier = Modifier.padding(top = 12.dp),
            )
        }
    }
}

@Composable
private fun NutritionTabContent(uiState: ProgressUiState, state: ProgressScreenState) {
    if (uiState.dailyNutrition.none { it.isLogged }) {
        FullScreenState(
            icon = { MascotAvatar(state = MascotState.Sleepy, size = 64.dp) },
            heading = "Nothing logged yet",
            body = "Log a few meals in the diary and your calorie trend shows up here.",
        )
        return
    }
    Column {
        SegmentedToggle(
            options = ChartRange.entries.map { it.label },
            selectedIndex = ChartRange.entries.indexOf(state.range),
            onSelect = { index -> state.range = ChartRange.entries[index] },
        )
        // The series is already dense and ends today, so the range is a plain tail slice — no
        // date math up here.
        val days = state.range.days?.let { uiState.dailyNutrition.takeLast(it) } ?: uiState.dailyNutrition
        NutritionTrendChart(
            days = days,
            targetCalories = uiState.targets?.calories,
            modifier = Modifier.padding(top = 16.dp),
        )
        NutritionAverageCard(
            averages = days.averages(),
            targets = uiState.targets,
            modifier = Modifier.padding(top = 16.dp),
        )
    }
}

@Composable
private fun MoodTabContent(uiState: ProgressUiState, state: ProgressScreenState) {
    if (uiState.moodDays.isEmpty()) {
        FullScreenState(
            icon = { MascotAvatar(state = MascotState.Sleepy, size = 64.dp) },
            heading = "No mood logged yet",
            body = "Tap how you're feeling on the Home screen and it shows up here.",
        )
        return
    }
    Column {
        SegmentedToggle(
            options = ChartRange.entries.map { it.label },
            selectedIndex = ChartRange.entries.indexOf(state.range),
            onSelect = { index -> state.range = ChartRange.entries[index] },
        )
        // Windowed by date rather than sliced off the end: the series is sparse, so the chart
        // needs the window's bounds to know where the gaps are.
        val today = todayEpochDay()
        val from = today - (state.range.days ?: ChartRange.OneYear.days!!)
        val days = uiState.moodDays.inRange(state.range, today)
        MoodTrendChart(
            days = days,
            fromEpochDay = from,
            toEpochDay = today,
            modifier = Modifier.padding(top = 16.dp),
        )
        val averages = days.moodAverages()
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            StatCell(label = "Avg mood", value = averageLabel(averages.mood))
            StatCell(label = "Avg energy", value = averageLabel(averages.energy))
            StatCell(label = "Days logged", value = "${averages.daysLogged}")
        }
    }
}

@Composable
private fun SleepTabContent(uiState: ProgressUiState, state: ProgressScreenState) {
    if (uiState.sleepNights.isEmpty()) {
        FullScreenState(
            icon = { MascotAvatar(state = MascotState.Sleepy, size = 64.dp) },
            heading = "No sleep data yet",
            body = "Connect Google Health in Profile and your nights show up here.",
        )
        return
    }
    Column {
        SegmentedToggle(
            options = ChartRange.entries.map { it.label },
            selectedIndex = ChartRange.entries.indexOf(state.range),
            onSelect = { index -> state.range = ChartRange.entries[index] },
        )
        // Windowed by date rather than sliced off the end, for the same reason the mood series is:
        // nights are sparse, so the chart needs the window's bounds to know where the gaps are.
        val today = todayEpochDay()
        val from = today - (state.range.days ?: ChartRange.OneYear.days!!)
        val nights = uiState.sleepNights.inRange(state.range, today)
        SleepTrendChart(
            nights = nights,
            fromEpochDay = from,
            toEpochDay = today,
            modifier = Modifier.padding(top = 16.dp),
        )
        val averages = nights.sleepAverages()
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            StatCell(label = "Avg night", value = durationLabel(averages.averageMinutes))
            StatCell(label = "Longest", value = durationLabel(averages.longestMinutes))
            StatCell(label = "Nights", value = "${averages.nights}")
        }
    }
}

private fun durationLabel(minutes: Int?): String = minutes?.let(::formatDuration) ?: "—"

private fun averageLabel(value: Double?): String =
    value?.let { "%.1f / ${MOOD_SCALE.last}".format(it) } ?: "—"

@Composable
private fun PhotosTabContent(uiState: ProgressUiState, state: ProgressScreenState) {
    if (uiState.photos.isEmpty()) {
        FullScreenState(
            icon = { MascotAvatar(state = MascotState.Sleepy, size = 64.dp) },
            heading = "No progress photos yet",
            body = "Add your first photo from the FAB to start tracking changes over time.",
        )
        return
    }
    ProgressPhotoGrid(
        photos = uiState.photos,
        selectedIds = state.selectedPhotoIds,
        onToggleSelect = state::togglePhotoSelection,
        modifier = Modifier.fillMaxSize(),
    )
}

@Composable
private fun MeasurementsTabContent(uiState: ProgressUiState, state: ProgressScreenState) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        MeasurementPart.entries.filter { it in uiState.measurements }.forEach { part ->
            MeasurementRow(
                name = part.name,
                historyCm = uiState.measurements[part].orEmpty().sortedBy { it.dateEpochDay }.map { it.valueCm },
                unit = uiState.preferredUnit,
                onTap = { state.openMeasurementSheet(part) },
            )
        }
        PrimaryButton(
            label = "+ Add measurement",
            onClick = { state.openMeasurementSheet(null) },
            modifier = Modifier.padding(top = 12.dp),
        )
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
