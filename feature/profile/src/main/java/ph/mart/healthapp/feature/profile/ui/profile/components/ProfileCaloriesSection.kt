package ph.mart.healthapp.feature.profile.ui.profile.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import ph.mart.healthapp.core.data.profile.ActivityLevel
import ph.mart.healthapp.core.data.profile.CALORIE_FLOOR_WARNING
import ph.mart.healthapp.core.data.profile.CALORIE_TARGET_KCAL
import ph.mart.healthapp.core.data.profile.DailyTargets
import ph.mart.healthapp.core.data.profile.Goal
import ph.mart.healthapp.core.data.profile.Profile
import ph.mart.healthapp.core.data.profile.Sex
import ph.mart.healthapp.core.data.profile.belowFloor
import ph.mart.healthapp.core.data.profile.dailyTargets
import ph.mart.healthapp.core.designsystem.component.AppCard
import ph.mart.healthapp.core.designsystem.component.MacroBar
import ph.mart.healthapp.core.designsystem.component.MacroInputGroup
import ph.mart.healthapp.core.designsystem.icon.AppIcons
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.core.designsystem.theme.tabularNums
import ph.mart.healthapp.feature.profile.R
import ph.mart.healthapp.feature.profile.ui.shared.components.StepperButton

/** How far one tap moves the calorie target. Same 50 the Confirm step nudges by. */
private const val CALORIE_STEP = 50

/**
 * The day's calorie target and the split beneath it — the one card on Profile whose numbers other
 * screens spend, which is why it is the first thing under the header and why it is the only target
 * card that carries more than a row.
 *
 * The numbers come straight from [dailyTargets], the same call Home and onboarding's Confirm step
 * make: editing them here only ever sets an override on top, never a second cached copy. The
 * activity level that feeds it used to sit at the bottom of this card as read-only text; it is on
 * About you now, beside the goal it works with, where it is a choice rather than a fact.
 *
 * [onResetTargets] is what keeps the nullable overrides usable — without it one nudge pins the
 * targets forever and a later weigh-in never moves them again. It appears only once something has
 * actually been overridden, on the same press that overrides it.
 */
@Composable
internal fun ProfileCaloriesSection(
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
    AppCard(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = stringResource(R.string.profile_targets_calories_label),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = targets.calories.toString(),
                    style = MaterialTheme.typography.headlineLarge.tabularNums,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    // "kcal" is a unit symbol, not copy — see CLAUDE.md's localization rules.
                    text = "kcal",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                // Stepper-only on purpose, unlike the macro fields below: the write is clamped to
                // CALORIE_TARGET_KCAL, and a clamped value re-seeds a typable field's text, so a
                // half-typed "1" would snap to "800" mid-keystroke.
                StepperButton(
                    symbol = "−",
                    label = stringResource(R.string.profile_targets_calories_down),
                    onClick = {
                        onSetCalorieTarget((targets.calories - CALORIE_STEP).coerceAtLeast(CALORIE_TARGET_KCAL.first))
                    },
                )
                StepperButton(
                    symbol = "+",
                    label = stringResource(R.string.profile_targets_calories_up),
                    onClick = {
                        onSetCalorieTarget((targets.calories + CALORIE_STEP).coerceAtMost(CALORIE_TARGET_KCAL.last))
                    },
                )
            }

            // `error` text on the card, never an `errorContainer` surface: being under a target is a
            // caution the user may walk past, not a failure. It warns and the stepper keeps going.
            AnimatedVisibility(visible = targets.belowFloor) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(
                        imageVector = AppIcons.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = stringResource(CALORIE_FLOOR_WARNING),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = stringResource(R.string.profile_macro_split),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = stringResource(
                        R.string.profile_targets_macro_pcts,
                        targets.percentOf(targets.proteinG * 4),
                        targets.percentOf(targets.carbsG * 4),
                        targets.percentOf(targets.fatG * 9),
                    ),
                    style = MaterialTheme.typography.labelSmall.tabularNums,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
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

            AnimatedVisibility(visible = overridden) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Surface(
                        onClick = onResetTargets,
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.heightIn(min = 48.dp).padding(vertical = 4.dp),
                        ) {
                            Text(
                                text = stringResource(R.string.profile_reset_targets),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Text(
                                text = stringResource(R.string.profile_reset_targets_sub),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

/** The share of the day's energy one macro carries, rounded for display only — the grams are the
 * stored figure and nothing reads this back. */
private fun DailyTargets.percentOf(macroKcal: Int): Int {
    val total = (proteinG * 4 + carbsG * 4 + fatG * 9).coerceAtLeast(1)
    return (macroKcal * 100.0 / total).roundToInt()
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
private fun ProfileCaloriesSectionPreview() {
    AppTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            ProfileCaloriesSection(
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

/** Overridden below the floor: the warning and the reset row are both there, and the stepper still
 * decrements. */
@PreviewLightDark
@Composable
private fun ProfileCaloriesSectionOverriddenPreview() {
    AppTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            ProfileCaloriesSection(
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
