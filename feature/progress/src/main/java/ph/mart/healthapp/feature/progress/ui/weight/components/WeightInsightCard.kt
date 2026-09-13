package ph.mart.healthapp.feature.progress.ui.weight.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import ph.mart.healthapp.core.data.profile.EnergyCheckIn
import ph.mart.healthapp.core.data.profile.EnergyEstimate
import ph.mart.healthapp.core.data.profile.MIN_MEANINGFUL_DELTA_KCAL
import ph.mart.healthapp.core.data.profile.UnitSystem
import ph.mart.healthapp.core.data.profile.kgToDisplayUnit
import ph.mart.healthapp.core.data.profile.weightUnitLabel
import ph.mart.healthapp.core.data.progress.GoalProjection
import ph.mart.healthapp.core.data.progress.PROJECTION_WINDOW_DAYS
import ph.mart.healthapp.core.designsystem.component.AIInsightCard
import ph.mart.healthapp.core.designsystem.component.goalProjectionLine
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.progress.R

/**
 * The Weight page's one `tertiaryContainer` card, and the only insight card in the app fed by two
 * sources — which is what let the standalone projection and energy-check-in cards go.
 *
 * The projection takes the headline when there is one. Without a target weight there is no
 * projection at all, and the check-in takes it instead: a measured maintenance is a figure that
 * stands on its own, and it is the reason the card doesn't simply vanish for anyone who never set a
 * goal weight. The check-in reports what it is still missing rather than going quiet, because a
 * card that says nothing gives nobody a reason to keep weighing in.
 */
@Composable
internal fun ColumnScope.WeightInsightCard(
    checkIn: EnergyCheckIn?,
    projection: GoalProjection?,
    unit: UnitSystem,
    onOpen: () -> Unit,
) {
    val projectionLine = projection?.let {
        goalProjectionLine(
            goalWeightLabel = stringResource(
                R.string.progress_weight_value,
                formatKg(it.goalWeightKg.kgToDisplayUnit(unit)),
                unit.weightUnitLabel(),
            ),
            targetEpochDay = it.targetEpochDay,
            reached = it.reached,
            windowDays = PROJECTION_WINDOW_DAYS,
        )
    }
    val estimate = checkIn?.estimate
    val checkInHeadline = when {
        checkIn == null -> null
        estimate == null -> stringResource(R.string.progress_weight_measuring)
        else -> stringResource(R.string.progress_weight_burning, estimate.maintenanceKcal)
    }
    val checkInNote = when {
        checkIn == null -> null
        estimate == null -> missingNote(checkIn)
        else -> deltaNote(estimate)
    }

    val headline = projectionLine ?: checkInHeadline ?: return
    val subline = when {
        // The projection already has the headline, so the check-in becomes the second line and
        // brings its own "tap to review" with it.
        projectionLine != null -> checkInHeadline?.let {
            stringResource(R.string.progress_weight_combined, it, checkInNote.orEmpty())
        }
        else -> checkInNote
    }
    AIInsightCard(
        text = headline,
        subline = subline,
        onClick = if (checkIn != null) onOpen else null,
        headlineStyle = MaterialTheme.typography.titleMedium,
    )
}

/** Both counts every time, so the user can tell which one is holding the measurement up. */
@Composable
private fun missingNote(checkIn: EnergyCheckIn) =
    pluralStringResource(
        R.plurals.progress_weight_checkin_progress,
        checkIn.weighIns,
        checkIn.daysLogged,
        checkIn.windowDays,
        checkIn.weighIns,
    )

@Composable
private fun deltaNote(estimate: EnergyEstimate): String {
    val delta = estimate.deltaKcal
    if (abs(delta) < MIN_MEANINGFUL_DELTA_KCAL) return stringResource(R.string.progress_weight_target_matches)
    return stringResource(
        if (delta > 0) R.string.progress_weight_target_under else R.string.progress_weight_target_over,
        abs(delta),
    )
}

/** The projection alone, which is what anyone with a goal weight and a fortnight of weigh-ins
 * sees. */
@PreviewLightDark
@Composable
private fun WeightInsightCardPreview() {
    AppTheme {
        Surface {
            Column(modifier = Modifier.padding(16.dp)) {
                WeightInsightCard(
                    checkIn = null,
                    projection = GoalProjection(
                        goalWeightKg = 82.0,
                        kgPerWeek = -0.4,
                        targetEpochDay = 20_720L,
                        reached = false,
                    ),
                    unit = UnitSystem.Metric,
                    onOpen = {},
                )
            }
        }
    }
}
