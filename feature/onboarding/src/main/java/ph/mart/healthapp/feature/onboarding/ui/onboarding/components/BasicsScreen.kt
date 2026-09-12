package ph.mart.healthapp.feature.onboarding.ui.onboarding.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.data.profile.Goal
import ph.mart.healthapp.core.data.profile.Sex
import ph.mart.healthapp.core.data.profile.UnitSystem
import ph.mart.healthapp.core.data.profile.cmToDisplayUnit
import ph.mart.healthapp.core.data.profile.displayUnitToCm
import ph.mart.healthapp.core.data.profile.displayUnitToKg
import ph.mart.healthapp.core.data.profile.kgToDisplayUnit
import ph.mart.healthapp.core.data.profile.lengthUnitLabel
import ph.mart.healthapp.core.data.profile.weightUnitLabel
import ph.mart.healthapp.core.designsystem.component.MascotState
import ph.mart.healthapp.core.designsystem.component.PrimaryButton
import ph.mart.healthapp.core.designsystem.component.RulerPickerField
import ph.mart.healthapp.core.designsystem.component.SegmentedToggle
import ph.mart.healthapp.core.designsystem.component.TextButton
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.onboarding.R
import ph.mart.healthapp.feature.onboarding.ui.onboarding.OnboardingForm
import ph.mart.healthapp.feature.onboarding.ui.onboarding.clearOverrides
import ph.mart.healthapp.feature.onboarding.ui.shared.components.OnboardingStep
import ph.mart.healthapp.feature.onboarding.ui.shared.components.reflowed

/** What each ruler will accept, in the unit it is drawn in. Age is the one the profile itself
 * bounds; the other two are the range a scale can usefully show rather than a medical limit. */
private val AGE_YEARS = 13.0..100.0
private val HEIGHT_CM = 120.0..220.0
private val HEIGHT_IN = 48.0..86.0
private val WEIGHT_KG = 30.0..250.0
private val WEIGHT_LB = 66.0..550.0

/**
 * Onboarding step 2 of 6. Four [RulerPickerField]s, because a stepper is one tap per unit and
 * every figure on this screen is chosen out of a range rather than nudged: 65 kg to 78 kg is 26
 * taps with a stepper and one gesture with a ruler. Tapping a value still opens the numeric IME,
 * so the typed path is there without a text field in the layout.
 *
 * Nothing is preselected on either toggle and nothing is preset in the three fields — an untouched
 * form reads "—" rather than a default the user never chose. Switching units converts every value
 * in place and resets none of them. Target weight only appears once the goal is something other
 * than Maintain, and is never required.
 */
@Composable
internal fun BasicsScreen(
    form: OnboardingForm,
    onFormChange: (OnboardingForm) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit,
) {
    val metric = form.units == UnitSystem.Metric
    val weightRange = if (metric) WEIGHT_KG else WEIGHT_LB
    val weightStep = if (metric) 0.5 else 1.0
    val weightDecimals = if (metric) 1 else 0
    val weightUnit = form.units.weightUnitLabel()

    OnboardingStep(
        step = 2,
        mascotState = MascotState.Idle,
        line = stringResource(R.string.onboarding_basics_bubble),
        headline = stringResource(R.string.onboarding_basics_title),
        onBack = onBack,
        bottomBar = {
            PrimaryButton(
                label = stringResource(R.string.onboarding_next),
                onClick = onNext,
                enabled = form.isBasicsValid,
                modifier = Modifier.fillMaxWidth(),
            )
        },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            SegmentedToggle(
                options = listOf(
                    stringResource(R.string.onboarding_basics_metric),
                    stringResource(R.string.onboarding_basics_imperial),
                ),
                selectedIndex = if (metric) 0 else 1,
                onSelect = { index ->
                    onFormChange(form.copy(units = if (index == 0) UnitSystem.Metric else UnitSystem.Imperial))
                },
            )
            SexRow(
                selected = form.sex,
                onSelect = { sex -> onFormChange(form.copy(sex = sex).clearOverrides()) },
            )
            RulerPickerField(
                label = stringResource(R.string.onboarding_basics_age),
                value = form.age?.toDouble(),
                range = AGE_YEARS,
                step = 1.0,
                unit = stringResource(R.string.onboarding_basics_age_unit),
                onValueChange = { onFormChange(form.copy(age = it.toInt()).clearOverrides()) },
            )
            RulerPickerField(
                label = stringResource(R.string.onboarding_basics_height),
                value = form.heightCm?.cmToDisplayUnit(form.units),
                range = if (metric) HEIGHT_CM else HEIGHT_IN,
                step = 1.0,
                unit = form.units.lengthUnitLabel(),
                onValueChange = {
                    onFormChange(form.copy(heightCm = it.displayUnitToCm(form.units)).clearOverrides())
                },
            )
            RulerPickerField(
                label = stringResource(R.string.onboarding_basics_weight),
                value = form.weightKg?.kgToDisplayUnit(form.units),
                range = weightRange,
                step = weightStep,
                decimals = weightDecimals,
                unit = weightUnit,
                onValueChange = {
                    onFormChange(form.copy(weightKg = it.displayUnitToKg(form.units)).clearOverrides())
                },
            )
            if (form.goal == Goal.Maintain) {
                Text(
                    text = stringResource(R.string.onboarding_basics_target_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                RulerPickerField(
                    label = stringResource(R.string.onboarding_basics_target),
                    value = form.targetWeightKg?.kgToDisplayUnit(form.units),
                    range = weightRange,
                    step = weightStep,
                    decimals = weightDecimals,
                    unit = weightUnit,
                    onValueChange = {
                        onFormChange(form.copy(targetWeightKg = it.displayUnitToKg(form.units)))
                    },
                    trailing = {
                        TextButton(
                            label = stringResource(R.string.onboarding_basics_clear),
                            onClick = { onFormChange(form.copy(targetWeightKg = null)) },
                        )
                    },
                )
            }
        }
    }
}

/** Label and toggle share a row until the font is large enough that they cannot — above the
 * reflow scale a 32dp label beside a two-pill track clips one of them. */
@Composable
private fun SexRow(selected: Sex?, onSelect: (Sex) -> Unit) {
    val label = stringResource(R.string.onboarding_basics_sex)
    val options = listOf(
        stringResource(R.string.onboarding_basics_male),
        stringResource(R.string.onboarding_basics_female),
    )
    val index = when (selected) {
        Sex.Male -> 0
        Sex.Female -> 1
        null -> -1
    }
    val toggle: @Composable (Modifier) -> Unit = { modifier ->
        SegmentedToggle(
            options = options,
            selectedIndex = index,
            onSelect = { onSelect(if (it == 0) Sex.Male else Sex.Female) },
            modifier = modifier,
        )
    }
    if (reflowed()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            toggle(Modifier.fillMaxWidth())
        }
    } else {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(32.dp),
            )
            toggle(Modifier.weight(1f))
        }
    }
}

@PreviewLightDark
@Composable
private fun BasicsScreenPreview() {
    AppTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            BasicsScreen(
                form = OnboardingForm(sex = Sex.Female, age = 28, goal = Goal.Lose, targetWeightKg = 58.0),
                onFormChange = {},
                onNext = {},
                onBack = {},
            )
        }
    }
}

/** First arrival: nothing set, every scale still drawn, Next at 40%. An untouched form is not an
 * error, so nothing here is red. */
@PreviewLightDark
@Composable
private fun BasicsScreenUnsetPreview() {
    AppTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            BasicsScreen(form = OnboardingForm(goal = Goal.Lose), onFormChange = {}, onNext = {}, onBack = {})
        }
    }
}
