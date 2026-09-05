package ph.mart.healthapp.feature.progress.ui.progress.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.data.profile.TrendDirection
import ph.mart.healthapp.core.designsystem.icon.AppIcons
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.progress.R
import ph.mart.healthapp.feature.progress.ui.progress.Subject
import ph.mart.healthapp.feature.progress.ui.progress.SubjectGroup
import ph.mart.healthapp.feature.progress.ui.progress.SubjectPreview
import ph.mart.healthapp.feature.progress.ui.progress.SubjectSummary
import ph.mart.healthapp.feature.progress.ui.progress.TrendArrow
import ph.mart.healthapp.feature.progress.ui.progress.subjectsIn

/** A row's own minimum, and the app's minimum touch target. */
private val RowMinHeight = 56.dp

/**
 * One family of subjects — a header naming it and how much of it is tracked, then a two-column grid
 * of its cards.
 *
 * A group with **nothing** tracked collapses to a single row instead. That is the whole of the
 * sparse-account treatment: there is no "sparse mode" flag anywhere, just a group that has nothing
 * to show yet saying so in one line rather than in four dashed boxes. Tapping it expands the grid
 * in place, for the session only — a preference nobody set is not worth a column.
 *
 * Tracked cards sort before empty ones, so the reading order is "what you have, then what you
 * could have".
 */
@Composable
internal fun GroupSection(
    group: SubjectGroup,
    summaries: Map<Subject, SubjectSummary>,
    /** `Profile.cycleTrackingOn` — off drops the Cycle subject from this group entirely, which is
     * the only thing that can take a card out of a grid rather than dashing it. */
    cycleTracking: Boolean,
    expanded: Boolean,
    onToggle: () -> Unit,
    onOpen: (Subject) -> Unit,
    onHint: (Subject) -> Unit,
    modifier: Modifier = Modifier,
) {
    val subjects = subjectsIn(group, cycleTracking)
    val cards = subjects.sortedByDescending { summaries[it]?.tracked == true }
    val trackedCount = subjects.count { summaries[it]?.tracked == true }

    if (trackedCount == 0 && !expanded) {
        CollapsedGroupRow(group = group, subjectCount = subjects.size, onExpand = onToggle, modifier = modifier)
        return
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp, start = 4.dp, end = 4.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(
                text = stringResource(group.label),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = if (trackedCount == subjects.size) {
                    "$trackedCount tracked"
                } else {
                    "$trackedCount of ${subjects.size} tracked"
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        cards.chunked(2).forEach { pair ->
            // IntrinsicSize.Min is what lets a card's footnote pin to its bottom: it gives the row
            // the height of its tallest card, so the shorter one's weighted spacer has slack to
            // take. Without it both columns wrap their own content and the last lines misalign.
            Row(
                modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min).padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                pair.forEach { subject ->
                    val summary = summaries[subject] ?: SubjectSummary(subject)
                    SubjectCard(
                        summary = summary,
                        onClick = { onOpen(subject) },
                        onHint = { onHint(subject) },
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                    )
                }
                if (pair.size == 1) Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

/** A group nobody has started. One row rather than a grid of dashed boxes — the point of the
 * grouping is that an untouched family should cost one line of screen, not four cards. */
@Composable
private fun CollapsedGroupRow(
    group: SubjectGroup,
    subjectCount: Int,
    onExpand: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val showGroup = stringResource(R.string.progress_show_group, stringResource(group.label))
    Surface(
        onClick = onExpand,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = modifier.fillMaxWidth().padding(top = 12.dp),
    ) {
        Row(
            modifier = Modifier.heightIn(min = RowMinHeight).padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(group.label),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(R.string.progress_group_none_tracked, subjectCount),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                imageVector = AppIcons.ChevronRight,
                contentDescription = showGroup,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun GroupSectionPreview() {
    AppTheme {
        Surface {
            Column(modifier = Modifier.padding(16.dp)) {
                GroupSection(
                    group = SubjectGroup.Body,
                    cycleTracking = false,
                    summaries = mapOf(
                        Subject.Weight to SubjectSummary(
                            subject = Subject.Weight,
                            value = "82.7",
                            unit = "kg",
                            preview = SubjectPreview.Line(listOf(84.8, 84.1, 83.6, 83.2, 82.7)),
                            footnote = "0.4 kg this week · on track",
                            arrow = TrendArrow.Down,
                            trend = TrendDirection.OnTrack,
                        ),
                        Subject.Photos to SubjectSummary(subject = Subject.Photos),
                        Subject.Measurements to SubjectSummary(
                            subject = Subject.Measurements,
                            value = "88.0",
                            unit = "cm waist",
                            preview = SubjectPreview.Line(listOf(90.0, 89.4, 88.8, 88.0)),
                            footnote = "1.5 cm · 3 parts",
                            arrow = TrendArrow.Down,
                            trend = TrendDirection.OnTrack,
                        ),
                    ),
                    expanded = false,
                    onToggle = {},
                    onOpen = {},
                    onHint = {},
                )
            }
        }
    }
}

/** Nothing tracked in the whole family — one row, not four dashed cards. */
@PreviewLightDark
@Composable
private fun GroupSectionCollapsedPreview() {
    AppTheme {
        Surface {
            Column(modifier = Modifier.padding(16.dp)) {
                GroupSection(
                    group = SubjectGroup.Wellbeing,
                    summaries = subjectsIn(SubjectGroup.Wellbeing).associateWith { SubjectSummary(it) },
                    cycleTracking = true,
                    expanded = false,
                    onToggle = {},
                    onOpen = {},
                    onHint = {},
                )
            }
        }
    }
}
