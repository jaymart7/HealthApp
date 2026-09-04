package ph.mart.healthapp.feature.progress.ui.nutrition.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.data.food.DayNutrition
import ph.mart.healthapp.core.data.food.averages
import ph.mart.healthapp.core.data.profile.DailyTargets
import ph.mart.healthapp.core.data.todayEpochDay
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.progress.ui.progress.ProgressScreenState
import ph.mart.healthapp.feature.progress.ui.progress.ProgressUiState
import ph.mart.healthapp.feature.progress.ui.progress.Subject
import ph.mart.healthapp.feature.progress.ui.progress.components.ChartCard
import ph.mart.healthapp.feature.progress.ui.progress.components.FactChip
import ph.mart.healthapp.feature.progress.ui.progress.components.FactChipRow
import ph.mart.healthapp.feature.progress.ui.progress.components.HeroValue
import ph.mart.healthapp.feature.progress.ui.progress.components.LegendEntry

/**
 * The one dense series on this screen — a row per day for the last year, so its window is a plain
 * tail slice rather than the date filter every sparse subject needs.
 *
 * The averages are over **logged days only**: averaging the zero-filled gaps in would report a
 * number the user never ate, which is also why the chip says how many days it counted.
 * [NutritionAverageCard] carries the macro breakdown, so there are no stat rows under it — the
 * three macro colours are fixed app-wide and a second, colourless table of the same figures would
 * only invite them to disagree.
 */
@Composable
internal fun ColumnScope.NutritionDetailBody(uiState: ProgressUiState, state: ProgressScreenState) {
    val range = state.rangeFor(Subject.Nutrition)
    val days = range.days?.let { uiState.dailyNutrition.takeLast(it) } ?: uiState.dailyNutrition
    val averages = days.averages()
    val target = uiState.targets?.calories

    HeroValue(value = "${averages.calories}", caption = "kcal average")
    FactChipRow(
        chips = listOfNotNull(
            target?.let {
                FactChip(
                    when {
                        averages.calories < it -> "${it - averages.calories} kcal under target"
                        averages.calories > it -> "${averages.calories - it} kcal over target"
                        else -> "On target"
                    },
                )
            },
            FactChip("${averages.daysLogged} days with food logged"),
        ),
    )
    ChartCard(
        title = "Calories",
        range = range,
        onRangeChange = { state.setRange(Subject.Nutrition, it) },
        legend = listOfNotNull(
            LegendEntry("Daily intake", MaterialTheme.colorScheme.primary),
            target?.let { LegendEntry("Target $it kcal", MaterialTheme.colorScheme.onSurfaceVariant, dashed = true) },
        ),
    ) {
        NutritionTrendChart(days = days, targetCalories = target)
    }
    NutritionAverageCard(averages = averages, targets = uiState.targets)
}

@PreviewLightDark
@Composable
private fun NutritionDetailPreview() {
    val today = todayEpochDay()
    AppTheme {
        Surface {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                NutritionDetailBody(
                    uiState = ProgressUiState(
                        dailyNutrition = listOf(1850, 2100, 0, 1720, 2340).mapIndexed { index, calories ->
                            DayNutrition(today - 4 + index, calories, calories / 16, calories / 10, calories / 30)
                        },
                        targets = DailyTargets(
                            calories = 1941,
                            proteinG = 146,
                            carbsG = 194,
                            fatG = 65,
                            floor = 1500,
                        ),
                    ),
                    state = ProgressScreenState(),
                )
            }
        }
    }
}
