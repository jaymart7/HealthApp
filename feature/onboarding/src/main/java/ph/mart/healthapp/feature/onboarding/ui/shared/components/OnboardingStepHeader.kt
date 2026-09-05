package ph.mart.healthapp.feature.onboarding.ui.shared.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.designsystem.component.StepProgressBar
import ph.mart.healthapp.core.designsystem.component.TextButton
import ph.mart.healthapp.core.designsystem.icon.AppIcons
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.onboarding.R

/** Back button + [StepProgressBar], shared by every onboarding step except Welcome. Dietary is
 * the only step with a trailing action ("Skip"), hence [trailingAction] rather than a fork. */
@Composable
internal fun OnboardingStepHeader(
    currentStep: Int,
    totalSteps: Int,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    trailingAction: (@Composable () -> Unit)? = null,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Surface(
                onClick = onBack,
                color = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(48.dp),
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(imageVector = AppIcons.Back, contentDescription = stringResource(R.string.onboarding_back))
                }
            }
            if (trailingAction != null) trailingAction() else Box(modifier = Modifier.size(48.dp))
        }
        StepProgressBar(currentStep = currentStep, totalSteps = totalSteps)
    }
}

@PreviewLightDark
@Composable
private fun OnboardingStepHeaderPreview() {
    AppTheme {
        Surface {
            OnboardingStepHeader(currentStep = 2, totalSteps = 5, onBack = {}, modifier = Modifier.fillMaxWidth())
        }
    }
}

@PreviewLightDark
@Composable
private fun OnboardingStepHeaderWithSkipPreview() {
    AppTheme {
        Surface {
            OnboardingStepHeader(
                currentStep = 4,
                totalSteps = 5,
                onBack = {},
                trailingAction = { TextButton(label = "Skip", onClick = {}) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
