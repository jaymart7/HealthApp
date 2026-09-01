package ph.mart.healthapp.feature.progress.ui.achievement.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.data.fasting.FastSession
import ph.mart.healthapp.core.data.profile.UnitSystem
import ph.mart.healthapp.core.data.profile.kgToDisplayUnit
import ph.mart.healthapp.core.data.profile.weightUnitLabel
import ph.mart.healthapp.core.data.streak.streakStats
import ph.mart.healthapp.core.data.todayEpochDay
import ph.mart.healthapp.core.designsystem.component.AppCard
import ph.mart.healthapp.core.designsystem.component.BadgeDot
import ph.mart.healthapp.core.designsystem.component.FullScreenState
import ph.mart.healthapp.core.designsystem.component.MascotAvatar
import ph.mart.healthapp.core.designsystem.component.MascotState
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.core.designsystem.theme.tabularNums
import ph.mart.healthapp.feature.progress.ui.achievement.BadgeFamily
import ph.mart.healthapp.feature.progress.ui.achievement.BadgeGroup
import ph.mart.healthapp.feature.progress.ui.achievement.badgeGroups
import ph.mart.healthapp.feature.progress.ui.progress.ProgressUiState

/**
 * Every badge in the app on one surface. The tab takes no `ProgressScreenState` — unlike the chart
 * tabs it has no range to slice and no sheet to open.
 *
 * The copy lives here rather than in the derivation, the division `WeeklyRecapCard` already draws:
 * `:core:data`-shaped folds count, the card names.
 */
@Composable
internal fun AchievementsTabContent(uiState: ProgressUiState) {
    if (uiState.activeDays.isEmpty()) {
        FullScreenState(
            icon = { MascotAvatar(state = MascotState.Idle, size = 64.dp) },
            heading = "No badges yet",
            body = "Log anything — a meal, a glass of water, a workout — and the first one lights up.",
        )
        return
    }

    val groups = badgeGroups(
        // Read here rather than at flow-construction time, so the streak can't freeze at whatever
        // day the app was opened — HomeViewModel's reason for doing the same.
        streak = uiState.activeDays.streakStats(todayEpochDay()),
        weightProgressKg = uiState.weightProgressKg,
        workoutCount = uiState.exerciseEntries.size,
        fasts = uiState.fastSessions,
        photoCount = uiState.photos.size,
    )
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        groups.forEach { group -> BadgeGroupCard(group = group, unit = uiState.preferredUnit) }
    }
}

@Composable
private fun BadgeGroupCard(group: BadgeGroup, unit: UnitSystem) {
    AppCard {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
        ) {
            Text(
                text = titleFor(group.family),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "${group.earnedCount} of ${group.tiers.size}",
                style = MaterialTheme.typography.titleSmall.tabularNums,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            group.tiers.forEach { tier ->
                BadgeDot(
                    label = tierLabel(group.family, tier, unit),
                    earned = group.current >= tier,
                    description = descriptionFor(group.family, tier, unit),
                )
            }
        }

        Text(
            text = captionFor(group, unit),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 12.dp),
        )
    }
}

internal fun titleFor(family: BadgeFamily): String = when (family) {
    BadgeFamily.Streak -> "Streak"
    BadgeFamily.DaysLogged -> "Days logged"
    BadgeFamily.WeightMoved -> "Toward your goal"
    BadgeFamily.Workouts -> "Workouts"
    BadgeFamily.Fasts -> "Fasts completed"
    BadgeFamily.LongestFast -> "Longest fast"
    BadgeFamily.Photos -> "Progress photos"
}

/** The weight tiers are kg-native (see [ph.mart.healthapp.core.data.streak.WeightBadge]), so
 * imperial reads 4 / 11 / 22 rather than a round 5 / 10 / 20. */
internal fun tierLabel(family: BadgeFamily, tier: Int, unit: UnitSystem): String =
    if (family == BadgeFamily.LongestFast) "${tier}h" else tierNumber(family, tier, unit)

/** The bare figure, no unit suffix — what a caption puts its own noun after. */
internal fun tierNumber(family: BadgeFamily, tier: Int, unit: UnitSystem): String =
    if (family == BadgeFamily.WeightMoved) "%.0f".format(tier.toDouble().kgToDisplayUnit(unit)) else tier.toString()

private fun nounFor(family: BadgeFamily, unit: UnitSystem): String = when (family) {
    BadgeFamily.Streak, BadgeFamily.DaysLogged -> "day"
    BadgeFamily.WeightMoved -> unit.weightUnitLabel()
    BadgeFamily.Workouts -> "workout"
    BadgeFamily.Fasts -> "fast"
    BadgeFamily.LongestFast -> "hour"
    BadgeFamily.Photos -> "photo"
}

/** "11 lb badge", "16-hour badge" — the dot itself is a bare number, so the whole of what it
 * means has to live in the semantics. */
private fun descriptionFor(family: BadgeFamily, tier: Int, unit: UnitSystem): String {
    val label = tierNumber(family, tier, unit)
    val noun = nounFor(family, unit)
    return if (family == BadgeFamily.WeightMoved) "$label $noun badge" else "$label-$noun badge"
}

/**
 * First matching rule wins, the shape Home's `captionFor` and `insightFor` share. Never names what
 * is missing beyond the next threshold — the same reason the weekly recap has a best day and
 * deliberately no worst one.
 */
internal fun captionFor(group: BadgeGroup, unit: UnitSystem): String {
    val next = group.next ?: return "Every badge earned."
    val noun = nounFor(group.family, unit)
    // Only the fasts family has a tier of one, and "1 more fast to your 1-fast badge" is a
    // sentence no one should read.
    if (next == 1) return "Your first $noun earns a badge."
    return when (group.family) {
        // Neither is a count that climbs one at a time: "9 more hours" would read as nine more
        // fasts, and the kilograms moved are floored here, so a remainder would be a rounded lie.
        BadgeFamily.LongestFast -> "A ${tierLabel(group.family, next, unit)} fast earns the next badge."
        BadgeFamily.WeightMoved -> "Reach ${tierNumber(group.family, next, unit)} $noun for the next badge."
        else -> {
            val remaining = next - group.current
            val plural = if (remaining == 1) noun else "${noun}s"
            "$remaining more $plural to your ${next}-$noun badge."
        }
    }
}

@PreviewLightDark
@Composable
private fun AchievementsTabPreview() {
    val today = todayEpochDay()
    val hour = 3_600_000L
    AppTheme {
        AchievementsTabContent(
            uiState = ProgressUiState(
                activeDays = (today - 30..today).toSet(),
                weightProgressKg = 5.2,
                fastSessions = listOf(
                    FastSession(startMillis = 0, endMillis = 17 * hour),
                    FastSession(startMillis = 0, endMillis = 14 * hour),
                ),
                preferredUnit = UnitSystem.Metric,
            ),
        )
    }
}

@PreviewLightDark
@Composable
private fun AchievementsTabEmptyPreview() {
    AppTheme {
        AchievementsTabContent(uiState = ProgressUiState())
    }
}
