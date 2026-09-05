package ph.mart.healthapp.feature.progress.ui.progress.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.designsystem.icon.AppIcons
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.core.designsystem.theme.tabularNums
import ph.mart.healthapp.feature.progress.R

/** Five dots is a progress bar you can count. More would be a chart; fewer would be a switch. */
private const val PROGRESS_DOTS = 5

private val RowMinHeight = 64.dp

/**
 * Badges, at the foot of the overview and deliberately **not** in a group grid: it is an
 * achievement list, not a trend, so a metric card promising a preview line would be a card that
 * lies about what is behind it. It is still one of the thirteen subjects and still opens its own
 * detail page.
 *
 * The dots are a rounded fifth of the earned ratio — [ph.mart.healthapp.core.designsystem.component.BadgeDot]
 * is the real thing and lives on the page behind this row, where each dot means one threshold.
 * These five mean nothing individually, which is why the count beside them carries the figure and
 * the dots carry no labels.
 */
@Composable
internal fun BadgesRow(
    earned: Int,
    total: Int,
    families: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val filled = if (total == 0) 0 else (earned * PROGRESS_DOTS + total / 2) / total
    // Resolved here because a semantics lambda cannot read a resource.
    val dotsDescription = stringResource(R.string.progress_badges_earned_description, earned, total)
    Surface(
        onClick = onClick,
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
                    text = stringResource(R.string.progress_badges_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(R.string.progress_badges_summary, earned, total, families),
                    style = MaterialTheme.typography.bodySmall.tabularNums,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    // One description for the strip: five dots read out one at a time is noise
                    // over a figure the line above already states.
                    .clearAndSetSemantics { contentDescription = dotsDescription },
            ) {
                repeat(PROGRESS_DOTS) { index ->
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(
                                if (index < filled) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.outlineVariant
                                },
                            ),
                    )
                }
            }
            Icon(
                imageVector = AppIcons.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun BadgesRowPreview() {
    AppTheme {
        Surface {
            Column(modifier = Modifier.padding(16.dp)) {
                BadgesRow(earned = 12, total = 40, families = 7, onClick = {})
                BadgesRow(earned = 0, total = 40, families = 7, onClick = {})
            }
        }
    }
}
