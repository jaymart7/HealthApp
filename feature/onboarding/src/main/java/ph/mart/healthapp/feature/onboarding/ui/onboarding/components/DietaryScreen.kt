package ph.mart.healthapp.feature.onboarding.ui.onboarding.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import ph.mart.healthapp.core.data.profile.DietaryPreference
import ph.mart.healthapp.core.designsystem.component.PrimaryButton
import ph.mart.healthapp.core.designsystem.component.SelectableCard
import ph.mart.healthapp.core.designsystem.component.TextButton
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.onboarding.R
import ph.mart.healthapp.feature.onboarding.ui.onboarding.DIET_OPTIONS
import ph.mart.healthapp.feature.onboarding.ui.onboarding.DietOption
import ph.mart.healthapp.feature.onboarding.ui.shared.components.OnboardingStepHeader

/** Onboarding step 4 of 6. Skip and Next are the same transition — tapping the already-selected
 * card toggles it off (handled by the caller's [onSelect]). */
@Composable
internal fun DietaryScreen(
    options: List<DietOption>,
    selected: DietaryPreference?,
    onSelect: (DietaryPreference) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            OnboardingStepHeader(
                currentStep = 4,
                totalSteps = 6,
                onBack = onBack,
                trailingAction = { TextButton(label = stringResource(R.string.onboarding_diet_skip), onClick = onNext) },
            )
            Text(
                text = stringResource(R.string.onboarding_diet_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                options.forEach { option ->
                    SelectableCard(
                        title = stringResource(option.title),
                        selected = selected == option.preference,
                        onClick = { onSelect(option.preference) },
                    )
                }
            }
        }
        Column(modifier = Modifier.padding(16.dp)) {
            PrimaryButton(label = stringResource(R.string.onboarding_next), onClick = onNext, modifier = Modifier.fillMaxWidth())
        }
    }
}

@PreviewLightDark
@Composable
private fun DietaryScreenPreview() {
    AppTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            DietaryScreen(options = DIET_OPTIONS, selected = DietaryPreference.Vegetarian, onSelect = {}, onNext = {}, onBack = {})
        }
    }
}
