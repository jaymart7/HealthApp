package ph.mart.healthapp.feature.progress.ui.shared.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.designsystem.theme.AppTheme

/** One day's figure, in whatever unit the caller measures in. */
data class DayBar(val dateEpochDay: Long, val value: Int)

/**
 * A day per slot, each drawn as a zero-based bar — one [Canvas], no charting library, gridlines in
 * `outlineVariant`, slots taken from the *window* rather than the list so a stretch with no data
 * stays a visible gap.
 *
 * Shared by the Sleep, Fasting and Activity tabs, which is why it lives in `ui/shared/` beside
 * [RangeBarChart] — the floating-bar chart Heart and Blood pressure draw, which is deliberately
 * *not* zero-based. Mood, Nutrition and Supplements keep their own canvases: two series with a
 * legend, a target line over a dense series, and percentages respectively.
 *
 * [minAxisValue] is the height the y-axis never shrinks below — a full night, a full day — so a run
 * of small figures reads as small rather than filling the canvas the way an auto-ranged axis would
 * let it. [goalValue] adds the dashed line a target-bearing series is judged against, drawn last so
 * it reads over the bars it judges.
 */
@Composable
fun DayBarChart(
    bars: List<DayBar>,
    fromEpochDay: Long,
    toEpochDay: Long,
    minAxisValue: Int = 0,
    goalValue: Int? = null,
    modifier: Modifier = Modifier,
) {
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val barColor = MaterialTheme.colorScheme.primary
    val goalColor = MaterialTheme.colorScheme.onSurfaceVariant

    Canvas(modifier = modifier.fillMaxWidth().height(200.dp)) {
        val slots = (toEpochDay - fromEpochDay + 1).toInt()
        if (slots <= 0) return@Canvas

        // Coerced to at least 1: a burn series can legitimately be all zeros, which a fixed floor
        // like the sleep chart's 480 never allowed.
        val maxValue = maxOf(
            bars.maxOfOrNull { it.value } ?: 0,
            minAxisValue,
            goalValue ?: 0,
        ).coerceAtLeast(1).toFloat()
        fun yFor(value: Int): Float = size.height - (value / maxValue * size.height)

        repeat(4) { row ->
            val y = size.height / 3f * row
            drawLine(gridColor, start = Offset(0f, y), end = Offset(size.width, y), strokeWidth = 1.dp.toPx())
        }

        // At a year's range each slot is well under a pixel wide, so the gap is only ever taken
        // out of a slot that can spare it — the same ceiling every other chart here accepts.
        val slot = size.width / slots
        val barWidth = (slot - 1.dp.toPx()).coerceAtLeast(slot * 0.6f)
        bars.forEach { bar ->
            val index = (bar.dateEpochDay - fromEpochDay).toInt()
            if (index !in 0 until slots || bar.value <= 0) return@forEach
            val top = yFor(bar.value)
            drawRect(
                color = barColor,
                topLeft = Offset(index * slot, top),
                size = Size(barWidth, size.height - top),
            )
        }

        if (goalValue != null) {
            val goalY = yFor(goalValue)
            drawLine(
                color = goalColor,
                start = Offset(0f, goalY),
                end = Offset(size.width, goalY),
                strokeWidth = 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx())),
            )
        }
    }
}

/** Two missing days in the middle: the chart says so rather than closing the gap. */
@PreviewLightDark
@Composable
private fun DayBarChartPreview() {
    val today = 20_000L
    val bars = listOf(432, 401, 512, 388, 455)
        .mapIndexed { index, minutes -> DayBar(today - 6 + if (index < 2) index else index + 2, minutes) }
    AppTheme {
        Surface {
            Column(modifier = Modifier.padding(16.dp)) {
                DayBarChart(bars = bars, fromEpochDay = today - 6, toEpochDay = today, minAxisValue = 480)
            }
        }
    }
}

/** The same series against a target — the shape Fasting and the steps chart draw. */
@PreviewLightDark
@Composable
private fun DayBarChartGoalPreview() {
    val today = 20_000L
    val bars = listOf(8_200, 11_400, 6_050, 12_900, 9_100)
        .mapIndexed { index, steps -> DayBar(today - 4 + index, steps) }
    AppTheme {
        Surface {
            Column(modifier = Modifier.padding(16.dp)) {
                DayBarChart(
                    bars = bars,
                    fromEpochDay = today - 4,
                    toEpochDay = today,
                    minAxisValue = 10_000,
                    goalValue = 10_000,
                )
            }
        }
    }
}
