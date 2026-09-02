package ph.mart.healthapp.feature.progress.ui.energy.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import ph.mart.healthapp.core.data.profile.EnergyCheckIn
import ph.mart.healthapp.core.data.profile.EnergyEstimate
import ph.mart.healthapp.core.data.profile.MIN_MEANINGFUL_DELTA_KCAL
import ph.mart.healthapp.core.designsystem.component.AppCard
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.core.designsystem.theme.tabularNums
import ph.mart.healthapp.feature.progress.ui.progress.components.Note

/**
 * The door to the check-in, under [ph.mart.healthapp.feature.progress.ui.weight.components.GoalProjectionCard]
 * because it is the same subject: the weight trend that card projects is the trend this one
 * measures a maintenance from.
 *
 * Monochrome, like the projection card above it and for the same reason — a coloured verdict on a
 * calorie target reads as a judgement on the month. It reports either the measurement or, in one
 * line, what is still missing: a user who never sees why the card is quiet has no reason to keep
 * weighing in.
 */
@Composable
internal fun EnergyCheckInCard(
    checkIn: EnergyCheckIn,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AppCard(modifier = modifier, onClick = onOpen) {
        Text(
            text = "Energy check-in",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            val estimate = checkIn.estimate
            Text(
                text = if (estimate == null) {
                    "Measuring what you actually burn"
                } else {
                    "You're burning about ${estimate.maintenanceKcal} kcal a day"
                },
                style = MaterialTheme.typography.titleMedium.tabularNums,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Note(if (estimate == null) missingNote(checkIn) else deltaNote(estimate))
        }
    }
}

/** Both counts every time, so the user can tell which one is holding the measurement up. */
private fun missingNote(checkIn: EnergyCheckIn) =
    "${checkIn.daysLogged} of ${checkIn.windowDays} days logged · " +
        "${checkIn.weighIns} ${if (checkIn.weighIns == 1) "weigh-in" else "weigh-ins"}. Keep going."

private fun deltaNote(estimate: EnergyEstimate): String {
    val delta = estimate.deltaKcal
    if (abs(delta) < MIN_MEANINGFUL_DELTA_KCAL) return "Your calorie target already matches. Tap for the detail."
    return "Your target is ${abs(delta)} kcal ${if (delta > 0) "under" else "over"} what this suggests. Tap to review."
}

@PreviewLightDark
@Composable
private fun EnergyCheckInCardPreview() {
    AppTheme {
        Surface {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(16.dp),
            ) {
                EnergyCheckInCard(
                    checkIn = EnergyCheckIn(
                        windowDays = 28,
                        daysLogged = 24,
                        weighIns = 5,
                        avgIntakeKcal = 2100,
                        currentTargetKcal = 1900,
                        estimate = EnergyEstimate(
                            maintenanceKcal = 2650,
                            recommendedKcal = 2150,
                            deltaKcal = 250,
                            kgPerWeek = -0.5,
                            clampedToFloor = false,
                        ),
                    ),
                    onOpen = {},
                )
                // Already adjusted: the measurement stands, the call to action is gone.
                EnergyCheckInCard(
                    checkIn = EnergyCheckIn(
                        windowDays = 28,
                        daysLogged = 24,
                        weighIns = 5,
                        avgIntakeKcal = 2100,
                        currentTargetKcal = 2150,
                        estimate = EnergyEstimate(
                            maintenanceKcal = 2650,
                            recommendedKcal = 2150,
                            deltaKcal = 0,
                            kgPerWeek = -0.5,
                            clampedToFloor = false,
                        ),
                    ),
                    onOpen = {},
                )
                EnergyCheckInCard(
                    checkIn = EnergyCheckIn(
                        windowDays = 28,
                        daysLogged = 9,
                        weighIns = 1,
                        avgIntakeKcal = 1980,
                        currentTargetKcal = 1900,
                        estimate = null,
                    ),
                    onOpen = {},
                )
            }
        }
    }
}
