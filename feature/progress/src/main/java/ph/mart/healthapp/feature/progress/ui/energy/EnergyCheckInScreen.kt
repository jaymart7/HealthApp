package ph.mart.healthapp.feature.progress.ui.energy

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import kotlin.math.abs
import ph.mart.healthapp.core.data.profile.CALORIE_FLOOR_WARNING
import ph.mart.healthapp.core.data.profile.EnergyCheckIn
import ph.mart.healthapp.core.data.profile.EnergyEstimate
import ph.mart.healthapp.core.data.profile.MIN_CHECKIN_LOGGED_DAYS
import ph.mart.healthapp.core.data.profile.MIN_CHECKIN_WEIGH_INS
import ph.mart.healthapp.core.data.profile.MIN_MEANINGFUL_DELTA_KCAL
import ph.mart.healthapp.core.data.profile.UnitSystem
import ph.mart.healthapp.core.data.profile.kgToDisplayUnit
import ph.mart.healthapp.core.data.profile.weightUnitLabel
import ph.mart.healthapp.core.designsystem.component.AppCard
import ph.mart.healthapp.core.designsystem.component.PrimaryButton
import ph.mart.healthapp.core.designsystem.component.SecondaryButton
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.progress.ui.progress.components.Note
import ph.mart.healthapp.feature.progress.ui.weight.components.StatCell
import ph.mart.healthapp.feature.progress.ui.weight.components.formatKg

/**
 * What the formula got wrong, and the one tap that fixes it.
 *
 * A full-screen overlay inside the Progress tab rather than a route — `RecapScreen`'s call, and
 * for its reason: a route earns its own `ViewModelStoreOwner`, and with it a second copy of
 * `ProgressViewModel`'s twelve repositories. Everything shown is folded from state the tab already
 * holds; the only thing this flow owns is the write, which is [EnergyCheckInViewModel]'s.
 *
 * Every figure it was measured from is on the page. A screen that tells someone to eat 250 kcal
 * more a day without showing its working is asking to be believed rather than read.
 */
@Composable
internal fun EnergyCheckInScreen(
    checkIn: EnergyCheckIn,
    unit: UnitSystem,
    addExerciseToBudget: Boolean,
    onApply: (Int) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // An overlay, not a route: back has to close it rather than leave the Progress tab.
    val navigationState = rememberNavigationEventState(currentInfo = NavigationEventInfo.None)
    NavigationBackHandler(state = navigationState, onBackCompleted = onClose)

    val estimate = checkIn.estimate
    Surface(color = MaterialTheme.colorScheme.surface, modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Energy check-in",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Column(
                modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (estimate == null) {
                    NotYetCard(checkIn)
                } else {
                    MeasurementCard(checkIn, estimate, addExerciseToBudget)
                }
                EvidenceCard(checkIn, unit)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                // Applying drives the delta to zero, so the button takes itself away — which is
                // what lets this screen keep no "dismissed" state anywhere.
                if (estimate != null && abs(estimate.deltaKcal) >= MIN_MEANINGFUL_DELTA_KCAL) {
                    PrimaryButton(
                        label = "Use ${estimate.recommendedKcal} kcal",
                        onClick = { onApply(estimate.recommendedKcal) },
                        modifier = Modifier.weight(1f),
                    )
                }
                SecondaryButton(label = "Close", onClick = onClose, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun MeasurementCard(checkIn: EnergyCheckIn, estimate: EnergyEstimate, addExerciseToBudget: Boolean) {
    AppCard {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            StatCell(label = "You burn", value = "${estimate.maintenanceKcal}")
            StatCell(label = "Target now", value = "${checkIn.currentTargetKcal}")
            StatCell(label = "Suggested", value = "${estimate.recommendedKcal}")
        }
        Note(
            if (abs(estimate.deltaKcal) < MIN_MEANINGFUL_DELTA_KCAL) {
                "Your target is within ${MIN_MEANINGFUL_DELTA_KCAL} kcal of what this measures — close enough to leave alone."
            } else {
                "Measured from what you ate and what your weight actually did, not from the formula."
            },
        )
        // Warn, never block — the rule the manual target on Profile → Goals already follows.
        if (estimate.clampedToFloor) Note(stringResource(CALORIE_FLOOR_WARNING))
        // A measured burn already contains the training that produced it, so crediting a workout
        // on top of this target counts the same session twice. Named, not fixed: the switch is
        // the user's, and it lives on a screen this one has no business writing to.
        if (addExerciseToBudget) {
            Note(
                "This already includes your workouts. \"Add exercise to budget\" in Profile → " +
                    "Exercise adds them again on top — consider turning it off if you use this target.",
            )
        }
    }
}

/** The counts, not an empty page: a user who can't see what's missing has no reason to keep going. */
@Composable
private fun NotYetCard(checkIn: EnergyCheckIn) {
    AppCard {
        Text(
            text = "Not enough to measure yet",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Note(
            "It takes $MIN_CHECKIN_LOGGED_DAYS days of food logged and $MIN_CHECKIN_WEIGH_INS " +
                "weigh-ins spread over a fortnight in the last ${checkIn.windowDays} days. " +
                "You have ${checkIn.daysLogged} and ${checkIn.weighIns}.",
        )
    }
}

@Composable
private fun EvidenceCard(checkIn: EnergyCheckIn, unit: UnitSystem) {
    AppCard {
        Text(
            text = "The last ${checkIn.windowDays} days",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            StatCell(label = "Ate daily", value = "${checkIn.avgIntakeKcal}")
            StatCell(label = "Days logged", value = "${checkIn.daysLogged}")
            StatCell(
                label = "Weight",
                value = checkIn.estimate?.let { trendLabel(it.kgPerWeek, unit) } ?: "—",
            )
        }
        Note("The daily average is over the days with food logged, so a gap doesn't drag it down.")
    }
}

private fun trendLabel(kgPerWeek: Double, unit: UnitSystem): String {
    val value = formatKg(kgPerWeek.kgToDisplayUnit(unit))
    return "${if (kgPerWeek > 0) "+" else ""}$value ${unit.weightUnitLabel()}/wk"
}

private val readyCheckIn = EnergyCheckIn(
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
)

@PreviewLightDark
@Composable
private fun EnergyCheckInScreenPreview() {
    AppTheme {
        EnergyCheckInScreen(
            checkIn = readyCheckIn,
            unit = UnitSystem.Metric,
            addExerciseToBudget = true,
            onApply = {},
            onClose = {},
        )
    }
}

/** The two states with no button: nothing to change, and nothing measured yet. */
@PreviewLightDark
@Composable
private fun EnergyCheckInScreenSettledPreview() {
    AppTheme {
        EnergyCheckInScreen(
            checkIn = readyCheckIn.copy(
                currentTargetKcal = 2150,
                estimate = readyCheckIn.estimate?.copy(deltaKcal = 0, clampedToFloor = true),
            ),
            unit = UnitSystem.Imperial,
            addExerciseToBudget = false,
            onApply = {},
            onClose = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun EnergyCheckInScreenNotYetPreview() {
    AppTheme {
        EnergyCheckInScreen(
            checkIn = EnergyCheckIn(
                windowDays = 28,
                daysLogged = 9,
                weighIns = 1,
                avgIntakeKcal = 1980,
                currentTargetKcal = 1900,
                estimate = null,
            ),
            unit = UnitSystem.Metric,
            addExerciseToBudget = false,
            onApply = {},
            onClose = {},
        )
    }
}
