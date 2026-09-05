package ph.mart.healthapp.feature.onboarding.ui.onboarding.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.data.profile.ActivityLevel
import ph.mart.healthapp.core.data.profile.CALORIE_FLOOR_WARNING
import ph.mart.healthapp.core.data.profile.CALORIE_TARGET_KCAL
import ph.mart.healthapp.core.data.profile.Goal
import ph.mart.healthapp.core.data.profile.Sex
import ph.mart.healthapp.core.data.profile.belowFloor
import ph.mart.healthapp.core.data.profile.dailyTargets
import ph.mart.healthapp.core.designsystem.component.MacroBar
import ph.mart.healthapp.core.designsystem.component.MacroInputGroup
import ph.mart.healthapp.core.designsystem.component.MascotAvatar
import ph.mart.healthapp.core.designsystem.component.MascotSpeechBubble
import ph.mart.healthapp.core.designsystem.component.MascotState
import ph.mart.healthapp.core.designsystem.component.NumericStepperField
import ph.mart.healthapp.core.designsystem.component.PrimaryButton
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.onboarding.R
import ph.mart.healthapp.feature.onboarding.ui.onboarding.OnboardingForm
import ph.mart.healthapp.feature.onboarding.ui.shared.components.OnboardingStepHeader

/** Onboarding step 5 of 5. Targets are always computed live from [form] via [dailyTargets] — the
 * +/- steppers here only set an override on top, never a second cached number. */
@Composable
internal fun ConfirmTargetsScreen(
    form: OnboardingForm,
    isCelebrating: Boolean,
    onFormChange: (OnboardingForm) -> Unit,
    onFinish: () -> Unit,
    onBack: () -> Unit,
) {
    val profile = form.toProfileOrNull() ?: return
    val targets = profile.dailyTargets()
    val belowFloor = targets.belowFloor

    Column(modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            OnboardingStepHeader(currentStep = 6, totalSteps = 6, onBack = onBack)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MascotAvatar(state = if (isCelebrating) MascotState.Celebrating else MascotState.Idle, size = 64.dp)
                MascotSpeechBubble(
                    text = if (isCelebrating) {
                        stringResource(R.string.onboarding_confirm_bubble_done)
                    } else {
                        stringResource(R.string.onboarding_confirm_bubble)
                    },
                )
            }
            Text(
                text = stringResource(R.string.onboarding_confirm_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                NumericStepperField(
                    label = stringResource(R.string.onboarding_confirm_calories),
                    value = targets.calories.toString(),
                    unitSuffix = "kcal",
                    onIncrement = { onFormChange(form.copy(calorieOverrideKcal = targets.calories + 50)) },
                    onDecrement = { onFormChange(form.copy(calorieOverrideKcal = (targets.calories - 50).coerceAtLeast(CALORIE_TARGET_KCAL.first))) },
                )
                if (belowFloor) {
                    Text(
                        text = stringResource(CALORIE_FLOOR_WARNING),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.onboarding_confirm_macro_split),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                MacroBar(proteinG = targets.proteinG, carbsG = targets.carbsG, fatG = targets.fatG)
                MacroInputGroup(
                    proteinG = targets.proteinG,
                    carbsG = targets.carbsG,
                    fatG = targets.fatG,
                    onProteinChange = { onFormChange(form.copy(proteinOverrideG = it)) },
                    onCarbsChange = { onFormChange(form.copy(carbsOverrideG = it)) },
                    onFatChange = { onFormChange(form.copy(fatOverrideG = it)) },
                    step = 5,
                    showPercentages = true,
                )
            }
            Text(
                text = stringResource(R.string.onboarding_confirm_disclaimer),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Column(modifier = Modifier.padding(16.dp)) {
            PrimaryButton(label = stringResource(R.string.onboarding_confirm_cta), onClick = onFinish, enabled = !isCelebrating, modifier = Modifier.fillMaxWidth())
        }
    }
}

@PreviewLightDark
@Composable
private fun ConfirmTargetsScreenPreview() {
    AppTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            ConfirmTargetsScreen(
                form = OnboardingForm(
                    goal = Goal.Lose,
                    age = 28,
                    sex = Sex.Female,
                    activityLevel = ActivityLevel.Light,
                ),
                isCelebrating = false,
                onFormChange = {},
                onFinish = {},
                onBack = {},
            )
        }
    }
}
