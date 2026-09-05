package ph.mart.healthapp.feature.home.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.designsystem.component.AppCard
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.core.designsystem.theme.tabularNums

/** The floor every tappable cell on Home holds — glasses, mood steps, flow steps, week cells, pills. */
internal val TapTargetMin = 44.dp

/** What a meta bar or button is given when the card is unpaired and its value sits to the left. */
private val MetaWideWidth = 120.dp

private val StatusDotSize = 8.dp
private val MetaBarHeight = 4.dp

/**
 * Whether this card's figure is on track — and, most of the time, whether that is even a question
 * this app can answer.
 *
 * [None] is the default and the common case on purpose. A dot on every card would be the screen
 * grading the user; it appears only where on-track/off-track is a fact FitPulse measures against a
 * target the user set — calories against the budget, a streak that is running, a fast past its
 * goal, a weight moving the way the goal asks. Water, macros, steps, sleep, heart, blood pressure,
 * mood, cycle, supplements, workouts and photos have no verdict, so they carry no mark.
 *
 * [OffTrack] is `error`, the trend-arrow rule: genuinely off track, never one step of a scale.
 */
internal enum class StatusMark { None, OnTrack, OffTrack }

/**
 * One figure, its label, and an optional line of supporting detail — the shape eight of Home's
 * cards share.
 *
 * [wide] is the whole of the difference between the two arrangements, and it is decided by
 * `homeRows()`, never by the card: paired with a neighbour it stacks, alone on its row it lays the
 * meta out beside the value instead of under it. That is why no card here may depend on its
 * position — every one of them has to read correctly both ways, and the same content is used for
 * both.
 *
 * [meta] is a `ColumnScope` slot rather than a typed parameter because its four shapes ([MetaText],
 * [MetaBar], [MetaButton], and the streak's badge row) have nothing in common but their placement.
 * The card gives it its width and alignment; the content just fills.
 */
@Composable
internal fun MetricCard(
    label: String,
    value: String,
    wide: Boolean,
    modifier: Modifier = Modifier,
    unit: String? = null,
    status: StatusMark = StatusMark.None,
    meta: @Composable ColumnScope.() -> Unit = {},
) {
    AppCard(modifier = modifier) {
        if (wide) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.Start),
                verticalAlignment = Alignment.Bottom,
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    LabelRow(label = label, status = status)
                    Value(value = value, unit = unit, wide = true)
                }
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalAlignment = Alignment.End,
                    modifier = Modifier.width(MetaWideWidth),
                    content = meta,
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    LabelRow(label = label, status = status)
                    Value(value = value, unit = unit, wide = false)
                }
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth(),
                    content = meta,
                )
            }
        }
    }
}

@Composable
private fun LabelRow(label: String, status: StatusMark) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        StatusDot(status)
    }
}

/**
 * The mark carries no content description: it restates the figure beside it — a streak of 0 days,
 * a weight moving the wrong way — and a screen reader announcing "on track" after every value
 * would be reading the same fact twice.
 */
@Composable
internal fun StatusDot(status: StatusMark) {
    val color = when (status) {
        StatusMark.None -> return
        StatusMark.OnTrack -> MaterialTheme.colorScheme.primary
        StatusMark.OffTrack -> MaterialTheme.colorScheme.error
    }
    Box(modifier = Modifier.size(StatusDotSize).background(color, CircleShape))
}

@Composable
private fun Value(value: String, unit: String?, wide: Boolean) {
    Row(verticalAlignment = Alignment.Bottom) {
        Text(
            text = value,
            style = if (wide) {
                MaterialTheme.typography.headlineSmall.tabularNums
            } else {
                MaterialTheme.typography.titleLarge.tabularNums
            },
            color = MaterialTheme.colorScheme.onSurface,
        )
        if (unit != null) {
            Text(
                text = unit,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 2.dp),
            )
        }
    }
}

/** A line of detail, optionally with a second, quieter one under it. */
@Composable
internal fun ColumnScope.MetaText(
    text: String,
    sub: String? = null,
    color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurfaceVariant,
    leading: @Composable (() -> Unit)? = null,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        leading?.invoke()
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium.tabularNums,
            color = color,
            textAlign = TextAlign.End,
        )
    }
    if (sub != null) {
        Text(
            text = sub,
            style = MaterialTheme.typography.labelSmall.tabularNums,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.End,
        )
    }
}

/**
 * A share of a target, with its caption under it. [progress] is a lambda so the value is read
 * inside the draw lambda and never invalidates composition — the shape `FastingCard`'s goal bar
 * already uses.
 */
@Composable
internal fun ColumnScope.MetaBar(progress: () -> Float, caption: String) {
    val trackColor = MaterialTheme.colorScheme.surfaceContainerHigh
    val fillColor = MaterialTheme.colorScheme.primary
    Canvas(modifier = Modifier.fillMaxWidth().height(MetaBarHeight)) {
        val radius = CornerRadius(size.height / 2f)
        drawRoundRect(color = trackColor, size = size, cornerRadius = radius)
        val width = size.width * progress().coerceIn(0f, 1f)
        if (width > 0f) {
            drawRoundRect(
                color = fillColor,
                size = Size(width.coerceAtLeast(size.height), size.height),
                cornerRadius = radius,
            )
        }
    }
    Text(
        text = caption,
        style = MaterialTheme.typography.labelSmall.tabularNums,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.End,
    )
}

/** The one thing a metric card can do besides report — start a fast, take a photo. */
@Composable
internal fun MetaButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        modifier = modifier.heightIn(min = TapTargetMin),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(PaddingValues(horizontal = 20.dp)),
        ) {
            Text(text = label, style = MaterialTheme.typography.labelLarge)
        }
    }
}

/** Both arrangements of the same content, which is the whole contract. */
@PreviewLightDark
@Composable
private fun MetricCardPreview() {
    AppTheme {
        Surface {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(16.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MetricCard(
                        label = "Weight",
                        value = "82.7",
                        unit = " kg",
                        wide = false,
                        status = StatusMark.OnTrack,
                        modifier = Modifier.weight(1f),
                    ) { MetaText(text = "0.4 kg vs 7d", sub = "74 kg by Feb 2027") }
                    MetricCard(
                        label = "Heart",
                        value = "68",
                        unit = " bpm",
                        wide = false,
                        modifier = Modifier.weight(1f),
                    ) { MetaText(text = "Day average", sub = "Lowest 52 bpm") }
                }
                MetricCard(label = "Steps", value = "8,432", wide = true) {
                    MetaBar(progress = { 0.84f }, caption = "10,000 goal · 312 kcal")
                }
                MetricCard(label = "Fasting", value = "Not started", wide = true) {
                    MetaButton(label = "Start", onClick = {})
                }
            }
        }
    }
}
