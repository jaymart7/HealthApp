package ph.mart.healthapp.feature.progress.ui.nutrition.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.data.food.DayNutrition
import ph.mart.healthapp.core.data.food.averages
import ph.mart.healthapp.core.data.profile.DailyTargets
import ph.mart.healthapp.core.data.progress.ChartRange
import ph.mart.healthapp.core.designsystem.component.FullScreenState
import ph.mart.healthapp.core.designsystem.component.MascotAvatar
import ph.mart.healthapp.core.designsystem.component.MascotState
import ph.mart.healthapp.core.designsystem.component.SegmentedToggle
import ph.mart.healthapp.core.data.todayEpochDay
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.progress.ui.progress.ProgressScreenState
import ph.mart.healthapp.feature.progress.ui.progress.ProgressUiState

@Composable
internal fun NutritionTabContent(uiState: ProgressUiState, state: ProgressScreenState) {
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

@PreviewLightDark
@Composable
private fun NutritionTabPreview() {
    val today = todayEpochDay()
    AppTheme {
        NutritionTabContent(
            uiState = ProgressUiState(
                dailyNutrition = listOf(1850, 2100, 0, 1720, 2340).mapIndexed { index, calories ->
                    DayNutrition(today - 4 + index, calories, calories / 16, calories / 10, calories / 30)
                },
                targets = DailyTargets(calories = 1941, proteinG = 146, carbsG = 194, fatG = 65, floor = 1500),
            ),
            state = ProgressScreenState(),
        )
    }
}

@PreviewLightDark
@Composable
private fun NutritionTabEmptyPreview() {
    AppTheme { NutritionTabContent(uiState = ProgressUiState(), state = ProgressScreenState()) }
}
