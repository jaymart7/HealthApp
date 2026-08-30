package ph.mart.healthapp.feature.progress.ui.weight.components

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import ph.mart.healthapp.core.data.profile.Goal
import ph.mart.healthapp.core.data.profile.TrendDirection
import ph.mart.healthapp.core.data.profile.UnitSystem
import ph.mart.healthapp.core.data.profile.goalRelativeTrend
import ph.mart.healthapp.core.data.profile.kgToDisplayUnit
import ph.mart.healthapp.core.data.profile.weightUnitLabel
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.core.designsystem.theme.tabularNums

/** Current / change / goal-remaining — goal-remaining cell omitted with the goal marker, same as
 * [WeightProgressChart]'s dashed line. Change color reuses the shared goal-relative trend logic,
 * never a fixed green/red mapping. */
@Composable
fun WeightStatRow(
    currentKg: Double,
    changeKg: Double,
    goal: Goal?,
    goalWeightKg: Double?,
    unit: UnitSystem,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        StatCell(label = "Current", value = "${formatKg(currentKg.kgToDisplayUnit(unit))} ${unit.weightUnitLabel()}")
        val trend = goalRelativeTrend(goal, changeKg)
        StatCell(
            label = "Change",
            value = "${if (changeKg > 0) "+" else ""}${formatKg(changeKg.kgToDisplayUnit(unit))} ${unit.weightUnitLabel()}",
            valueColor = when (trend) {
                TrendDirection.OnTrack -> MaterialTheme.colorScheme.primary
                TrendDirection.OffTrack -> MaterialTheme.colorScheme.error
                TrendDirection.Neutral -> MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
        if (goalWeightKg != null) {
            val remaining = abs(currentKg - goalWeightKg)
            StatCell(label = "Goal remaining", value = "${formatKg(remaining.kgToDisplayUnit(unit))} ${unit.weightUnitLabel()}")
        }
    }
}

/** Shared with [WeeklyRecapCard] — same screen, same feature, so it stays here rather than
 * being promoted to `:core:designsystem` (that rule is about components used across screens). */
@Composable
internal fun StatCell(label: String, value: String, valueColor: Color? = null) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.tabularNums,
            color = valueColor ?: MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Start,
        )
    }
}

internal fun formatKg(value: Double): String =
    if (value == value.toInt().toDouble()) value.toInt().toString() else "%.1f".format(value)

@PreviewLightDark
@Composable
private fun WeightStatRowPreview() {
    AppTheme {
        Surface {
            WeightStatRow(
                currentKg = 76.5,
                changeKg = -1.5,
                goal = Goal.Lose,
                goalWeightKg = 72.0,
                unit = UnitSystem.Metric,
                modifier = Modifier.fillMaxWidth().padding(16.dp),
            )
        }
    }
}
