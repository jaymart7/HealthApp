package ph.mart.healthapp.feature.profile.ui.profile.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.data.fasting.FAST_GOAL_HOURS
import ph.mart.healthapp.core.data.health.STEP_GOAL_NUDGE
import ph.mart.healthapp.core.data.health.STEP_GOAL_STEPS
import ph.mart.healthapp.core.data.health.formatSteps
import ph.mart.healthapp.core.data.profile.UnitSystem
import ph.mart.healthapp.core.data.water.WATER_GOAL_GLASSES
import ph.mart.healthapp.core.data.water.waterVolumeLabel
import ph.mart.healthapp.core.designsystem.component.AppCard
import ph.mart.healthapp.core.designsystem.icon.AppIcons
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.profile.R
import ph.mart.healthapp.feature.profile.ui.shared.components.IconTile
import ph.mart.healthapp.feature.profile.ui.shared.components.StepperRow

/**
 * Water, fasting and steps: the three targets Mifflin–St Jeor has nothing to say about, so unlike
 * the calorie figure above nothing derives them and each one is simply a number you set.
 *
 * They were three sections holding one row each, which is what made a stepper touched once a year
 * look like a switch touched weekly. One card, three rows, one heading — and each one carries a
 * *derived* sublabel, because "8 glasses" means nothing until it says 1.6 L and a 16-hour fast
 * means nothing until it says how long that leaves to eat.
 *
 * All three are nudge-only and clamp at the edges rather than validating after the fact — the same
 * shape the calorie target uses, and for the same reason: a clamped write re-seeds a typed field
 * mid-keystroke. The step goal is deliberately not snapshotted per day; see `Profile.stepGoal`.
 */
@Composable
internal fun ProfileDayTargetsSection(
    waterGoalGlasses: Int,
    onSetWaterGoal: (Int) -> Unit,
    fastingGoalHours: Int,
    onSetFastingGoal: (Int) -> Unit,
    stepGoal: Int,
    onSetStepGoal: (Int) -> Unit,
    unit: UnitSystem,
    modifier: Modifier = Modifier,
) {
    AppCard(modifier = modifier, contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)) {
        StepperRow(
            label = stringResource(R.string.profile_daytargets_water),
            sublabel = stringResource(R.string.profile_daytargets_water_sub, waterVolumeLabel(waterGoalGlasses, unit)),
            value = "$waterGoalGlasses",
            unit = stringResource(R.string.profile_water_unit),
            leading = { IconTile(icon = AppIcons.Water, contentDescription = null, accent = false) },
            onIncrement = { onSetWaterGoal((waterGoalGlasses + 1).coerceAtMost(WATER_GOAL_GLASSES.last)) },
            onDecrement = { onSetWaterGoal((waterGoalGlasses - 1).coerceAtLeast(WATER_GOAL_GLASSES.first)) },
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        StepperRow(
            label = stringResource(R.string.profile_daytargets_fasting),
            sublabel = stringResource(R.string.profile_daytargets_fasting_sub, 24 - fastingGoalHours),
            value = "$fastingGoalHours",
            unit = stringResource(R.string.profile_fasting_unit),
            leading = { IconTile(icon = AppIcons.Timer, contentDescription = null, accent = false) },
            onIncrement = { onSetFastingGoal((fastingGoalHours + 1).coerceAtMost(FAST_GOAL_HOURS.last)) },
            onDecrement = { onSetFastingGoal((fastingGoalHours - 1).coerceAtLeast(FAST_GOAL_HOURS.first)) },
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        StepperRow(
            label = stringResource(R.string.profile_daytargets_steps),
            // ponytail: one static line rather than "Counted by Google Health" when the connection
            // is live. HealthSyncRepository.connection() is a suspend one-shot, so saying so would
            // cost this ViewModel a probe and a resume refresh for a sublabel. True either way.
            sublabel = stringResource(R.string.profile_daytargets_steps_sub),
            value = formatSteps(stepGoal),
            unit = stringResource(R.string.profile_step_unit),
            leading = { IconTile(icon = AppIcons.Steps, contentDescription = null, accent = false) },
            onIncrement = { onSetStepGoal((stepGoal + STEP_GOAL_NUDGE).coerceAtMost(STEP_GOAL_STEPS.last)) },
            onDecrement = { onSetStepGoal((stepGoal - STEP_GOAL_NUDGE).coerceAtLeast(STEP_GOAL_STEPS.first)) },
        )
    }
}

@PreviewLightDark
@Composable
private fun ProfileDayTargetsSectionPreview() {
    AppTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            ProfileDayTargetsSection(
                waterGoalGlasses = 8,
                onSetWaterGoal = {},
                fastingGoalHours = 16,
                onSetFastingGoal = {},
                stepGoal = 8_000,
                onSetStepGoal = {},
                unit = UnitSystem.Metric,
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}
