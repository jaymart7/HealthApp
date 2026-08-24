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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.data.profile.Goal
import ph.mart.healthapp.core.designsystem.component.MascotAvatar
import ph.mart.healthapp.core.designsystem.component.MascotSpeechBubble
import ph.mart.healthapp.core.designsystem.component.MascotState
import ph.mart.healthapp.core.designsystem.component.PrimaryButton
import ph.mart.healthapp.core.designsystem.component.SelectableCard
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.onboarding.ui.components.OnboardingStepHeader

/** Onboarding step 1 of 5. */
@Composable
fun GoalScreen(options: List<GoalOption>, selected: Goal?, onSelect: (Goal) -> Unit, onNext: () -> Unit, onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            OnboardingStepHeader(currentStep = 1, totalSteps = 5, onBack = onBack)
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                MascotAvatar(state = MascotState.Idle, size = 64.dp)
                MascotSpeechBubble(text = "What's your goal?")
            }
            Text(
                text = "What brings you here?",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                options.forEach { option ->
                    SelectableCard(
                        title = option.title,
                        subtitle = option.subtitle,
                        selected = selected == option.goal,
                        onClick = { onSelect(option.goal) },
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
private fun GoalScreenPreview() {
    AppTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            GoalScreen(options = GOAL_OPTIONS, selected = Goal.Lose, onSelect = {}, onNext = {}, onBack = {})
        }
    }
}
