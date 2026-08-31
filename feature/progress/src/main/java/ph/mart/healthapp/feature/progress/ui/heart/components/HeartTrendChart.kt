package ph.mart.healthapp.feature.progress.ui.heart.components

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
import ph.mart.healthapp.core.data.health.HeartDay
import ph.mart.healthapp.core.designsystem.theme.AppTheme

/** Breathing room above and below the window's own readings, so no bar sits flat on an edge. */
private const val AXIS_PAD_BPM = 5

/**
 * Each day as a floating bar spanning that day's lowest reading up to its average — one [Canvas],
 * no charting library, same construction as [SleepTrendChart] (gridlines in `outlineVariant`,
 * slots taken from the *window* rather than the list, so a week the watch was off charge stays a
 * visible gap).
 *
 * The one departure from every other chart in this app: these bars are **not zero-based**, and the
 * y-axis is ranged over the window's own readings instead. Nobody's heart visits 0-45 bpm, so a
 * zero-based axis would spend most of its height on a range that can never carry a bar and squash
 * the 20 beats that actually differ into a few pixels. Both ends of a bar are labelled by the stat
 * row beneath it, which is why there is no legend.
 */
@Composable
fun HeartTrendChart(
    days: List<HeartDay>,
    fromEpochDay: Long,
    toEpochDay: Long,
    modifier: Modifier = Modifier,
) {
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val barColor = MaterialTheme.colorScheme.primary

    Canvas(modifier = modifier.fillMaxWidth().height(200.dp)) {
        val slots = (toEpochDay - fromEpochDay + 1).toInt()
        if (slots <= 0 || days.isEmpty()) return@Canvas

        val low = (days.minOf { it.minBpm } - AXIS_PAD_BPM).coerceAtLeast(0)
        val high = days.maxOf { it.averageBpm } + AXIS_PAD_BPM
        val range = (high - low).coerceAtLeast(1).toFloat()
        fun yFor(bpm: Int): Float = size.height - ((bpm - low) / range * size.height)

        repeat(4) { row ->
            val y = size.height / 3f * row
            drawLine(gridColor, start = Offset(0f, y), end = Offset(size.width, y), strokeWidth = 1.dp.toPx())
        }

        // At a year's range each slot is well under a pixel wide, so the gap is only ever taken
        // out of a slot that can spare it — the same ceiling the other charts accept.
        val slot = size.width / slots
        val barWidth = (slot - 1.dp.toPx()).coerceAtLeast(slot * 0.6f)
        // A day whose average equals its lowest is one reading, not nothing, so it still draws.
        val minBarHeight = 2.dp.toPx()
        days.forEach { day ->
            val index = (day.dateEpochDay - fromEpochDay).toInt()
            if (index !in 0 until slots || day.averageBpm <= 0) return@forEach
            val top = yFor(day.averageBpm)
            val bottom = yFor(day.minBpm)
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
private fun HeartTrendChartPreview() {
    val today = 20_000L
    // Two missing days in the middle: the watch was off charge, and the chart says so.
    val days = listOf(68 to 52, 71 to 55, 0 to 0, 0 to 0, 66 to 49, 74 to 58, 69 to 53)
        .mapIndexed { index, (average, min) -> HeartDay(today - 6 + index, average, min) }
        .filter { it.averageBpm > 0 }
    AppTheme {
        Surface {
            Column(modifier = Modifier.padding(16.dp)) {
                HeartTrendChart(days = days, fromEpochDay = today - 6, toEpochDay = today)
            }
        }
    }
}
