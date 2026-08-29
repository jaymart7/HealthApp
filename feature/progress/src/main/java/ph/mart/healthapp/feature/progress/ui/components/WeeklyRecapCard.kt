package ph.mart.healthapp.feature.progress.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import ph.mart.healthapp.core.data.food.NutritionAverages
import ph.mart.healthapp.core.data.profile.DailyTargets
import ph.mart.healthapp.core.data.profile.Goal
import ph.mart.healthapp.core.data.profile.TREND_ARROW_DEADBAND_KG
import ph.mart.healthapp.core.data.profile.TrendDirection
import ph.mart.healthapp.core.data.profile.UnitSystem
import ph.mart.healthapp.core.data.profile.WeightTrendDisplay
import ph.mart.healthapp.core.data.profile.goalRelativeTrend
import ph.mart.healthapp.core.data.profile.kgToDisplayUnit
import ph.mart.healthapp.core.data.profile.weightUnitLabel
import ph.mart.healthapp.core.designsystem.component.AppCard
import ph.mart.healthapp.core.designsystem.component.formatWeekday
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.progress.ui.BestDay
import ph.mart.healthapp.feature.progress.ui.RECAP_WINDOW_DAYS
import ph.mart.healthapp.feature.progress.ui.WeeklyRecap

/**
 * The rolling week at a glance, above the sub-tab toggle — it spans nutrition, weight and
 * consistency, so it belongs to no single tab. Every number is derived in
 * [ph.mart.healthapp.feature.progress.ui.weeklyRecap]; this only formats.
 *
 * The weight colour comes from the shared [goalRelativeTrend], never a green-for-loss default,
 * and reads neutral below [TREND_ARROW_DEADBAND_KG] where the movement is too small to call.
 */
@Composable
fun WeeklyRecapCard(
    recap: WeeklyRecap,
    goal: Goal?,
    unit: UnitSystem,
    modifier: Modifier = Modifier,
) {
    AppCard(modifier = modifier) {
        Text(
            text = "Last 7 days",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 12.dp),
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            StatCell(label = "Days logged", value = "${recap.daysLogged}/$RECAP_WINDOW_DAYS")
            StatCell(
                label = "Avg calories",
                value = recap.targets?.let { "${recap.averages.calories} / ${it.calories}" }
                    ?: "${recap.averages.calories}",
            )
            WeightCell(trend = recap.weightTrend, goal = goal, unit = unit)
        }
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(top = 12.dp),
        ) {
            recap.bestDay?.let { Note("Best day — ${formatWeekday(it.dateEpochDay)}, ${it.calories} kcal") }
            // Two denominators in one card: the four-domain day count and the food-only average.
            // Spelled out when they differ, the same way NutritionAverageCard does.
            if (recap.averages.daysLogged != recap.daysLogged) {
                val days = recap.averages.daysLogged
                Note("Calories averaged over $days ${if (days == 1) "day" else "days"} with food logged.")
            }
            if (recap.weightTrend?.hasPrior != true) {
                Note("No weight change to compare yet.")
            }
        }
    }
}

@Composable
private fun WeightCell(trend: WeightTrendDisplay?, goal: Goal?, unit: UnitSystem) {
    if (trend == null || !trend.hasPrior) {
        StatCell(label = "Weight", value = "—")
        return
    }
    val delta = trend.deltaKg
    val neutral = MaterialTheme.colorScheme.onSurfaceVariant
    StatCell(
        label = "Weight",
        value = "${if (delta > 0) "+" else ""}${formatKg(delta.kgToDisplayUnit(unit))} ${unit.weightUnitLabel()}",
        valueColor = if (abs(delta) < TREND_ARROW_DEADBAND_KG) {
            neutral
        } else {
            when (goalRelativeTrend(goal, delta)) {
                TrendDirection.OnTrack -> MaterialTheme.colorScheme.primary
                TrendDirection.OffTrack -> MaterialTheme.colorScheme.error
                TrendDirection.Neutral -> neutral
            }
        },
    )
}

/** Shared with [GoalProjectionCard] — same screen, same feature, so it stays here rather than
 * being promoted to `:core:designsystem` (that rule is about components used across screens). */
@Composable
internal fun Note(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

private val PREVIEW_TARGETS =
    DailyTargets(calories = 2000, proteinG = 150, carbsG = 200, fatG = 67, floor = 1500)

@PreviewLightDark
@Composable
private fun WeeklyRecapCardPreview() {
    AppTheme {
        Surface {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(16.dp),
            ) {
                WeeklyRecapCard(
                    recap = WeeklyRecap(
                        daysLogged = 7,
                        averages = NutritionAverages(1940, 141, 196, 68, daysLogged = 7),
                        targets = PREVIEW_TARGETS,
                        weightTrend = WeightTrendDisplay(currentKg = 76.0, deltaKg = -0.6, hasPrior = true),
                        bestDay = BestDay(dateEpochDay = 20_690, calories = 1985),
                    ),
                    goal = Goal.Lose,
                    unit = UnitSystem.Metric,
                )
                // Sparse week: water-only days pad the count, and nothing has been weighed.
                WeeklyRecapCard(
                    recap = WeeklyRecap(
                        daysLogged = 4,
                        averages = NutritionAverages(1720, 118, 170, 61, daysLogged = 2),
                        targets = PREVIEW_TARGETS,
                        weightTrend = null,
                        bestDay = BestDay(dateEpochDay = 20_688, calories = 1880),
                    ),
                    goal = Goal.Lose,
                    unit = UnitSystem.Metric,
                )
            }
        }
    }
}
