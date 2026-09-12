package ph.mart.healthapp.feature.home.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import ph.mart.healthapp.core.data.profile.TREND_ARROW_DEADBAND_KG
import ph.mart.healthapp.core.data.profile.UnitSystem
import ph.mart.healthapp.core.data.profile.kgToDisplayUnit
import ph.mart.healthapp.core.data.profile.weightUnitLabel
import ph.mart.healthapp.core.data.progress.PhotoWeightArc
import ph.mart.healthapp.core.designsystem.icon.AppIcons
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.home.R

/**
 * [daysSinceLastPhoto] null means no photo has ever been taken — the card says so in words rather
 * than printing a nonsense day count, which is why the value here is not always a number. "Take
 * one" opens the same Add photo sheet the FAB uses; the sheet is hosted by AppScaffold, so this is
 * a callback, not a cross-feature import.
 *
 * [arc] is what the run adds up to — the weight between the oldest and newest shot that carry one,
 * the same `weightArc()` the Progress tab's Photos card folds. It is **quiet**: the arrow carries
 * the direction and the line keeps `onSurfaceVariant`, because the weight card on this same screen
 * already says whether that movement is with the goal or against it, and a second verdict would be
 * Home grading one number twice.
 *
 * No status mark, for the reason above it: there is no photo *target*, so there is nothing to be on
 * or off track against.
 */
@Composable
fun ProgressPhotoReminderCard(
    daysSinceLastPhoto: Long?,
    photoCount: Int,
    arc: PhotoWeightArc?,
    unit: UnitSystem,
    onTakePhoto: () -> Unit,
    wide: Boolean,
    modifier: Modifier = Modifier,
) {
    MetricCard(
        label = stringResource(R.string.home_photo_title),
        value = when (daysSinceLastPhoto) {
            null -> stringResource(R.string.home_photo_none_value)
            0L -> stringResource(R.string.home_photo_today)
            else -> "$daysSinceLastPhoto"
        },
        unit = daysSinceLastPhoto?.takeIf { it > 0L }?.let {
            pluralStringResource(R.plurals.home_photo_days_unit, it.toInt())
        },
        wide = wide,
        modifier = modifier,
    ) {
        // Nothing shot yet: "None yet" over the button is the whole card, and a "0 shots" line
        // under it would only repeat the value in smaller type.
        if (photoCount > 0) {
            MetaText(
                text = pluralStringResource(R.plurals.home_photo_shots, photoCount, photoCount),
                sub = arc?.let {
                    pluralStringResource(
                        R.plurals.home_photo_arc,
                        it.days.toInt(),
                        formatWeight(abs(it.deltaKg).kgToDisplayUnit(unit)),
                        unit.weightUnitLabel(),
                        it.days.toInt(),
                    )
                },
                leading = arc?.takeIf { abs(it.deltaKg) >= TREND_ARROW_DEADBAND_KG }?.let { moved ->
                    {
                        Icon(
                            imageVector = if (moved.deltaKg < 0) AppIcons.TrendDown else AppIcons.TrendUp,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                },
            )
        }
        MetaButton(label = stringResource(R.string.home_photo_cta), onClick = onTakePhoto)
    }
}

@PreviewLightDark
@Composable
private fun ProgressPhotoReminderCardPreview() {
    val arc = PhotoWeightArc(deltaKg = -2.1, days = 92)
    AppTheme {
        Surface {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(16.dp)) {
                ProgressPhotoReminderCard(
                    daysSinceLastPhoto = 12,
                    photoCount = 4,
                    arc = arc,
                    unit = UnitSystem.Metric,
                    onTakePhoto = {},
                    wide = true,
                )
                // Paired, where the meta column is the card's own width rather than 120dp: the
                // same content has to read both ways, which is the whole of MetricCard's contract.
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    ProgressPhotoReminderCard(
                        daysSinceLastPhoto = 3,
                        photoCount = 4,
                        arc = arc,
                        unit = UnitSystem.Metric,
                        onTakePhoto = {},
                        wide = false,
                        modifier = Modifier.weight(1f),
                    )
                    // Shots logged without a weight on them: the count, and no arc under it.
                    ProgressPhotoReminderCard(
                        daysSinceLastPhoto = 3,
                        photoCount = 2,
                        arc = null,
                        unit = UnitSystem.Metric,
                        onTakePhoto = {},
                        wide = false,
                        modifier = Modifier.weight(1f),
                    )
                }
                ProgressPhotoReminderCard(
                    daysSinceLastPhoto = null,
                    photoCount = 0,
                    arc = null,
                    unit = UnitSystem.Metric,
                    onTakePhoto = {},
                    wide = true,
                )
            }
        }
    }
}
