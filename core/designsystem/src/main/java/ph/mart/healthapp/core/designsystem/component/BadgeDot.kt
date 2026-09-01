package ph.mart.healthapp.core.designsystem.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.core.designsystem.theme.Motion
import ph.mart.healthapp.core.designsystem.theme.tabularNums

/**
 * One earned-or-not threshold marker. Filled means earned — every caller scores it off a *best*
 * figure, never a current one, so a dot never goes dark again.
 *
 * Started life private inside Home's `StreakCard`; promoted here once Progress's Badges tab drew
 * the identical dot, per CLAUDE.md's "used in ≥2 screens → :core:designsystem, never duplicated"
 * rule — the same path [AppCard] took.
 *
 * The colour transition is not the streak celebration CLAUDE.md rules out, and it needs none of
 * the persisted "already celebrated" state that ruled the celebration out. `animateColorAsState`
 * does not animate on first composition: a badge already earned when the screen opens simply draws
 * earned. It animates only a flip it actually witnesses — the moment the badge is won, while the
 * user is looking at it. Colour only; a scale pop here would cross the line.
 *
 * [description] names the badge ("7-day badge", "25 photos badge"); the earned state is appended
 * here so no caller can describe a dot without saying whether it's lit.
 */
@Composable
fun BadgeDot(label: String, earned: Boolean, description: String, modifier: Modifier = Modifier) {
    val spec = tween<Color>(durationMillis = Motion.State, easing = Motion.Standard)
    val container by animateColorAsState(
        targetValue = if (earned) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        animationSpec = spec,
        label = "badgeContainer",
    )
    val content by animateColorAsState(
        targetValue = if (earned) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = spec,
        label = "badgeContent",
    )
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(container)
            .clearAndSetSemantics {
                contentDescription = "$description, ${if (earned) "earned" else "not yet earned"}"
            },
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.tabularNums,
            color = content,
        )
    }
}

@PreviewLightDark
@Composable
private fun BadgeDotPreview() {
    AppTheme {
        Surface {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(16.dp),
            ) {
                BadgeDot(label = "3", earned = true, description = "3-day badge")
                BadgeDot(label = "7", earned = true, description = "7-day badge")
                BadgeDot(label = "14", earned = false, description = "14-day badge")
                BadgeDot(label = "100", earned = false, description = "100-day badge")
            }
        }
    }
}
