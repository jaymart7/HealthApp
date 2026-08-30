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
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.data.profile.ActivityLevel
import ph.mart.healthapp.core.designsystem.component.PrimaryButton
import ph.mart.healthapp.core.designsystem.component.SelectableCard
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.onboarding.ui.onboarding.ACTIVITY_OPTIONS
import ph.mart.healthapp.feature.onboarding.ui.onboarding.ActivityOption
import ph.mart.healthapp.feature.onboarding.ui.shared.components.OnboardingStepHeader

/** Onboarding step 3 of 5. */
@Composable
internal fun ActivityScreen(
    options: List<ActivityOption>,
    selected: ActivityLevel?,
    onSelect: (ActivityLevel) -> Unit,
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
            OnboardingStepHeader(currentStep = 3, totalSteps = 6, onBack = onBack)
            Text(
                text = "How active are you?",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                options.forEach { option ->
                    SelectableCard(
                        title = option.title,
                        subtitle = option.subtitle,
                        selected = selected == option.level,
                        onClick = { onSelect(option.level) },
                    )
                }
            }
        }
        Column(modifier = Modifier.padding(16.dp)) {
            PrimaryButton(label = "Next", onClick = onNext, enabled = selected != null, modifier = Modifier.fillMaxWidth())
        }
    }
}

@PreviewLightDark
@Composable
private fun ActivityScreenPreview() {
    AppTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            ActivityScreen(options = ACTIVITY_OPTIONS, selected = ActivityLevel.Light, onSelect = {}, onNext = {}, onBack = {})
        }
    }
}
