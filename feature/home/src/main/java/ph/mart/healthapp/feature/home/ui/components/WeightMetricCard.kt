package ph.mart.healthapp.feature.home.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import ph.mart.healthapp.core.data.profile.Goal
import ph.mart.healthapp.core.data.profile.TREND_ARROW_DEADBAND_KG
import ph.mart.healthapp.core.data.profile.TrendDirection
import ph.mart.healthapp.core.data.profile.UnitSystem
import ph.mart.healthapp.core.data.profile.WeightTrendDisplay
import ph.mart.healthapp.core.data.profile.goalRelativeTrend
import ph.mart.healthapp.core.data.profile.kgToDisplayUnit
import ph.mart.healthapp.core.data.profile.weightUnitLabel
import ph.mart.healthapp.core.data.progress.GoalProjection
import ph.mart.healthapp.core.data.progress.PROJECTION_WINDOW_DAYS
import ph.mart.healthapp.core.designsystem.component.goalProjectionLine
import ph.mart.healthapp.core.designsystem.icon.AppIcons
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.home.R

/**
 * Current weight + the change against 7 days ago. The trend **colour** comes from the shared
 * `goalRelativeTrend()` in `:core:data` — identical to Progress's WeightStatRow, never a
 * green-for-loss default. The arrow is direction only, suppressed below
 * [TREND_ARROW_DEADBAND_KG] where the movement is too small to call. Arrow direction is the delta;
 * colour is the goal — the two are not the same question.
 *
 * [projection] adds the horizon the delta never gives: "vs 7d ago" says which way, not when you
 * arrive. Null (no target weight, a Maintain goal, or too little recent data to fit a rate) draws
 * nothing at all — the same call the three watch cards make by hiding rather than zeroing. The line
 * is `onSurfaceVariant` even when the trend points away from the goal: the delta above already
 * carries the verdict colour, and a red date would read as a second one. Its words come from
 * `:core:designsystem` so Progress can't word it differently.
 */
@Composable
fun WeightMetricCard(
    trend: WeightTrendDisplay,
    goal: Goal?,
    unit: UnitSystem,
    projection: GoalProjection?,
    wide: Boolean,
    modifier: Modifier = Modifier,
) {
    val direction = goalRelativeTrend(goal, trend.deltaKg)
    val color = when (direction) {
        TrendDirection.OnTrack -> MaterialTheme.colorScheme.primary
        TrendDirection.OffTrack -> MaterialTheme.colorScheme.error
        TrendDirection.Neutral -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val showArrow = trend.hasPrior && abs(trend.deltaKg) >= TREND_ARROW_DEADBAND_KG
    MetricCard(
        label = stringResource(R.string.home_weight_title),
        value = formatWeight(trend.currentKg.kgToDisplayUnit(unit)),
        unit = " " + unit.weightUnitLabel(),
        wide = wide,
        status = when (direction) {
            TrendDirection.OnTrack -> StatusMark.OnTrack
            TrendDirection.OffTrack -> StatusMark.OffTrack
            TrendDirection.Neutral -> StatusMark.None
        },
        modifier = modifier,
    ) {
        MetaText(
            text = if (trend.hasPrior) {
                stringResource(
                    R.string.home_weight_delta,
                    formatWeight(abs(trend.deltaKg).kgToDisplayUnit(unit)),
                    unit.weightUnitLabel(),
                )
            } else {
                stringResource(R.string.home_weight_no_prior)
            },
            sub = projection?.let {
                goalProjectionLine(
                    goalWeightLabel = stringResource(
                        R.string.home_weight_value,
                        formatWeight(it.goalWeightKg.kgToDisplayUnit(unit)),
                        unit.weightUnitLabel(),
                    ),
                    targetEpochDay = it.targetEpochDay,
                    reached = it.reached,
                    windowDays = PROJECTION_WINDOW_DAYS,
                )
            },
            color = color,
            leading = if (showArrow) {
                {
                    Icon(
                        imageVector = if (trend.deltaKg < 0) AppIcons.TrendDown else AppIcons.TrendUp,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(16.dp),
                    )
                }
            } else {
                null
            },
        )
    }
}

private fun formatWeight(value: Double): String =
    if (value == value.toInt().toDouble()) value.toInt().toString() else "%.1f".format(value)

@PreviewLightDark
@Composable
private fun WeightMetricCardPreview() {
    AppTheme {
        Surface {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(16.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    WeightMetricCard(
                        trend = WeightTrendDisplay(currentKg = 76.5, deltaKg = -0.6, hasPrior = true),
                        goal = Goal.Lose,
                        unit = UnitSystem.Metric,
                        projection = GoalProjection(72.0, -0.4, 20_760, reached = false),
                        wide = false,
                        modifier = Modifier.weight(1f),
                    )
                    // Trending away from the goal: still one neutral projection line, never a red date.
                    WeightMetricCard(
                        trend = WeightTrendDisplay(currentKg = 76.5, deltaKg = 0.4, hasPrior = true),
                        goal = Goal.Lose,
                        unit = UnitSystem.Metric,
                        projection = GoalProjection(72.0, 0.3, null, reached = false),
                        wide = false,
                        modifier = Modifier.weight(1f),
                    )
                }
                // Nothing to project from, and no prior week to compare against.
                WeightMetricCard(
                    trend = WeightTrendDisplay(currentKg = 76.5, deltaKg = 0.0, hasPrior = false),
                    goal = Goal.Lose,
                    unit = UnitSystem.Metric,
                    projection = null,
                    wide = true,
                )
            }
        }
    }
}
