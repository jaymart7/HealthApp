package ph.mart.healthapp.feature.progress.ui.progress.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.data.profile.TrendDirection
import ph.mart.healthapp.core.data.progress.ChartRange
import ph.mart.healthapp.core.designsystem.component.SegmentedToggle
import ph.mart.healthapp.core.designsystem.icon.AppIcons
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.core.designsystem.theme.tabularNums
import ph.mart.healthapp.feature.progress.ui.progress.Subject

/** Every tap target on a detail page clears this. */
private val TapTarget = 48.dp

/** "1M" needs nothing like a `labelLarge` word's worth of pill, and four of them have to share a
 * card header with its title. */
private val RangePillWidth = 40.dp

/**
 * The detail page's own toolbar. Not `AppTopBar`: that one belongs to `AppScaffold` and is what a
 * *route* a level above a tab gets. A subject page is a swap-in inside the Progress tab — it keeps
 * the bottom bar and the FAB — so it draws its own back arrow, and back goes to the overview
 * rather than out of the tab.
 */
@Composable
internal fun DetailHeader(
    title: String,
    onBack: () -> Unit,
    onShare: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().heightIn(min = 56.dp).padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack, modifier = Modifier.size(TapTarget)) {
            Icon(
                imageVector = AppIcons.Back,
                contentDescription = "Back to Progress",
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f).padding(start = 4.dp),
        )
        if (onShare != null) {
            IconButton(onClick = onShare, modifier = Modifier.size(TapTarget)) {
                Icon(
                    imageVector = AppIcons.Share,
                    contentDescription = "Recap",
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

/** The one number the page is about, and what it is. Baseline-aligned so the unit sits on the
 * figure's feet rather than its middle. */
@Composable
internal fun HeroValue(value: String, caption: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 4.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.displayMedium.tabularNums,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = " $caption",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp),
        )
    }
}

/** One fact, in a pill. [trend] colours it and [leading] says which way — never one without the
 * other, and the words in [text] say it a third time. */
data class FactChip(
    val text: String,
    val leading: ImageVector? = null,
    val trend: TrendDirection = TrendDirection.Neutral,
)

@Composable
internal fun FactChipRow(chips: List<FactChip>, modifier: Modifier = Modifier) {
    if (chips.isEmpty()) return
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        chips.forEach { chip ->
            val color = when (chip.trend) {
                TrendDirection.OnTrack -> MaterialTheme.colorScheme.primary
                TrendDirection.OffTrack -> MaterialTheme.colorScheme.error
                TrendDirection.Neutral -> MaterialTheme.colorScheme.onSurfaceVariant
            }
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    chip.leading?.let {
                        Icon(
                            imageVector = it,
                            contentDescription = null,
                            tint = color,
                            modifier = Modifier.size(12.dp),
                        )
                        Spacer(modifier = Modifier.size(4.dp))
                    }
                    Text(
                        text = chip.text,
                        style = MaterialTheme.typography.bodySmall.tabularNums,
                        color = color,
                    )
                }
            }
        }
    }
}

/** One entry of a chart's legend — a swatch and what it means. [dashed] draws the goal marker's
 * own stroke rather than a solid bar, so the two never read as the same series. */
data class LegendEntry(val label: String, val color: Color, val dashed: Boolean = false)

/**
 * The card a chart lives in — its title, **its own range toggle**, the chart, and the legend.
 *
 * The toggle moved in here from above the tab strip, which is the change that makes a detail page
 * readable: the control now sits inside the thing it controls, and the range is remembered per
 * subject rather than shared, so re-slicing Sleep can't silently re-slice Weight.
 *
 * The legend is not decoration. It is what keeps a three-series chart readable without colour
 * vision, which is why it is a parameter rather than an option.
 */
@Composable
internal fun ChartCard(
    title: String,
    range: ChartRange?,
    onRangeChange: ((ChartRange) -> Unit)?,
    legend: List<LegendEntry>,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                // Null on a card that shares another's toggle — the Activity page draws two charts
                // against one range, and two identical toggles would be two controls for one value.
                if (range != null && onRangeChange != null) {
                    SegmentedToggle(
                        options = ChartRange.entries.map { it.label },
                        selectedIndex = ChartRange.entries.indexOf(range),
                        onSelect = { index -> onRangeChange(ChartRange.entries[index]) },
                        trackColor = MaterialTheme.colorScheme.surfaceContainer,
                        minPillWidth = RangePillWidth,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            content()
            if (legend.isNotEmpty()) {
                HorizontalDivider(
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant,
                    modifier = Modifier.padding(top = 16.dp, bottom = 12.dp),
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    legend.forEach { entry ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Swatch(entry = entry)
                            Spacer(modifier = Modifier.size(6.dp))
                            Text(
                                text = entry.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Swatch(entry: LegendEntry) {
    Box(
        modifier = Modifier
            .width(14.dp)
            .height(4.dp)
            .drawBehind {
                drawLine(
                    color = entry.color,
                    start = Offset(0f, size.height / 2),
                    end = Offset(size.width, size.height / 2),
                    strokeWidth = 2.5.dp.toPx(),
                    cap = StrokeCap.Round,
                    pathEffect = if (entry.dashed) {
                        PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx()))
                    } else {
                        null
                    },
                )
            },
    )
}

/** A label and its figure. [valueColor] is set only where the figure carries a verdict, and only
 * beside words that already carry it. */
data class StatRow(val label: String, val value: String, val trend: TrendDirection = TrendDirection.Neutral)

/**
 * The stat rows under a chart — one card, 56dp rows, inset dividers. This replaced the three-across
 * `StatCell` row every tab used to draw: three cells could not hold "86 of 92 days" without
 * wrapping, and a row reads left to right the way the figure is spoken.
 */
@Composable
internal fun StatRowsCard(rows: List<StatRow>, modifier: Modifier = Modifier) {
    if (rows.isEmpty()) return
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column {
            rows.forEachIndexed { index, row ->
                if (index > 0) {
                    HorizontalDivider(
                        thickness = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = row.label,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = row.value,
                        style = MaterialTheme.typography.titleSmall.tabularNums,
                        color = when (row.trend) {
                            TrendDirection.OnTrack -> MaterialTheme.colorScheme.primary
                            TrendDirection.OffTrack -> MaterialTheme.colorScheme.error
                            TrendDirection.Neutral -> MaterialTheme.colorScheme.onSurface
                        },
                    )
                }
            }
        }
    }
}

/**
 * The rest of the group, at the foot of a detail page — the one new navigation affordance, and what
 * replaces browsing by tab strip without bringing a strip back. It **replaces** the current page
 * rather than pushing onto it, so hopping Sleep → Mood → Heart leaves one back step, not three.
 *
 * Three siblings or fewer draw as pills; four draw as rows, because four pills on a 360dp screen
 * are four clipped words. A row says what the subject holds, or "Nothing yet" — which is the same
 * claim the overview's dashed card makes, in the same words.
 */
@Composable
internal fun SiblingSwitcher(
    groupLabel: String,
    siblings: List<Pair<Subject, String?>>,
    onSelect: (Subject) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (siblings.isEmpty()) return
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "More in $groupLabel",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp, top = 12.dp, bottom = 12.dp),
        )
        if (siblings.size <= 3) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                siblings.forEach { (subject, _) ->
                    Surface(
                        onClick = { onSelect(subject) },
                        shape = RoundedCornerShape(999.dp),
                        color = Color.Transparent,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.weight(1f).heightIn(min = TapTarget),
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = subject.label,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                siblings.forEach { (subject, value) ->
                    Surface(
                        onClick = { onSelect(subject) },
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier
                                .heightIn(min = 56.dp)
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = subject.label,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                text = value ?: "Nothing yet",
                                style = MaterialTheme.typography.bodySmall.tabularNums,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Icon(
                                imageVector = AppIcons.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 8.dp).size(20.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun DetailChromePreview() {
    AppTheme {
        Surface {
            Column(modifier = Modifier.padding(bottom = 16.dp)) {
                DetailHeader(title = "Weight", onBack = {}, onShare = {})
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    HeroValue(value = "82.7", caption = "kg today")
                    FactChipRow(
                        chips = listOf(
                            FactChip("2.1 kg in 3 months · on track", AppIcons.TrendDown, TrendDirection.OnTrack),
                            FactChip("0.7 kg to goal"),
                        ),
                    )
                    StatRowsCard(
                        rows = listOf(
                            StatRow("This week", "0.4 kg down", TrendDirection.OnTrack),
                            StatRow("Weekly average", "0.4 kg"),
                            StatRow("Readings logged", "86 of 92 days"),
                        ),
                    )
                    SiblingSwitcher(
                        groupLabel = "Body",
                        siblings = listOf(Subject.Photos to "14 shots", Subject.Measurements to null),
                        onSelect = {},
                    )
                }
            }
        }
    }
}
