package ph.mart.healthapp.feature.progress.ui.components

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
import ph.mart.healthapp.core.data.health.SleepNight
import ph.mart.healthapp.core.designsystem.theme.AppTheme

/** A full night. The y-axis never shrinks below it, so a run of four-hour nights reads as short
 * rather than filling the canvas the way an auto-ranged axis would let it. */
private const val FULL_NIGHT_MINUTES = 480

/**
 * Nightly sleep duration as bars over a fixed date window — one [Canvas], no charting library,
 * same construction as [MoodTrendChart] (gridlines in `outlineVariant`, zero-based bars). Nights
 * Google Health never sent draw nothing, so a gap stays visibly a gap.
 *
 * Slots come from the *window*, not from the list, because the series is sparse: a night's
 * position on the x-axis is its date, which is what keeps a week the watch was off charge
 * looking like a week. One series, so no legend — unlike the mood chart's two.
 */
@Composable
fun SleepTrendChart(
    nights: List<SleepNight>,
    fromEpochDay: Long,
    toEpochDay: Long,
    modifier: Modifier = Modifier,
) {
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val barColor = MaterialTheme.colorScheme.primary

    Canvas(modifier = modifier.fillMaxWidth().height(200.dp)) {
        val slots = (toEpochDay - fromEpochDay + 1).toInt()
        if (slots <= 0) return@Canvas

        val maxValue = maxOf(nights.maxOfOrNull { it.minutesAsleep } ?: 0, FULL_NIGHT_MINUTES).toFloat()
        fun yFor(value: Int): Float = size.height - (value / maxValue * size.height)

        repeat(4) { row ->
            val y = size.height / 3f * row
            drawLine(gridColor, start = Offset(0f, y), end = Offset(size.width, y), strokeWidth = 1.dp.toPx())
        }

        // At a year's range each slot is well under a pixel wide, so the gap is only ever taken
        // out of a slot that can spare it — the same ceiling NutritionTrendChart accepts.
        val slot = size.width / slots
        val barWidth = (slot - 1.dp.toPx()).coerceAtLeast(slot * 0.6f)
        nights.forEach { night ->
            val index = (night.dateEpochDay - fromEpochDay).toInt()
            if (index !in 0 until slots || night.minutesAsleep <= 0) return@forEach
            val top = yFor(night.minutesAsleep)
            drawRect(
                color = barColor,
                topLeft = Offset(index * slot, top),
                size = Size(barWidth, size.height - top),
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun SleepTrendChartPreview() {
    val today = 20_000L
    val nights = listOf(432, 401, 0, 0, 512, 388, 455)
        .mapIndexed { index, minutes -> SleepNight(today - 6 + index, minutes) }
        .filter { it.minutesAsleep > 0 }
    AppTheme {
        Surface {
            Column(modifier = Modifier.padding(16.dp)) {
                SleepTrendChart(nights = nights, fromEpochDay = today - 6, toEpochDay = today)
            }
        }
    }
}
