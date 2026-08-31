package ph.mart.healthapp.feature.progress.ui.supplement.components

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
import ph.mart.healthapp.core.data.supplement.SupplementDay
import ph.mart.healthapp.core.data.supplement.adherenceByDay
import ph.mart.healthapp.core.designsystem.theme.AppTheme

/**
 * The share of each day's doses that were actually taken, as bars over a fixed date window — one
 * [Canvas], no charting library, same construction as [
 * ph.mart.healthapp.feature.progress.ui.mood.components.MoodTrendChart].
 *
 * Zero-based and capped at 100%, unlike the Heart tab's chart: the axis here has a real floor and
 * a real ceiling, and a day at 0 is a day the user saw the list and took none of it — worth
 * drawing as an empty slot rather than hiding.
 *
 * Slots come from the *window*, not the list: the series is sparse, so a day's x-position is its
 * date. A day with no rows at all draws nothing, which is what keeps a week before the user's
 * first supplement looking like a gap rather than a run of misses.
 */
@Composable
fun SupplementAdherenceChart(
    days: List<SupplementDay>,
    fromEpochDay: Long,
    toEpochDay: Long,
    modifier: Modifier = Modifier,
) {
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val barColor = MaterialTheme.colorScheme.primary

    Canvas(modifier = modifier.fillMaxWidth().height(200.dp)) {
        val slots = (toEpochDay - fromEpochDay + 1).toInt()
        if (slots <= 0) return@Canvas

        repeat(4) { row ->
            val y = size.height / 3f * row
            drawLine(gridColor, start = Offset(0f, y), end = Offset(size.width, y), strokeWidth = 1.dp.toPx())
        }

        val slot = size.width / slots
        val barWidth = (slot - 1.dp.toPx()).coerceAtLeast(slot * 0.8f)
        days.adherenceByDay().forEach { (date, share) ->
            val index = (date - fromEpochDay).toInt()
            if (index !in 0 until slots) return@forEach
            // A zero day is a drawn slot with no height — the gridline is what shows it was seen.
            val top = size.height - share * size.height
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
private fun SupplementAdherenceChartPreview() {
    val today = 20_000L
    val days = listOf(2 to 2, 1 to 2, 2 to 2, 0 to 2, 2 to 2)
        .mapIndexed { index, (taken, due) ->
            SupplementDay(today - 6 + index, supplementId = 1, taken = taken, dueTimes = due)
        }
    AppTheme {
        Surface {
            Column(modifier = Modifier.padding(16.dp)) {
                SupplementAdherenceChart(days = days, fromEpochDay = today - 6, toEpochDay = today)
            }
        }
    }
}
