package ph.mart.healthapp.feature.home.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
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
import ph.mart.healthapp.core.data.profile.UnitSystem
import ph.mart.healthapp.core.data.profile.kgToDisplayUnit
import ph.mart.healthapp.core.data.profile.weightUnitLabel
import ph.mart.healthapp.core.data.streak.StreakBadge
import ph.mart.healthapp.core.data.streak.StreakStats
import ph.mart.healthapp.core.data.streak.earnedBadges
import ph.mart.healthapp.core.data.streak.earnedWeightBadge
import ph.mart.healthapp.core.data.streak.nextBadge
import ph.mart.healthapp.core.designsystem.component.AppCard
import ph.mart.healthapp.core.designsystem.icon.AppIcons
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.core.designsystem.theme.Motion
import ph.mart.healthapp.core.designsystem.theme.tabularNums

/**
 * Consistency, not today's numbers. Every value is derived in `:core:data/streak` — this card
 * only formats. [weightProgressKg] is null for a Maintain goal (no direction to move) and is
 * hidden when it's not positive, so a bad week never gets its own line.
 */
@Composable
fun StreakCard(
    streak: StreakStats,
    weightProgressKg: Double?,
    unit: UnitSystem,
    modifier: Modifier = Modifier,
) {
    val earned = streak.earnedBadges()
    AppCard(modifier = modifier) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
        ) {
            Text(
                text = "Streak",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = AppIcons.Streak,
                    contentDescription = null,
                    tint = if (streak.current > 0) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    text = dayCountLabel(streak.current),
                    style = MaterialTheme.typography.titleSmall.tabularNums,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StreakBadge.entries.forEach { badge ->
                BadgeDot(days = badge.days, earned = badge in earned)
            }
        }

        Text(
            text = captionFor(streak),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 12.dp),
        )

        if (weightProgressKg != null && weightProgressKg > 0) {
            Text(
                text = weightLineFor(weightProgressKg, unit),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

/**
 * Filled means earned — off the *best* run ever, so it never goes dark again.
 *
 * The colour transition is not the streak celebration CLAUDE.md rules out, and it needs none of
 * the persisted "already celebrated" state that ruled the celebration out. `animateColorAsState`
 * does not animate on first composition: a badge already earned when Home opens simply draws
 * earned. It animates only a flip it actually witnesses — the moment the badge is won, while the
 * user is looking at it. Colour only; a scale pop here would cross the line.
 */
@Composable
private fun BadgeDot(days: Int, earned: Boolean) {
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
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(container)
            .clearAndSetSemantics {
                contentDescription = if (earned) "$days-day badge, earned" else "$days-day badge, not yet earned"
            },
    ) {
        Text(
            text = days.toString(),
            style = MaterialTheme.typography.labelMedium.tabularNums,
            color = content,
        )
    }
}

internal fun dayCountLabel(days: Int): String = if (days == 1) "1 day" else "$days days"

/** First matching rule wins, same shape as Home's `insightFor`. */
internal fun captionFor(streak: StreakStats): String {
    if (streak.current == 0) return "Log anything today to start a streak."
    val next = streak.nextBadge() ?: return "Best: ${dayCountLabel(streak.best)}."
    val remaining = next.days - streak.current
    return "${dayCountLabel(remaining)} to your ${next.days}-day badge."
}

internal fun weightLineFor(progressKg: Double, unit: UnitSystem): String {
    val moved = "%.1f %s".format(progressKg.kgToDisplayUnit(unit), unit.weightUnitLabel())
    val badge = earnedWeightBadge(progressKg)
        ?: return "$moved toward your goal."
    val threshold = "%.1f %s".format(badge.kg.kgToDisplayUnit(unit), unit.weightUnitLabel())
    return "$moved toward your goal · $threshold badge earned."
}

@PreviewLightDark
@Composable
private fun StreakCardPreview() {
    AppTheme {
        Surface {
            StreakCard(
                streak = StreakStats(current = 12, best = 31, totalDaysLogged = 74),
                weightProgressKg = 5.2,
                unit = UnitSystem.Metric,
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun StreakCardNoStreakPreview() {
    AppTheme {
        Surface {
            StreakCard(
                streak = StreakStats(current = 0, best = 4, totalDaysLogged = 9),
                weightProgressKg = null,
                unit = UnitSystem.Metric,
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}
