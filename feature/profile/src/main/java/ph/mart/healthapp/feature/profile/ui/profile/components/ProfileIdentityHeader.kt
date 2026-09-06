package ph.mart.healthapp.feature.profile.ui.profile.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import ph.mart.healthapp.core.data.profile.ActivityLevel
import ph.mart.healthapp.core.data.profile.Goal
import ph.mart.healthapp.core.data.profile.Profile
import ph.mart.healthapp.core.data.profile.Sex
import ph.mart.healthapp.core.data.profile.TREND_ARROW_DEADBAND_KG
import ph.mart.healthapp.core.data.profile.TrendDirection
import ph.mart.healthapp.core.data.profile.UnitSystem
import ph.mart.healthapp.core.data.profile.cmToDisplayUnit
import ph.mart.healthapp.core.data.profile.goalRelativeTrend
import ph.mart.healthapp.core.data.profile.kgToDisplayUnit
import ph.mart.healthapp.core.data.profile.lengthUnitLabel
import ph.mart.healthapp.core.data.profile.trendVsSevenDaysAgo
import ph.mart.healthapp.core.data.profile.weightUnitLabel
import ph.mart.healthapp.core.data.progress.WeightEntry
import ph.mart.healthapp.core.designsystem.component.MascotAvatar
import ph.mart.healthapp.core.designsystem.component.MascotState
import ph.mart.healthapp.core.designsystem.icon.AppIcons
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.core.designsystem.theme.tabularNums
import ph.mart.healthapp.feature.profile.R
import ph.mart.healthapp.feature.profile.ui.shared.components.IconTile
import ph.mart.healthapp.feature.profile.ui.shared.formatBodyValue
import ph.mart.healthapp.feature.profile.ui.shared.headline
import ph.mart.healthapp.feature.profile.ui.shared.label

/**
 * The one block on Profile that is about the *person*: who the app thinks you are, and what it is
 * aiming you at. Everything it prints was already stored and none of it was shown anywhere after
 * onboarding — that gap is what the redesign exists to close, and the whole block is the doorway to
 * About you, where it becomes editable.
 *
 * `surfaceContainerHigh` at 28dp, once per screen: the top tier of the three, so the subject of the
 * page does not read like the fifth card down it.
 *
 * The weight it shows is the *logged* one — the profile's own `weightKg` is the onboarding figure
 * and stops being true the first time anyone steps on a scale, so [trendVsSevenDaysAgo] falls back
 * to it only when the log is empty. The trend is goal-relative, from the same `:core:data` call
 * Home's `WeightMetricCard` and Progress's weight row make: down is not "good", it is good for a
 * Lose goal. And it never rides on colour alone — the arrow says which way and the words say
 * whether that is the way you asked for.
 */
@Composable
internal fun ProfileIdentityHeader(
    profile: Profile,
    weightEntries: List<WeightEntry>,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val unit = profile.preferredUnit
    val trend = weightEntries.trendVsSevenDaysAgo(profile.weightKg)

    // Two questions, deliberately not one: the arrow is the delta's direction, the colour is the
    // goal's verdict. A movement under the deadband is real but too small to call either way, so it
    // takes the flat glyph and the neutral tone rather than a confident arrow.
    val moved = trend.hasPrior && abs(trend.deltaKg) >= TREND_ARROW_DEADBAND_KG
    val direction = if (moved) goalRelativeTrend(profile.goal, trend.deltaKg) else TrendDirection.Neutral

    Surface(
        onClick = onEdit,
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(16.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MascotAvatar(state = MascotState.Idle, size = 64.dp)
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        text = stringResource(profile.goal.headline()),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = stringResource(
                            R.string.profile_header_summary,
                            stringResource(profile.sex.label()),
                            profile.age,
                            formatBodyValue(profile.heightCm.cmToDisplayUnit(unit)),
                            unit.lengthUnitLabel(),
                            stringResource(profile.activityLevel.label()),
                        ),
                        style = MaterialTheme.typography.bodySmall.tabularNums,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                // Not a button — the whole block is the tap target, and a second one inside it would
                // be two ways to do one thing. It is the visible affordance and, because the tile is
                // not itself clickable, its description is what the block announces.
                IconTile(
                    icon = AppIcons.Edit,
                    contentDescription = stringResource(R.string.profile_header_edit_a11y),
                    accent = false,
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
            ) {
                StatCell(label = stringResource(R.string.profile_header_now)) {
                    StatValue(
                        value = formatBodyValue(trend.currentKg.kgToDisplayUnit(unit)),
                        unit = unit.weightUnitLabel(),
                    )
                    TrendLine(
                        deltaKg = trend.deltaKg,
                        hasPrior = trend.hasPrior,
                        moved = moved,
                        direction = direction,
                        unit = unit,
                    )
                }
                // No target set is a real state, not a zero: Progress's goal line and Home's
                // projection hide together with this cell rather than drawing against a guess.
                profile.targetWeightKg?.let { targetKg ->
                    VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    StatCell(label = stringResource(R.string.profile_header_target)) {
                        StatValue(
                            value = formatBodyValue(targetKg.kgToDisplayUnit(unit)),
                            unit = unit.weightUnitLabel(),
                        )
                        Text(
                            text = stringResource(
                                R.string.profile_header_to_go,
                                formatBodyValue(abs(trend.currentKg - targetKg).kgToDisplayUnit(unit)),
                                unit.weightUnitLabel(),
                            ),
                            style = MaterialTheme.typography.labelSmall.tabularNums,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RowScope.StatCell(label: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.weight(1f)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        content()
    }
}

@Composable
private fun StatValue(value: String, unit: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.Bottom) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge.tabularNums,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = unit,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 2.dp),
        )
    }
}

/** Arrow **and** words, in that order: the glyph carries the direction, the sentence carries the
 * verdict, and the colour only ever agrees with something already written down. */
@Composable
private fun TrendLine(
    deltaKg: Double,
    hasPrior: Boolean,
    moved: Boolean,
    direction: TrendDirection,
    unit: UnitSystem,
) {
    if (!hasPrior) {
        Text(
            text = stringResource(R.string.profile_header_no_prior),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }
    val color = when (direction) {
        TrendDirection.OnTrack -> MaterialTheme.colorScheme.primary
        TrendDirection.OffTrack -> MaterialTheme.colorScheme.error
        TrendDirection.Neutral -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val verdict = stringResource(
        when {
            !moved -> R.string.profile_header_steady
            direction == TrendDirection.OnTrack -> R.string.profile_header_on_track
            direction == TrendDirection.OffTrack -> R.string.profile_header_off_track
            else -> R.string.profile_header_changed
        },
    )
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = when {
                !moved -> AppIcons.TrendFlat
                deltaKg < 0 -> AppIcons.TrendDown
                else -> AppIcons.TrendUp
            },
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(14.dp),
        )
        Text(
            text = stringResource(
                R.string.profile_header_trend,
                formatBodyValue(abs(deltaKg).kgToDisplayUnit(unit)),
                unit.weightUnitLabel(),
                verdict,
            ),
            style = MaterialTheme.typography.labelSmall.tabularNums,
            color = color,
        )
    }
}

private fun previewProfile() = Profile(
    sex = Sex.Male,
    age = 26,
    heightCm = 170.0,
    weightKg = 82.7,
    activityLevel = ActivityLevel.Moderate,
    goal = Goal.Lose,
    targetWeightKg = 75.0,
)

private fun previewEntries() = listOf(
    WeightEntry(dateEpochDay = 20_000, weightKg = 83.1),
    WeightEntry(dateEpochDay = 20_007, weightKg = 82.7),
)

@PreviewLightDark
@Composable
private fun ProfileIdentityHeaderPreview() {
    AppTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            ProfileIdentityHeader(
                profile = previewProfile(),
                weightEntries = previewEntries(),
                onEdit = {},
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

/** No target and no prior weigh-in: the Target cell and its rule are gone rather than zeroed, and
 * the trend line says why it has nothing to report. */
@PreviewLightDark
@Composable
private fun ProfileIdentityHeaderBarePreview() {
    AppTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            ProfileIdentityHeader(
                profile = previewProfile().copy(targetWeightKg = null, goal = Goal.Maintain),
                weightEntries = emptyList(),
                onEdit = {},
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}
