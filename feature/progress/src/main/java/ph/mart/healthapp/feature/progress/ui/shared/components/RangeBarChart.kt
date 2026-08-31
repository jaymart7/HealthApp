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
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.designsystem.theme.AppTheme

/** One day's span: [low] to [high], both in whatever unit the caller measures in. */
data class RangeBar(val dateEpochDay: Long, val low: Int, val high: Int)

/**
 * A day per slot, each drawn as a floating bar from its low to its high — one [Canvas], no
 * charting library, gridlines in `outlineVariant`, slots taken from the *window* rather than the
 * list so a stretch with no data stays a visible gap.
 *
 * Shared by the Heart and Blood pressure tabs, which is why it lives in `ui/shared/`.
 *
 * The one departure from every other chart in this app: these bars are **not zero-based**, and the
 * y-axis is ranged over the window's own values instead. Nobody's heart visits 0-45 bpm and nobody's
 * diastolic visits 0-50, so a zero-based axis would spend most of its height on a range that can
 * never carry a bar and squash the 20 points that actually differ into a few pixels. Both ends of
 * every bar are named by the stat row beneath it, which is why there is no legend.
 *
 * [axisPad] is the breathing room above and below the window's own values, so no bar sits flat on
 * an edge.
 */
@Composable
fun RangeBarChart(
    bars: List<RangeBar>,
    fromEpochDay: Long,
    toEpochDay: Long,
    axisPad: Int,
    modifier: Modifier = Modifier,
) {
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val barColor = MaterialTheme.colorScheme.primary

    Canvas(modifier = modifier.fillMaxWidth().height(200.dp)) {
        val slots = (toEpochDay - fromEpochDay + 1).toInt()
        if (slots <= 0 || bars.isEmpty()) return@Canvas

        val low = (bars.minOf { it.low } - axisPad).coerceAtLeast(0)
        val high = bars.maxOf { it.high } + axisPad
        val range = (high - low).coerceAtLeast(1).toFloat()
        fun yFor(value: Int): Float = size.height - ((value - low) / range * size.height)

        repeat(4) { row ->
            val y = size.height / 3f * row
            drawLine(gridColor, start = Offset(0f, y), end = Offset(size.width, y), strokeWidth = 1.dp.toPx())
        }

        // At a year's range each slot is well under a pixel wide, so the gap is only ever taken
        // out of a slot that can spare it — the same ceiling the other charts accept.
        val slot = size.width / slots
        val barWidth = (slot - 1.dp.toPx()).coerceAtLeast(slot * 0.6f)
        // A bar whose high equals its low is one reading, not nothing, so it still draws.
        val minBarHeight = 2.dp.toPx()
        bars.forEach { bar ->
            val index = (bar.dateEpochDay - fromEpochDay).toInt()
            if (index !in 0 until slots || bar.high <= 0) return@forEach
            val top = yFor(bar.high)
            val bottom = yFor(bar.low)
            drawRect(
                color = barColor,
                topLeft = Offset(index * slot, top),
                size = Size(barWidth, (bottom - top).coerceAtLeast(minBarHeight)),
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun RangeBarChartPreview() {
    val today = 20_000L
    // Two missing days in the middle: the chart says so rather than closing the gap.
    val bars = listOf(52 to 68, 55 to 71, 49 to 66, 58 to 74, 53 to 69)
        .mapIndexed { index, (low, high) ->
            RangeBar(today - 6 + if (index < 2) index else index + 2, low, high)
        }
    AppTheme {
        Surface {
            Column(modifier = Modifier.padding(16.dp)) {
                RangeBarChart(bars = bars, fromEpochDay = today - 6, toEpochDay = today, axisPad = 5)
            }
        }
    }
}
