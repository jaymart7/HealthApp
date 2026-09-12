package ph.mart.healthapp.feature.onboarding.ui.onboarding.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.selectableGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.data.profile.DietaryPreference
import ph.mart.healthapp.core.designsystem.component.MascotState
import ph.mart.healthapp.core.designsystem.component.PrimaryButton
import ph.mart.healthapp.core.designsystem.component.SelectableCard
import ph.mart.healthapp.core.designsystem.component.TextButton
import ph.mart.healthapp.core.designsystem.component.TonalButton
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.onboarding.R
import ph.mart.healthapp.feature.onboarding.ui.onboarding.DIET_OPTIONS
import ph.mart.healthapp.feature.onboarding.ui.onboarding.DietOption
import ph.mart.healthapp.feature.onboarding.ui.shared.components.MinCardHeight
import ph.mart.healthapp.feature.onboarding.ui.shared.components.OnboardingStep
import ph.mart.healthapp.feature.onboarding.ui.shared.components.cardsFit

/**
 * Onboarding step 4 of 6, and the one optional question in the flow.
 *
 * **It keeps its button.** Tapping the selected card clears it, so auto-advance would make
 * deselection impossible, and "no preference" is a real answer that needs somewhere to go.
 *
 * Everything else about the step says *optional* by weight rather than by label: the cards are
 * outlined instead of filled, the button is tonal until something is chosen, and the stack is
 * bottom-anchored so the air lands in one band under the headline and the answers sit in thumb
 * reach. Three steps of filled cards and a filled button have already taught the reader what
 * required looks like. Selected still fills like every other step — optional applies to the
 * question, not to the answer.
 */
@Composable
internal fun DietaryScreen(
    options: List<DietOption>,
    selected: DietaryPreference?,
    onSelect: (DietOption) -> Unit,
    onSkip: () -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit,
) {
    val next = stringResource(R.string.onboarding_next)
    // Fixed height, not `weight(1f)` like steps 1 and 3: a one-line label in a 130dp card is a
    // hole. The air becomes one band under the headline instead — unless there isn't any, in
    // which case the step scrolls like the others.
    val fits = cardsFit(cards = 4)
    OnboardingStep(
        step = 4,
        mascotState = if (selected == null) MascotState.Idle else MascotState.Happy,
        line = stringResource(
            options.firstOrNull { it.preference == selected }?.bubble ?: R.string.onboarding_diet_bubble,
        ),
        headline = stringResource(R.string.onboarding_diet_title),
        onBack = onBack,
        scrollable = !fits,
        trailingAction = { TextButton(label = stringResource(R.string.onboarding_diet_skip), onClick = onSkip) },
        bottomBar = {
            if (selected == null) {
                TonalButton(label = next, onClick = onNext, modifier = Modifier.fillMaxWidth())
            } else {
                PrimaryButton(label = next, onClick = onNext, modifier = Modifier.fillMaxWidth())
            }
        },
    ) {
        if (fits) Spacer(modifier = Modifier.weight(1f))
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth().semantics {
                isTraversalGroup = true
                selectableGroup()
            },
        ) {
            options.forEach { option ->
                SelectableCard(
                    title = stringResource(option.title),
                    leadingIcon = option.icon,
                    outlined = true,
                    selected = selected == option.preference,
                    onClick = { onSelect(option) },
                    modifier = Modifier.height(MinCardHeight),
                )
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun DietaryScreenPreview() {
    AppTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            DietaryScreen(
                options = DIET_OPTIONS,
                selected = DietaryPreference.Vegetarian,
                onSelect = {},
                onSkip = {},
                onNext = {},
                onBack = {},
            )
        }
    }
}

/** Nothing chosen: outlined cards, tonal button — the whole step one level quieter. */
@PreviewLightDark
@Composable
private fun DietaryScreenSkippablePreview() {
    AppTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            DietaryScreen(
                options = DIET_OPTIONS,
                selected = null,
                onSelect = {},
                onSkip = {},
                onNext = {},
                onBack = {},
            )
        }
    }
}
