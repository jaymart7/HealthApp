package ph.mart.healthapp.feature.progress.ui.nutrition.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.data.food.DayNutrition
import ph.mart.healthapp.core.designsystem.theme.AppTheme

/**
 * Daily calories as bars against a dashed target line — one [Canvas], no charting library, same
 * construction as [WeightProgressChart] (gridlines in `outlineVariant`, the goal marker dashed in
 * `tertiary`). Bars are zero-based, unlike the weight chart's auto-ranged line: a calorie bar that
 * didn't start at zero would misread badly.
 *
 * Unlogged days draw nothing, so a gap stays visibly a gap.
 */
@Composable
fun NutritionTrendChart(
    days: List<DayNutrition>,
    targetCalories: Int?,
    modifier: Modifier = Modifier,
) {
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val barColor = MaterialTheme.colorScheme.primary
    val targetColor = MaterialTheme.colorScheme.tertiary

    Canvas(modifier = modifier.fillMaxWidth().height(200.dp)) {
        if (days.isEmpty()) return@Canvas

        // 8% headroom above whichever is taller. Without it a target nobody reached sits exactly on
        // the top edge and reads as a border rather than a line.
        val peak = days.maxOf { it.calories }.coerceAtLeast(targetCalories ?: 0).coerceAtLeast(1)
        val maxValue = peak * 1.08f
        fun yFor(value: Int): Float = size.height - (value / maxValue * size.height)

        repeat(4) { row ->
            val y = size.height / 3f * row
            drawLine(gridColor, start = Offset(0f, y), end = Offset(size.width, y), strokeWidth = 1.dp.toPx())
        }

        // At a year's range each slot is well under a pixel wide, so the gap is only ever taken
        // out of a slot that can spare it.
        val slot = size.width / days.size
        val barWidth = (slot - 1.dp.toPx()).coerceAtLeast(slot * 0.6f)
        days.forEachIndexed { index, day ->
            if (day.calories <= 0) return@forEachIndexed
            val top = yFor(day.calories)
            drawRect(
                color = barColor,
                topLeft = Offset(index * slot, top),
                size = Size(barWidth, size.height - top),
            )
        }

        targetCalories?.takeIf { it > 0 }?.let { target ->
            val y = yFor(target)
            drawLine(
                color = targetColor,
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1.5.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f)),
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun NutritionTrendChartPreview() {
    val days = listOf(1850, 2100, 0, 1720, 2340, 1980, 2050).mapIndexed { index, calories ->
        DayNutrition(
            dateEpochDay = index.toLong(),
            calories = calories,
            proteinG = calories / 16,
            carbsG = calories / 10,
            fatG = calories / 30,
        )
    }
    AppTheme {
        Surface {
            Column {
                NutritionTrendChart(days = days, targetCalories = 1941)
            }
        }
    }
}
