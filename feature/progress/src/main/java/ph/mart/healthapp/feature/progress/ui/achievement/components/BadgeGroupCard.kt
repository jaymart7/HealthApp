package ph.mart.healthapp.feature.progress.ui.achievement.components

import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.data.Phrase
import ph.mart.healthapp.core.data.phrase
import ph.mart.healthapp.core.data.plural
import ph.mart.healthapp.core.data.profile.UnitSystem
import ph.mart.healthapp.core.data.profile.kgToDisplayUnit
import ph.mart.healthapp.core.data.profile.weightUnitLabel
import ph.mart.healthapp.core.data.resolve
import ph.mart.healthapp.core.designsystem.component.AppCard
import ph.mart.healthapp.core.designsystem.component.BadgeDot
import ph.mart.healthapp.core.designsystem.component.formatDecimals
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.core.designsystem.theme.tabularNums
import ph.mart.healthapp.feature.progress.R
import ph.mart.healthapp.feature.progress.ui.achievement.BadgeFamily
import ph.mart.healthapp.feature.progress.ui.achievement.BadgeGroup

/**
 * One badge family: its name, how many of its tiers are lit, the dots themselves, and a caption
 * naming the next one.
 *
 * The copy lives here rather than in the derivation, the division `RecapCard` already draws:
 * `:core:data`-shaped folds count, the card names.
 */
@Composable
internal fun BadgeGroupCard(group: BadgeGroup, unit: UnitSystem) {
    val resources = LocalResources.current
    AppCard {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
        ) {
            Text(
                text = stringResource(titleFor(group.family)),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(R.string.progress_badges_earned, group.earnedCount, group.tiers.size),
                style = MaterialTheme.typography.titleSmall.tabularNums,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            group.tiers.forEach { tier ->
                BadgeDot(
                    label = tierLabel(group.family, tier, unit),
                    earned = group.current >= tier,
                    description = descriptionFor(group.family, tier, unit).resolve(resources),
                )
            }
        }

        Text(
            text = captionFor(group, unit).resolve(resources),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 12.dp),
        )
    }
}

@StringRes
internal fun titleFor(family: BadgeFamily): Int = when (family) {
    BadgeFamily.Streak -> R.string.progress_badge_family_streak
    BadgeFamily.DaysLogged -> R.string.progress_badge_family_days_logged
    BadgeFamily.WeightMoved -> R.string.progress_badge_family_weight_moved
    BadgeFamily.Workouts -> R.string.progress_badge_family_workouts
    BadgeFamily.Fasts -> R.string.progress_badge_family_fasts
    BadgeFamily.LongestFast -> R.string.progress_badge_family_longest_fast
    BadgeFamily.Photos -> R.string.progress_badge_family_photos
}

/** The weight tiers are kg-native (see [ph.mart.healthapp.core.data.streak.WeightBadge]), so
 * imperial reads 4 / 11 / 22 rather than a round 5 / 10 / 20. */
internal fun tierLabel(family: BadgeFamily, tier: Int, unit: UnitSystem): String =
    if (family == BadgeFamily.LongestFast) "${tier}h" else tierNumber(family, tier, unit)

/** The bare figure, no unit suffix — what a caption puts its own noun after. */
internal fun tierNumber(family: BadgeFamily, tier: Int, unit: UnitSystem): String =
    if (family == BadgeFamily.WeightMoved) formatDecimals(tier.toDouble().kgToDisplayUnit(unit), decimals = 0) else tier.toString()

private fun nounFor(family: BadgeFamily, unit: UnitSystem): Phrase = when (family) {
    BadgeFamily.Streak, BadgeFamily.DaysLogged -> phrase(R.string.progress_badge_noun_day)
    BadgeFamily.WeightMoved -> Phrase.Raw(unit.weightUnitLabel())
    BadgeFamily.Workouts -> phrase(R.string.progress_badge_noun_workout)
    BadgeFamily.Fasts -> phrase(R.string.progress_badge_noun_fast)
    BadgeFamily.LongestFast -> phrase(R.string.progress_badge_noun_hour)
    BadgeFamily.Photos -> phrase(R.string.progress_badge_noun_photo)
}

/** "11 lb badge", "16-hour badge" — the dot itself is a bare number, so the whole of what it
 * means has to live in the semantics. One resource per noun, because the hyphenated compound is
 * English grammar a translation should be free to drop. */
private fun descriptionFor(family: BadgeFamily, tier: Int, unit: UnitSystem): Phrase {
    val label = tierNumber(family, tier, unit)
    return when (family) {
        BadgeFamily.WeightMoved -> phrase(R.string.progress_badge_desc_weight, label, unit.weightUnitLabel())
        BadgeFamily.Streak, BadgeFamily.DaysLogged -> phrase(R.string.progress_badge_desc_day, label)
        BadgeFamily.Workouts -> phrase(R.string.progress_badge_desc_workout, label)
        BadgeFamily.Fasts -> phrase(R.string.progress_badge_desc_fast, label)
        BadgeFamily.LongestFast -> phrase(R.string.progress_badge_desc_hour, label)
        BadgeFamily.Photos -> phrase(R.string.progress_badge_desc_photo, label)
    }
}

/**
 * First matching rule wins, the shape Home's `captionFor` and `insightFor` share. Never names what
 * is missing beyond the next threshold — the same reason the weekly recap has a best day and
 * deliberately no worst one.
 */
internal fun captionFor(group: BadgeGroup, unit: UnitSystem): Phrase {
    val next = group.next ?: return phrase(R.string.progress_badge_caption_all)
    // Only the fasts family has a tier of one, and "1 more fast to your 1-fast badge" is a
    // sentence no one should read.
    if (next == 1) return phrase(R.string.progress_badge_caption_first, nounFor(group.family, unit))
    return when (group.family) {
        // Neither is a count that climbs one at a time: "9 more hours" would read as nine more
        // fasts, and the kilograms moved are floored here, so a remainder would be a rounded lie.
        BadgeFamily.LongestFast -> phrase(R.string.progress_badge_caption_fast, tierLabel(group.family, next, unit))
        BadgeFamily.WeightMoved ->
            phrase(R.string.progress_badge_caption_weight, tierNumber(group.family, next, unit), unit.weightUnitLabel())
        else -> {
            val remaining = next - group.current
            plural(moreFor(group.family), remaining, remaining, next)
        }
    }
}

/** The counting families' captions, one plural each: the noun is inside the sentence twice. */
@PluralsRes
private fun moreFor(family: BadgeFamily): Int = when (family) {
    BadgeFamily.Workouts -> R.plurals.progress_badge_caption_more_workout
    BadgeFamily.Fasts -> R.plurals.progress_badge_caption_more_fast
    BadgeFamily.Photos -> R.plurals.progress_badge_caption_more_photo
    // Streak and DaysLogged; LongestFast and WeightMoved return before reaching here.
    else -> R.plurals.progress_badge_caption_more_day
}

@PreviewLightDark
@Composable
private fun BadgeGroupCardPreview() {
    AppTheme {
        Surface {
            Column(modifier = Modifier.padding(16.dp)) {
                BadgeGroupCard(
                    group = BadgeGroup(BadgeFamily.Streak, listOf(3, 7, 14, 30), current = 9),
                    unit = UnitSystem.Metric,
                )
            }
        }
    }
}
