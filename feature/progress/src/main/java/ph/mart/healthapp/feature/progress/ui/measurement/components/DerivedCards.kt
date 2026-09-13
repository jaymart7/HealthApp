package ph.mart.healthapp.feature.progress.ui.measurement.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.data.profile.UnitSystem
import ph.mart.healthapp.core.data.profile.kgToDisplayUnit
import ph.mart.healthapp.core.data.profile.weightUnitLabel
import ph.mart.healthapp.core.data.progress.WAIST_TO_HEIGHT_HEALTHY_MAX
import ph.mart.healthapp.core.data.progress.fatMassKgOf
import ph.mart.healthapp.core.data.progress.leanMassKgOf
import ph.mart.healthapp.core.data.progress.waistToHeightOf
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.progress.R
import ph.mart.healthapp.feature.progress.ui.progress.components.StatRow
import ph.mart.healthapp.feature.progress.ui.progress.components.StatRowsCard

/**
 * The two cards the Measurements page derives, above the rows they are derived from. Both draw
 * nothing at all until every input exists — there is no dash state, because a ratio with a side
 * missing is not a reading that hasn't been taken, it is a ratio that doesn't exist.
 */

/**
 * Waist against height.
 *
 * Two rows rather than one: the number means nothing without the published boundary beside it, and
 * [StatRow] carries a label and a value, not a caption. Both are `Neutral` — the boundary is
 * reported, never graded, which is the same line the absent fertile window draws on the Cycle page.
 */
@Composable
internal fun WaistToHeightCard(waistCm: Double?, heightCm: Double?, modifier: Modifier = Modifier) {
    val ratio = waistToHeightOf(waistCm ?: return, heightCm ?: return) ?: return
    StatRowsCard(
        rows = listOf(
            StatRow(
                label = stringResource(R.string.progress_measurement_waist_height),
                value = formatRatio(ratio),
            ),
            StatRow(
                label = stringResource(R.string.progress_measurement_waist_height_healthy),
                value = stringResource(
                    R.string.progress_measurement_waist_height_max,
                    formatRatio(WAIST_TO_HEIGHT_HEALTHY_MAX),
                ),
            ),
        ),
        modifier = modifier.padding(bottom = 8.dp),
    )
}

/**
 * The only thing on the page that reads a weigh-in: the newest body fat reading against the newest
 * weight, split into what is fat and what is not. Same rule as the card above — both rows or
 * neither, and no band or target beside them, because there is no one published healthy body fat
 * range to name the way [BmiCategory][ph.mart.healthapp.core.data.progress.BmiCategory] names the
 * WHO bands.
 *
 * These two *do* convert: a mass is a mass, where the ratio above is the same number in either
 * unit system.
 */
@Composable
internal fun BodyCompositionCard(
    bodyFatPercent: Double?,
    weightKg: Double?,
    unit: UnitSystem,
    modifier: Modifier = Modifier,
) {
    val percent = bodyFatPercent ?: return
    val weight = weightKg ?: return
    val fatKg = fatMassKgOf(weight, percent) ?: return
    val leanKg = leanMassKgOf(weight, percent) ?: return

    StatRowsCard(
        rows = listOf(
            StatRow(
                label = stringResource(R.string.progress_measurement_fat_mass),
                value = stringResource(
                    R.string.progress_measurement_value,
                    formatMeasurement(fatKg.kgToDisplayUnit(unit)),
                    unit.weightUnitLabel(),
                ),
            ),
            StatRow(
                label = stringResource(R.string.progress_measurement_lean_mass),
                value = stringResource(
                    R.string.progress_measurement_value,
                    formatMeasurement(leanKg.kgToDisplayUnit(unit)),
                    unit.weightUnitLabel(),
                ),
            ),
        ),
        modifier = modifier.padding(bottom = 8.dp),
    )
}

/** Two decimals always — the whole figure lives between 0.4 and 0.6, so a dropped one would put
 * every reading on the boundary it is being compared against. */
private fun formatRatio(value: Double): String = "%.2f".format(value)

@PreviewLightDark
@Composable
private fun DerivedCardsPreview() {
    AppTheme {
        Surface {
            Column(modifier = Modifier.padding(16.dp)) {
                WaistToHeightCard(waistCm = 85.5, heightCm = 178.0)
                BodyCompositionCard(bodyFatPercent = 18.0, weightKg = 82.0, unit = UnitSystem.Metric)
            }
        }
    }
}
