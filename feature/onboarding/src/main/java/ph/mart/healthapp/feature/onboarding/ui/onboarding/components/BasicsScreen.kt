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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import ph.mart.healthapp.core.data.profile.CM_PER_IN
import ph.mart.healthapp.core.data.profile.Goal
import ph.mart.healthapp.core.data.profile.KG_PER_LB
import ph.mart.healthapp.core.data.profile.Sex
import ph.mart.healthapp.core.data.profile.UnitSystem
import ph.mart.healthapp.core.data.profile.lengthUnitLabel
import ph.mart.healthapp.core.data.profile.round1
import ph.mart.healthapp.core.data.profile.weightUnitLabel
import ph.mart.healthapp.core.designsystem.component.NumericStepperField
import ph.mart.healthapp.core.designsystem.component.PrimaryButton
import ph.mart.healthapp.core.designsystem.component.SegmentedToggle
import ph.mart.healthapp.core.designsystem.component.TextButton
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.onboarding.R
import ph.mart.healthapp.feature.onboarding.ui.onboarding.OnboardingForm
import ph.mart.healthapp.feature.onboarding.ui.onboarding.clearOverrides
import ph.mart.healthapp.feature.onboarding.ui.shared.components.OnboardingStepHeader

/** Onboarding step 2 of 5. Age/height/weight steps use only +/- buttons (matches the prototype —
 * there is no typed entry), so age is always clamped into 13..100 and never needs an inline
 * error. Target weight only shows once a goal other than Maintain is picked (step 1). */
@Composable
internal fun BasicsScreen(form: OnboardingForm, onFormChange: (OnboardingForm) -> Unit, onNext: () -> Unit, onBack: () -> Unit) {
    val metric = form.units == UnitSystem.Metric
    val heightStep = if (metric) 1.0 else CM_PER_IN
    val weightStep = if (metric) 0.5 else KG_PER_LB

    Column(modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            OnboardingStepHeader(currentStep = 2, totalSteps = 6, onBack = onBack)
            Text(
                text = stringResource(R.string.onboarding_basics_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            SegmentedToggle(
                options = listOf(
                    stringResource(R.string.onboarding_basics_metric),
                    stringResource(R.string.onboarding_basics_imperial),
                ),
                selectedIndex = if (metric) 0 else 1,
                onSelect = { index -> onFormChange(form.copy(units = if (index == 0) UnitSystem.Metric else UnitSystem.Imperial)) },
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.onboarding_basics_sex),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                SegmentedToggle(
                    options = listOf(
                        stringResource(R.string.onboarding_basics_male),
                        stringResource(R.string.onboarding_basics_female),
                    ),
                    selectedIndex = when (form.sex) { Sex.Male -> 0; Sex.Female -> 1; null -> -1 },
                    onSelect = { index -> onFormChange(form.copy(sex = if (index == 0) Sex.Male else Sex.Female).clearOverrides()) },
                )
            }
            NumericStepperField(
                label = stringResource(R.string.onboarding_basics_age),
                value = form.age?.toString() ?: "—",
                unitSuffix = stringResource(R.string.onboarding_basics_age_unit),
                onIncrement = { onFormChange(form.copy(age = if (form.age == null) 25 else (form.age + 1).coerceAtMost(100)).clearOverrides()) },
                onDecrement = { onFormChange(form.copy(age = if (form.age == null) 25 else (form.age - 1).coerceAtLeast(13)).clearOverrides()) },
            )
            NumericStepperField(
                label = stringResource(R.string.onboarding_basics_height),
                value = displayHeight(form.heightCm, metric).toString(),
                unitSuffix = form.units.lengthUnitLabel(),
                onIncrement = { onFormChange(form.copy(heightCm = form.heightCm + heightStep).clearOverrides()) },
                onDecrement = { onFormChange(form.copy(heightCm = (form.heightCm - heightStep).coerceAtLeast(50.0)).clearOverrides()) },
            )
            NumericStepperField(
                label = stringResource(R.string.onboarding_basics_weight),
                value = formatWeight(displayWeight(form.weightKg, metric)),
                unitSuffix = form.units.weightUnitLabel(),
                onIncrement = { onFormChange(form.copy(weightKg = round1(form.weightKg + weightStep)).clearOverrides()) },
                onDecrement = { onFormChange(form.copy(weightKg = round1((form.weightKg - weightStep).coerceAtLeast(20.0))).clearOverrides()) },
            )
            if (form.goal != Goal.Maintain) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(
                            text = stringResource(R.string.onboarding_basics_target),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        TextButton(
                            label = stringResource(R.string.onboarding_basics_clear),
                            onClick = { onFormChange(form.copy(targetWeightKg = null)) },
                        )
                    }
                    NumericStepperField(
                        label = "",
                        value = form.targetWeightKg?.let { formatWeight(displayWeight(it, metric)) } ?: "—",
                        unitSuffix = form.units.weightUnitLabel(),
                        onIncrement = {
                            val base = form.targetWeightKg ?: form.weightKg
                            onFormChange(form.copy(targetWeightKg = round1(base + weightStep)))
                        },
                        onDecrement = {
                            val base = form.targetWeightKg ?: form.weightKg
                            onFormChange(form.copy(targetWeightKg = round1((base - weightStep).coerceAtLeast(20.0))))
                        },
                    )
                }
            }
        }
        Column(modifier = Modifier.padding(16.dp)) {
            PrimaryButton(label = stringResource(R.string.onboarding_next), onClick = onNext, enabled = form.isBasicsValid, modifier = Modifier.fillMaxWidth())
        }
    }
}

private fun displayHeight(heightCm: Double, metric: Boolean): Int =
    (if (metric) heightCm else heightCm / CM_PER_IN).roundToInt()

private fun displayWeight(weightKg: Double, metric: Boolean): Double =
    round1(if (metric) weightKg else weightKg / KG_PER_LB)

private fun formatWeight(value: Double): String =
    if (value == value.roundToInt().toDouble()) value.roundToInt().toString() else value.toString()

@PreviewLightDark
@Composable
private fun BasicsScreenPreview() {
    AppTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            BasicsScreen(
                form = OnboardingForm(sex = Sex.Female, age = 28, goal = Goal.Lose),
                onFormChange = {},
                onNext = {},
                onBack = {},
            )
        }
    }
}
