package ph.mart.healthapp.feature.progress.ui.timelapse.components

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.data.progress.ProgressPhoto
import ph.mart.healthapp.core.data.todayEpochDay
import ph.mart.healthapp.core.designsystem.component.formatDayMonth
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.core.designsystem.theme.tabularNums
import ph.mart.healthapp.feature.progress.R
import ph.mart.healthapp.feature.progress.ui.shared.components.photoStageColor
import ph.mart.healthapp.feature.progress.ui.timelapse.nearestFrame
import ph.mart.healthapp.feature.progress.ui.timelapse.tickFractions

/** A finger is 48dp wide and the track is 4 — the difference is hit area, not ink. */
private val TrackHitHeight = 48.dp
private val TrackHeight = 4.dp
private val TickHeight = 12.dp
private val PlayheadRadius = 8.dp

/** Past this many shots the ticks stop being countable and start being texture, so they thin out
 * rather than merging into a solid bar. */
private const val DENSE_TICK_COUNT = 24

/**
 * The scrubber, as a **date** timeline rather than a frame index.
 *
 * Each shot's tick sits where it was actually taken within the run, so a fortnight of daily photos
 * and the month of nothing that followed look like what they are. An index slider draws those as
 * equal steps and quietly tells the reader the run was evenly paced — the one claim a progress
 * timelapse should never make on its own.
 *
 * Dragging scrubs and snaps to the nearest shot by date; [onScrubTo] is what pauses playback, the
 * same call the old slider made.
 */
@Composable
internal fun TimelapseTimeline(
    photos: List<ProgressPhoto>,
    index: Int,
    onScrubTo: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (photos.size < 2) return
    val fractions = remember(photos) { tickFractions(photos) }
    val position = fractions.getOrElse(index) { 0f }
    val track = MaterialTheme.colorScheme.outlineVariant
    val played = MaterialTheme.colorScheme.primary
    val ring = photoStageColor()
    val spoken = stringResource(R.string.progress_timelapse_timeline)
    val dense = photos.size > DENSE_TICK_COUNT

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(TrackHitHeight)
                .pointerInput(photos) {
                    detectTapGestures { offset -> onScrubTo(nearestFrame(offset.x / size.width, photos)) }
                }
                .pointerInput(photos) {
                    detectHorizontalDragGestures(
                        onDragStart = { offset -> onScrubTo(nearestFrame(offset.x / size.width, photos)) },
                    ) { change, _ ->
                        change.consume()
                        onScrubTo(nearestFrame(change.position.x / size.width, photos))
                    }
                }
                .semantics {
                    contentDescription = spoken
                    progressBarRangeInfo = ProgressBarRangeInfo(position, 0f..1f)
                    setProgress { target -> onScrubTo(nearestFrame(target, photos)); true }
                }
                .drawBehind {
                    drawTimeline(
                        fractions = fractions,
                        position = position,
                        trackColor = track,
                        playedColor = played,
                        ringColor = ring,
                        trackHeight = TrackHeight.toPx(),
                        tickHeight = TickHeight.toPx(),
                        tickWidth = (if (dense) 1.dp else 2.dp).toPx(),
                        playheadRadius = PlayheadRadius.toPx(),
                    )
                },
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            RangeLabel(photos.first().dateEpochDay)
            RangeLabel(photos.last().dateEpochDay)
        }
    }
}

@Composable
private fun RangeLabel(epochDay: Long) {
    Text(
        text = formatDayMonth(epochDay),
        style = MaterialTheme.typography.labelSmall.tabularNums,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/**
 * Drawn rather than composed: one shot per tick means a set of sixty would otherwise be sixty
 * layout nodes redrawn eight times a second.
 *
 * The playhead carries a ring in the stage's own colour — without it, a playhead sitting on its
 * own progress fill is a `primary` circle on a `primary` bar, and at speed there is nothing to
 * follow.
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawTimeline(
    fractions: List<Float>,
    position: Float,
    trackColor: Color,
    playedColor: Color,
    ringColor: Color,
    trackHeight: Float,
    tickHeight: Float,
    tickWidth: Float,
    playheadRadius: Float,
) {
    val centerY = size.height / 2f
    val radius = CornerRadius(trackHeight / 2f, trackHeight / 2f)
    // Inset by the playhead, so it has room to sit fully on the track at either end.
    val left = playheadRadius
    val usable = (size.width - 2 * playheadRadius).coerceAtLeast(0f)

    drawRoundRect(
        color = trackColor,
        topLeft = Offset(left, centerY - trackHeight / 2f),
        size = Size(usable, trackHeight),
        cornerRadius = radius,
    )
    drawRoundRect(
        color = playedColor,
        topLeft = Offset(left, centerY - trackHeight / 2f),
        size = Size(usable * position, trackHeight),
        cornerRadius = radius,
    )
    fractions.forEach { fraction ->
        drawRoundRect(
            color = if (fraction <= position) playedColor else trackColor,
            topLeft = Offset(left + usable * fraction - tickWidth / 2f, centerY - tickHeight / 2f),
            size = Size(tickWidth, tickHeight),
            cornerRadius = CornerRadius(tickWidth / 2f, tickWidth / 2f),
        )
    }
    val playheadX = left + usable * position
    drawCircle(color = ringColor, radius = playheadRadius + tickWidth, center = Offset(playheadX, centerY))
    drawCircle(color = playedColor, radius = playheadRadius, center = Offset(playheadX, centerY))
}

@PreviewLightDark
@Composable
private fun TimelapseTimelinePreview() {
    val today = todayEpochDay()
    AppTheme {
        // The gap is the point: three shots in a week, then three weeks of nothing.
        Surface(color = photoStageColor()) {
            TimelapseTimeline(
                photos = listOf(
                    ProgressPhoto(id = 1, dateEpochDay = today - 30, filePath = ""),
                    ProgressPhoto(id = 2, dateEpochDay = today - 28, filePath = ""),
                    ProgressPhoto(id = 3, dateEpochDay = today - 26, filePath = ""),
                    ProgressPhoto(id = 4, dateEpochDay = today, filePath = ""),
                ),
                index = 2,
                onScrubTo = {},
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}
