package ph.mart.healthapp.feature.progress.ui.heart.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.data.health.HeartDay
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.progress.ui.shared.components.RangeBar
import ph.mart.healthapp.feature.progress.ui.shared.components.RangeBarChart

/** Breathing room above and below the window's own readings, so no bar sits flat on an edge. */
private const val AXIS_PAD_BPM = 5

/**
 * Each day as a floating bar spanning that day's lowest reading up to its average.
 *
 * The drawing is [RangeBarChart]'s — including the decision not to zero-base the axis, which that
 * file explains. This one only says what a heart bar's two ends *are*.
 */
@Composable
fun HeartTrendChart(
    days: List<HeartDay>,
    fromEpochDay: Long,
    toEpochDay: Long,
    modifier: Modifier = Modifier,
) {
    RangeBarChart(
        bars = days.map { RangeBar(it.dateEpochDay, low = it.minBpm, high = it.averageBpm) },
        fromEpochDay = fromEpochDay,
        toEpochDay = toEpochDay,
        axisPad = AXIS_PAD_BPM,
        modifier = modifier,
    )
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
