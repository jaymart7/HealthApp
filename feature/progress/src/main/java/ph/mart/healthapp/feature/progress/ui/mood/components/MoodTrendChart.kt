package ph.mart.healthapp.feature.progress.ui.mood.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.data.mood.MOOD_SCALE
import ph.mart.healthapp.core.data.mood.MoodDay
import ph.mart.healthapp.core.designsystem.theme.AppTheme

/**
 * Mood and energy as paired bars over a fixed date window — one [Canvas], no charting library,
 * same construction as [NutritionTrendChart] (gridlines in `outlineVariant`, zero-based bars).
 * Days with nothing logged draw nothing, so a gap stays visibly a gap.
 *
 * Slots come from the *window*, not from the list, because the mood series is sparse: a day's
 * position on the x-axis is its date, which is what keeps a two-week silence looking like two
 * weeks rather than closing up.
 *
 * `primary`/`secondary` here are just two distinguishable series colours — the fixed
 * protein/carbs/fat assignment is about macro charts and doesn't reach this one. Two series on
 * one canvas is also why this chart carries a legend and the others don't.
 */
@Composable
fun MoodTrendChart(
    days: List<MoodDay>,
    fromEpochDay: Long,
    toEpochDay: Long,
    modifier: Modifier = Modifier,
) {
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val moodColor = MaterialTheme.colorScheme.primary
    val energyColor = MaterialTheme.colorScheme.secondary

    Column(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxWidth().height(200.dp)) {
            val slots = (toEpochDay - fromEpochDay + 1).toInt()
            if (slots <= 0) return@Canvas

            val maxValue = MOOD_SCALE.last.toFloat()
            fun yFor(value: Int): Float = size.height - (value / maxValue * size.height)

            repeat(4) { row ->
                val y = size.height / 3f * row
                drawLine(gridColor, start = Offset(0f, y), end = Offset(size.width, y), strokeWidth = 1.dp.toPx())
            }

            // Half a slot each, so the pair reads as one day. At a year's range a slot is under a
            // pixel wide and the two series blend — the same ceiling NutritionTrendChart accepts.
            val slot = size.width / slots
            val barWidth = (slot / 2f - 0.5.dp.toPx()).coerceAtLeast(slot / 2f * 0.8f)
            days.forEach { day ->
                val index = (day.dateEpochDay - fromEpochDay).toInt()
                if (index !in 0 until slots) return@forEach
                listOf(day.mood to moodColor, day.energy to energyColor)
                    .forEachIndexed { series, (value, color) ->
                        if (value <= 0) return@forEachIndexed
                        val top = yFor(value)
                        drawRect(
                            color = color,
                            topLeft = Offset(index * slot + series * slot / 2f, top),
                            size = Size(barWidth, size.height - top),
                        )
                    }
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(top = 8.dp),
        ) {
            LegendDot(color = moodColor, label = "Mood")
            LegendDot(color = energyColor, label = "Energy")
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Column(modifier = Modifier.size(8.dp).clip(CircleShape).background(color)) {}
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@PreviewLightDark
@Composable
private fun MoodTrendChartPreview() {
    val today = 20_000L
    val days = listOf(4 to 3, 5 to 4, 3 to 2, 0 to 0, 2 to 0, 4 to 5, 5 to 4)
        .mapIndexed { index, (mood, energy) -> MoodDay(today - 6 + index, mood, energy) }
        .filter { it.mood > 0 || it.energy > 0 }
    AppTheme {
        Surface {
            Column(modifier = Modifier.padding(16.dp)) {
                MoodTrendChart(days = days, fromEpochDay = today - 6, toEpochDay = today)
            }
        }
    }
}
