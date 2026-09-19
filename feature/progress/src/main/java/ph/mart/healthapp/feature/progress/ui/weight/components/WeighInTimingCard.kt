package ph.mart.healthapp.feature.progress.ui.weight.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import ph.mart.healthapp.core.data.profile.UnitSystem
import ph.mart.healthapp.core.data.profile.kgToDisplayUnit
import ph.mart.healthapp.core.data.profile.weightUnitLabel
import ph.mart.healthapp.core.designsystem.component.AppCard
import ph.mart.healthapp.core.designsystem.component.formatMinuteOfDay
import ph.mart.healthapp.core.designsystem.component.formatOneDecimal
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.progress.R
import ph.mart.healthapp.feature.progress.ui.weight.WeighInTimeSplit

/**
 * What the clock is doing to this user's scale — see
 * [weighInTimeSplit][ph.mart.healthapp.feature.progress.ui.weight.weighInTimeSplit] for what the
 * app is willing to say here and why weight is allowed on both sides of it.
 *
 * An ordinary [AppCard] and no status dot, [PatternsCard][ph.mart.healthapp.feature.progress.ui.progress.components.PatternsCard]'s
 * reasoning: a dot means on-track, and a comparison is not a verdict. Null draws nothing at all
 * rather than a card explaining its own absence.
 */
@Composable
internal fun WeighInTimingCard(split: WeighInTimeSplit?, unit: UnitSystem, modifier: Modifier = Modifier) {
    if (split == null) return
    val delta = abs(split.deltaKg).kgToDisplayUnit(unit)
    AppCard(modifier = modifier) {
        Text(
            text = stringResource(R.string.progress_weight_timing_title),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 12.dp),
        )
        Text(
            // Both counts ride in the sentence, PatternsCard's rule: "9 against 7" is what tells
            // the reader how much to trust the line.
            text = stringResource(
                if (split.deltaKg >= 0) R.string.progress_weight_timing_above else R.string.progress_weight_timing_below,
                formatMinuteOfDay(split.lateFromMinute),
                formatOneDecimal(delta),
                unit.weightUnitLabel(),
                split.lateDays,
                split.earlyDays,
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = stringResource(R.string.progress_weight_timing_note),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 12.dp),
        )
    }
}

@PreviewLightDark
@Composable
private fun WeighInTimingCardPreview() {
    AppTheme {
        Surface {
            WeighInTimingCard(
                split = WeighInTimeSplit(
                    earlyDays = 8,
                    lateDays = 11,
                    earlyAvgKg = 76.2,
                    lateAvgKg = 76.9,
                    lateFromMinute = 13 * 60 + 40,
                ),
                unit = UnitSystem.Metric,
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}
