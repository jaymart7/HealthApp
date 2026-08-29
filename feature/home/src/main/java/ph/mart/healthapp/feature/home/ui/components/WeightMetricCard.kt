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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import ph.mart.healthapp.core.data.profile.Goal
import ph.mart.healthapp.core.data.profile.TrendDirection
import ph.mart.healthapp.core.data.profile.TREND_ARROW_DEADBAND_KG
import ph.mart.healthapp.core.data.profile.UnitSystem
import ph.mart.healthapp.core.data.profile.WeightTrendDisplay
import ph.mart.healthapp.core.data.profile.goalRelativeTrend
import ph.mart.healthapp.core.data.profile.kgToDisplayUnit
import ph.mart.healthapp.core.data.profile.weightUnitLabel
import ph.mart.healthapp.core.designsystem.component.AppCard
import ph.mart.healthapp.core.designsystem.icon.AppIcons
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.core.designsystem.theme.tabularNums

/**
 * Current weight + the change against 7 days ago. The trend **colour** comes from the shared
 * `goalRelativeTrend()` in `:core:data` — identical to Progress's WeightStatRow, never a
 * green-for-loss default. The arrow is direction only, suppressed below
 * [TREND_ARROW_DEADBAND_KG] where the movement is too small to call.
 */
@Composable
fun WeightMetricCard(
    trend: WeightTrendDisplay,
    goal: Goal?,
    unit: UnitSystem,
    modifier: Modifier = Modifier,
) {
    AppCard(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Weight",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "${formatWeight(trend.currentKg.kgToDisplayUnit(unit))} ${unit.weightUnitLabel()}",
                    style = MaterialTheme.typography.headlineSmall.tabularNums,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            val color = when (goalRelativeTrend(goal, trend.deltaKg)) {
                TrendDirection.OnTrack -> MaterialTheme.colorScheme.primary
                TrendDirection.OffTrack -> MaterialTheme.colorScheme.error
                TrendDirection.Neutral -> MaterialTheme.colorScheme.onSurfaceVariant
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (trend.hasPrior && abs(trend.deltaKg) >= TREND_ARROW_DEADBAND_KG) {
                    Icon(
                        imageVector = if (trend.deltaKg < 0) AppIcons.TrendDown else AppIcons.TrendUp,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(16.dp),
                    )
                }
                Text(
                    text = if (trend.hasPrior) {
                        "${formatWeight(abs(trend.deltaKg).kgToDisplayUnit(unit))} ${unit.weightUnitLabel()} vs 7d ago"
                    } else {
                        "No prior data"
                    },
                    style = MaterialTheme.typography.bodyMedium.tabularNums,
                    color = color,
                )
            }
        }
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
                WeightMetricCard(
                    trend = WeightTrendDisplay(currentKg = 76.5, deltaKg = -0.6, hasPrior = true),
                    goal = Goal.Lose,
                    unit = UnitSystem.Metric,
                )
                WeightMetricCard(
                    trend = WeightTrendDisplay(currentKg = 76.5, deltaKg = 0.0, hasPrior = false),
                    goal = Goal.Lose,
                    unit = UnitSystem.Metric,
                )
            }
        }
    }
}
