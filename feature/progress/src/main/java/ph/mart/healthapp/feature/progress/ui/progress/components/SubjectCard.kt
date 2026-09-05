package ph.mart.healthapp.feature.progress.ui.progress.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.data.profile.TrendDirection
import ph.mart.healthapp.core.designsystem.icon.AppIcons
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.core.designsystem.theme.tabularNums
import ph.mart.healthapp.feature.progress.R
import ph.mart.healthapp.feature.progress.ui.photo.components.GRID_TILE_PX
import ph.mart.healthapp.feature.progress.ui.photo.components.rememberBitmapFromFile
import ph.mart.healthapp.feature.progress.ui.progress.Subject
import ph.mart.healthapp.feature.progress.ui.progress.SubjectAccent
import ph.mart.healthapp.feature.progress.ui.progress.SubjectPreview
import ph.mart.healthapp.feature.progress.ui.progress.SubjectSummary
import ph.mart.healthapp.feature.progress.ui.progress.TrendArrow

/** Tall enough for a name, a figure, a preview and a footnote without any of them wrapping. */
private val CardMinHeight = 104.dp

/** The preview strip. Small enough to read as texture rather than a chart you could measure. */
private val PreviewHeight = 26.dp

/**
 * One subject on the overview grid — the whole card is the door to its detail page.
 *
 * Two states, one box: a tracked subject fills `surfaceContainerLow`, an untracked one draws a
 * **dashed** `outlineVariant` outline over nothing and says "Nothing yet". The dash is what makes
 * the difference visible without colour, and it is why an empty card keeps its slot in the grid
 * rather than disappearing — a subject that vanished when it had no data is a subject nobody would
 * ever find. Tracked cards sort before empty ones inside a group; the empties are still reachable.
 *
 * [onHint] is the affordance line's own tap. It is the same destination as the card for every
 * subject but Blood pressure, whose "Log a reading" opens the sheet this screen already owns.
 */
@Composable
internal fun SubjectCard(
    summary: SubjectSummary,
    onClick: () -> Unit,
    onHint: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (summary.tracked) {
        TrackedCard(summary = summary, onClick = onClick, modifier = modifier)
    } else {
        EmptyCard(summary = summary, onClick = onClick, onHint = onHint, modifier = modifier)
    }
}

@Composable
private fun TrackedCard(summary: SubjectSummary, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = modifier.heightIn(min = CardMinHeight),
    ) {
        Column(
            modifier = Modifier.padding(12.dp).fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = stringResource(summary.subject.label),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = summary.value.orEmpty(),
                    style = MaterialTheme.typography.titleLarge.tabularNums,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                )
                summary.unit?.let {
                    Text(
                        text = " $it",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
            }
            SubjectPreviewGraph(
                preview = summary.preview,
                accent = summary.subject.accent,
                modifier = Modifier.fillMaxWidth().height(PreviewHeight),
            )
            // Pins the footnote to the bottom, so every card in a row lines its last line up.
            Spacer(modifier = Modifier.weight(1f))
            TrendFootnote(summary = summary)
        }
    }
}

@Composable
private fun TrendFootnote(summary: SubjectSummary) {
    val color = when (summary.trend) {
        TrendDirection.OnTrack -> MaterialTheme.colorScheme.primary
        TrendDirection.OffTrack -> MaterialTheme.colorScheme.error
        TrendDirection.Neutral -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        summary.arrow?.let {
            Icon(
                imageVector = when (it) {
                    TrendArrow.Down -> AppIcons.TrendDown
                    TrendArrow.Flat -> AppIcons.TrendFlat
                    TrendArrow.Up -> AppIcons.TrendUp
                },
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(12.dp),
            )
            Spacer(modifier = Modifier.size(4.dp))
        }
        Text(
            text = summary.footnote,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            maxLines = 2,
        )
    }
}

@Composable
private fun EmptyCard(
    summary: SubjectSummary,
    onClick: () -> Unit,
    onHint: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val outline = MaterialTheme.colorScheme.outlineVariant
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = Color.Transparent,
        modifier = modifier
            .heightIn(min = CardMinHeight)
            .dashedOutline(outline),
    ) {
        Column(
            modifier = Modifier.padding(12.dp).fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = stringResource(summary.subject.label),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = stringResource(R.string.progress_nothing_yet),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickableRow(onHint),
            ) {
                Text(
                    text = stringResource(summary.subject.emptyHint),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    modifier = Modifier.weight(1f, fill = false),
                )
                Icon(
                    imageVector = AppIcons.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp),
                )
            }
        }
    }
}

/** The 1dp dashed outline an empty card wears instead of a fill. Drawn rather than `border(...)`,
 * which has no dash. */
private fun Modifier.dashedOutline(color: Color): Modifier = drawBehind {
    val stroke = 1.dp.toPx()
    val radius = CornerRadius(20.dp.toPx())
    drawRoundRect(
        color = color,
        topLeft = Offset(stroke / 2, stroke / 2),
        size = Size(size.width - stroke, size.height - stroke),
        cornerRadius = radius,
        style = Stroke(width = stroke, pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f))),
    )
}

/** The affordance line sits inside a card that is already clickable, so it takes its own tap —
 * the two destinations differ for Blood pressure, and only for Blood pressure. */
private fun Modifier.clickableRow(onClick: () -> Unit): Modifier = clickable(onClick = onClick)

/**
 * The three previews the handoff draws, all one [Canvas] each and none of them a chart: no axis, no
 * labels, no goal line. A line for a value that moves, day bars for one that is counted, three
 * tiles for photos.
 *
 * A zero day is drawn as a stub in `outlineVariant` rather than skipped — a gap has to read as a
 * gap, which is the same reason `DayBarChart` places its bars by date.
 */
@Composable
internal fun SubjectPreviewGraph(
    preview: SubjectPreview,
    accent: SubjectAccent,
    modifier: Modifier = Modifier,
) {
    val accentColor = when (accent) {
        SubjectAccent.Primary -> MaterialTheme.colorScheme.primary
        SubjectAccent.Secondary -> MaterialTheme.colorScheme.secondary
    }
    val stubColor = MaterialTheme.colorScheme.outlineVariant
    when (preview) {
        is SubjectPreview.PhotoStrip -> Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = modifier,
        ) {
            preview.paths.take(3).forEach { path ->
                PhotoTile(path = path, modifier = Modifier.weight(1f).fillMaxSize())
            }
        }

        is SubjectPreview.Line -> Canvas(modifier = modifier) {
            val values = preview.values
            if (values.size < 2) return@Canvas
            val min = values.min()
            val max = values.max()
            val range = (max - min).coerceAtLeast(0.01)
            val inset = 2.dp.toPx()
            val usable = size.height - inset * 2
            fun point(index: Int) = Offset(
                x = index / (values.size - 1).toFloat() * size.width,
                y = inset + (usable - ((values[index] - min) / range * usable)).toFloat(),
            )
            for (i in 0 until values.size - 1) {
                drawLine(
                    color = accentColor,
                    start = point(i),
                    end = point(i + 1),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round,
                )
            }
        }

        is SubjectPreview.Bars -> Canvas(modifier = modifier) {
            val values = preview.values
            if (values.isEmpty()) return@Canvas
            val gap = 3.dp.toPx()
            val width = ((size.width - gap * (values.size - 1)) / values.size).coerceAtLeast(1f)
            val max = values.max().coerceAtLeast(1)
            val radius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
            values.forEachIndexed { index, value ->
                val height = (value.toFloat() / max * size.height).coerceAtLeast(if (value > 0) 2f else 0f)
                val x = index * (width + gap)
                if (value <= 0) {
                    // A day that happened and held nothing, drawn as a stub so it isn't a hole.
                    drawRoundRect(
                        color = stubColor,
                        topLeft = Offset(x, size.height - 2.dp.toPx()),
                        size = Size(width, 2.dp.toPx()),
                        cornerRadius = radius,
                    )
                } else {
                    drawRoundRect(
                        color = accentColor,
                        topLeft = Offset(x, size.height - height),
                        size = Size(width, height),
                        cornerRadius = radius,
                    )
                }
            }
        }

        SubjectPreview.None -> Spacer(modifier = modifier)
    }
}

@Composable
private fun PhotoTile(path: String, modifier: Modifier = Modifier) {
    val bitmap = rememberBitmapFromFile(path, GRID_TILE_PX)
    val photo = stringResource(R.string.progress_photo_generic)
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(5.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clearAndSetSemantics { contentDescription = photo },
    ) {
        bitmap?.let {
            Image(
                bitmap = it,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun SubjectCardPreview() {
    AppTheme {
        Surface {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(16.dp),
            ) {
                SubjectCard(
                    summary = SubjectSummary(
                        subject = Subject.Weight,
                        value = "82.7",
                        unit = "kg",
                        preview = SubjectPreview.Line(listOf(84.8, 84.4, 84.1, 83.6, 83.2, 82.9, 82.7)),
                        footnote = "0.4 kg this week · on track",
                        arrow = TrendArrow.Down,
                        trend = TrendDirection.OnTrack,
                    ),
                    onClick = {},
                    onHint = {},
                    modifier = Modifier.weight(1f),
                )
                SubjectCard(
                    summary = SubjectSummary(subject = Subject.Supplements),
                    onClick = {},
                    onHint = {},
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun SubjectCardBarsPreview() {
    AppTheme {
        Surface {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(16.dp),
            ) {
                SubjectCard(
                    summary = SubjectSummary(
                        subject = Subject.Nutrition,
                        value = "1651",
                        unit = "kcal avg",
                        // The third day is a gap, and draws as a stub rather than nothing.
                        preview = SubjectPreview.Bars(listOf(1850, 2100, 0, 1720, 2340, 1610, 1490)),
                        footnote = "610 kcal under target",
                    ),
                    onClick = {},
                    onHint = {},
                    modifier = Modifier.weight(1f),
                )
                SubjectCard(
                    summary = SubjectSummary(
                        subject = Subject.Sleep,
                        value = "6h 52m",
                        unit = "avg",
                        preview = SubjectPreview.Bars(listOf(432, 401, 512, 388, 447, 460, 402)),
                        footnote = "From your watch · 7 nights",
                    ),
                    onClick = {},
                    onHint = {},
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}
