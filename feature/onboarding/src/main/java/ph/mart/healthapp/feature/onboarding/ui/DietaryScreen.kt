package ph.mart.healthapp.feature.onboarding.ui

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
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.data.profile.DietaryPreference
import ph.mart.healthapp.core.designsystem.component.PrimaryButton
import ph.mart.healthapp.core.designsystem.component.SelectableCard
import ph.mart.healthapp.core.designsystem.component.TextButton
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.onboarding.ui.components.OnboardingStepHeader

/** Onboarding step 4 of 6. Skip and Next are the same transition — tapping the already-selected
 * card toggles it off (handled by the caller's [onSelect]). */
@Composable
fun DietaryScreen(
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
                trailingAction = { TextButton(label = "Skip", onClick = onNext) },
            )
            Text(
                text = "Any dietary preference?",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                options.forEach { option ->
                    SelectableCard(
                        title = option.title,
                        selected = selected == option.preference,
                        onClick = { onSelect(option.preference) },
                    )
                }
            }
        }
        Column(modifier = Modifier.padding(16.dp)) {
            PrimaryButton(label = "Next", onClick = onNext, modifier = Modifier.fillMaxWidth())
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
