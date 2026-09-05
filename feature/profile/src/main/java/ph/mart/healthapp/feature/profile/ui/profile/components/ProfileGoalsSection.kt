package ph.mart.healthapp.feature.profile.ui.profile.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.data.profile.ActivityLevel
import ph.mart.healthapp.core.data.profile.CALORIE_FLOOR_WARNING
import ph.mart.healthapp.core.data.profile.CALORIE_TARGET_KCAL
import ph.mart.healthapp.core.data.profile.Goal
import ph.mart.healthapp.core.data.profile.Profile
import ph.mart.healthapp.core.data.profile.Sex
import ph.mart.healthapp.core.data.profile.belowFloor
import ph.mart.healthapp.core.data.profile.dailyTargets
import ph.mart.healthapp.core.designsystem.component.AppCard
import ph.mart.healthapp.core.designsystem.component.MacroBar
import ph.mart.healthapp.core.designsystem.component.MacroInputGroup
import ph.mart.healthapp.core.designsystem.component.NumericStepperField
import ph.mart.healthapp.core.designsystem.theme.AppTheme

/** How far one tap moves the calorie target. Same 50 the Confirm step nudges by. */
private const val CALORIE_STEP = 50

private fun ActivityLevel.label(): String = when (this) {
    ActivityLevel.Sedentary -> "Sedentary"
    ActivityLevel.Light -> "Light"
    ActivityLevel.Moderate -> "Moderate"
    ActivityLevel.Very -> "Very active"
}

/**
 * The numbers come straight from [dailyTargets], the same call Home and onboarding's Confirm step
 * make — editing them here only ever sets an override on top, never a second cached copy.
 *
 * [onResetTargets] is what keeps the nullable overrides usable: without it one nudge pins the
 * targets forever and a later weigh-in never moves them again. Its card is only drawn once
 * something has actually been overridden.
 */
@Composable
internal fun ProfileGoalsSection(
    profile: Profile,
    onSetCalorieTarget: (Int) -> Unit,
    onSetProteinTarget: (Int) -> Unit,
    onSetCarbsTarget: (Int) -> Unit,
    onSetFatTarget: (Int) -> Unit,
    onResetTargets: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val targets = profile.dailyTargets()
    val overridden = with(profile) {
        calorieOverrideKcal != null || proteinOverrideG != null || carbsOverrideG != null || fatOverrideG != null
    }
    SettingsSection(label = "Goals", modifier = modifier) {
        AppCard {
            // Stepper-only on purpose, unlike the macro fields below: the write is clamped to
            // CALORIE_TARGET_KCAL, and a clamped value re-seeds the typable field's text, so a
            // half-typed "1" would snap to "800" mid-keystroke. Don't pass onValueChange.
            NumericStepperField(
                label = "Calorie target",
                value = targets.calories.toString(),
                unitSuffix = "kcal",
                onIncrement = { onSetCalorieTarget((targets.calories + CALORIE_STEP).coerceAtMost(CALORIE_TARGET_KCAL.last)) },
                onDecrement = { onSetCalorieTarget((targets.calories - CALORIE_STEP).coerceAtLeast(CALORIE_TARGET_KCAL.first)) },
                error = stringResource(CALORIE_FLOOR_WARNING).takeIf { targets.belowFloor },
            )
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier.padding(vertical = 12.dp),
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Macro split",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                MacroBar(proteinG = targets.proteinG, carbsG = targets.carbsG, fatG = targets.fatG)
                MacroInputGroup(
                    proteinG = targets.proteinG,
                    carbsG = targets.carbsG,
                    fatG = targets.fatG,
                    onProteinChange = onSetProteinTarget,
                    onCarbsChange = onSetCarbsTarget,
                    onFatChange = onSetFatTarget,
                    step = 5,
                    showPercentages = true,
                )
            }
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier.padding(vertical = 12.dp),
            )
            SettingsRow(
                label = "Activity level",
                trailing = {
                    Text(
                        text = profile.activityLevel.label(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
            )
        }
        if (overridden) {
            AppCard(onClick = onResetTargets) {
                SettingsRow(
                    label = "Reset to calculated",
                    sublabel = "Goes back to the targets worked out from your height, weight, age and goal",
                )
            }
        }
    }
}

private fun previewProfile() = Profile(
    sex = Sex.Male,
    age = 26,
    heightCm = 170.0,
    weightKg = 75.5,
    activityLevel = ActivityLevel.Sedentary,
    goal = Goal.Maintain,
)

@PreviewLightDark
@Composable
private fun ProfileGoalsSectionPreview() {
    AppTheme {
        Surface {
            ProfileGoalsSection(
                profile = previewProfile(),
                onSetCalorieTarget = {},
                onSetProteinTarget = {},
                onSetCarbsTarget = {},
                onSetFatTarget = {},
                onResetTargets = {},
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

/** Overridden below the floor: the reset card appears and the calorie field carries its warning. */
@PreviewLightDark
@Composable
private fun ProfileGoalsSectionOverriddenPreview() {
    AppTheme {
        Surface {
            ProfileGoalsSection(
                profile = previewProfile().copy(calorieOverrideKcal = 1400, proteinOverrideG = 150),
                onSetCalorieTarget = {},
                onSetProteinTarget = {},
                onSetCarbsTarget = {},
                onSetFatTarget = {},
                onResetTargets = {},
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}
