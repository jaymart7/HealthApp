package ph.mart.healthapp.feature.progress.ui.fasting.components

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
import ph.mart.healthapp.core.data.fasting.FastSession
import ph.mart.healthapp.core.data.fasting.dateEpochDay
import ph.mart.healthapp.core.data.fasting.durationMinutes
import ph.mart.healthapp.core.designsystem.theme.AppTheme

/** The y-axis never shrinks below a day, so a run of short fasts reads as short rather than
 * filling the canvas the way an auto-ranged axis would let it. */
private const val FULL_DAY_MINUTES = 24 * 60

/**
 * Completed fasts as bars over a fixed date window — one [Canvas], no charting library, same
 * construction as [ph.mart.healthapp.feature.progress.ui.sleep.components.SleepTrendChart]
 * (gridlines in `outlineVariant`, zero-based bars). Days without a fast draw nothing, so a gap
 * stays visibly a gap.
 *
 * The one addition over the sleep chart is the dashed goal line: a fast's whole point is whether
 * it cleared the target, and a bar you have to measure against an axis doesn't say that.
 *
 * Slots come from the *window*, not the list, because the series is sparse — a fast's x-position
 * is the day it ended.
 */
@Composable
fun FastingTrendChart(
    sessions: List<FastSession>,
    goalHours: Int,
    fromEpochDay: Long,
    toEpochDay: Long,
    modifier: Modifier = Modifier,
) {
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val barColor = MaterialTheme.colorScheme.primary
    val goalColor = MaterialTheme.colorScheme.onSurfaceVariant

    Canvas(modifier = modifier.fillMaxWidth().height(200.dp)) {
        val slots = (toEpochDay - fromEpochDay + 1).toInt()
        if (slots <= 0) return@Canvas

        val goalMinutes = goalHours * 60
        val maxValue = maxOf(
            sessions.maxOfOrNull { it.durationMinutes(nowMillis = 0) } ?: 0,
            goalMinutes,
            FULL_DAY_MINUTES,
        ).toFloat()
        fun yFor(value: Int): Float = size.height - (value / maxValue * size.height)

        repeat(4) { row ->
            val y = size.height / 3f * row
            drawLine(gridColor, start = Offset(0f, y), end = Offset(size.width, y), strokeWidth = 1.dp.toPx())
        }

        // Same ceiling the sleep and nutrition charts accept: at a year's range a slot is under a
        // pixel wide, so the gap only ever comes out of a slot that can spare it.
        val slot = size.width / slots
        val barWidth = (slot - 1.dp.toPx()).coerceAtLeast(slot * 0.6f)
        sessions.forEach { session ->
            val index = (session.dateEpochDay - fromEpochDay).toInt()
            val minutes = session.durationMinutes(nowMillis = 0)
            if (index !in 0 until slots || minutes <= 0) return@forEach
            val top = yFor(minutes)
            drawRect(
                color = barColor,
                topLeft = Offset(index * slot, top),
                size = Size(barWidth, size.height - top),
            )
        }

        // Drawn last so it reads over the bars it judges.
        val goalY = yFor(goalMinutes)
        drawLine(
            color = goalColor,
            start = Offset(0f, goalY),
            end = Offset(size.width, goalY),
            strokeWidth = 1.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx())),
        )
    }
}

@PreviewLightDark
@Composable
private fun FastingTrendChartPreview() {
    val today = 20_000L
    val day = 86_400_000L
    val sessions = listOf(15, 17, 12, 18).mapIndexed { index, hours ->
        val end = (today - 3 + index) * day + 12 * 3_600_000L
        FastSession(
            id = index.toLong(),
            startMillis = end - hours * 3_600_000L,
            endMillis = end,
            goalHours = 16,
        )
    }
    AppTheme {
        Surface {
            Column(modifier = Modifier.padding(16.dp)) {
                FastingTrendChart(
                    sessions = sessions,
                    goalHours = 16,
                    fromEpochDay = today - 6,
                    toEpochDay = today,
                )
            }
        }
    }
}
