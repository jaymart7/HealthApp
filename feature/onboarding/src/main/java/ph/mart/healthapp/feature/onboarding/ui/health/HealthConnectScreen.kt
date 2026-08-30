package ph.mart.healthapp.feature.onboarding.ui.health

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import org.koin.androidx.compose.koinViewModel
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect
import ph.mart.healthapp.core.designsystem.component.HealthDisclosurePanel
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.onboarding.ui.shared.components.OnboardingStepHeader

/**
 * Onboarding step 5 of 6 — the Google Health disclosure, before the profile is even written.
 *
 * It sits here rather than after Confirm because finishing onboarding writes the profile, and
 * `AppRoot` swaps the whole wizard out the moment that lands. Skipping costs nothing: the same
 * disclosure and the same connect flow live in Profile → Connections for later.
 */
@Composable
internal fun HealthConnectScreen(
    onNext: () -> Unit,
    onBack: () -> Unit,
    viewModel: OnboardingHealthViewModel = koinViewModel(),
) {
    val uiState by viewModel.collectAsState()

    val consentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult(),
    ) { result -> viewModel.onConsentResult(result.data) }

    viewModel.collectSideEffect { effect ->
        when (effect) {
            is OnboardingHealthSideEffect.LaunchConsent ->
                consentLauncher.launch(IntentSenderRequest.Builder(effect.pendingIntent.intentSender).build())

            // Connected and the first sync is away — nothing to wait for, keep the wizard moving.
            OnboardingHealthSideEffect.Connected -> onNext()
        }
    }

    HealthConnectContent(
        canConnect = uiState.canConnect,
        message = uiState.message,
        messageIsError = uiState.messageIsError,
        onConnect = viewModel::connect,
        onSkip = onNext,
        onBack = onBack,
    )
}

@Composable
private fun HealthConnectContent(
    canConnect: Boolean,
    message: String?,
    messageIsError: Boolean,
    onConnect: () -> Unit,
    onSkip: () -> Unit,
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
            OnboardingStepHeader(currentStep = 5, totalSteps = 6, onBack = onBack)
            HealthDisclosurePanel(
                onConnect = onConnect,
                onDismiss = onSkip,
                dismissLabel = "Skip for now",
                connectEnabled = canConnect,
                message = message,
                messageIsError = messageIsError,
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun HealthConnectScreenPreview() {
    AppTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            HealthConnectContent(
                canConnect = true,
                message = null,
                messageIsError = false,
                onConnect = {},
                onSkip = {},
                onBack = {},
            )
        }
    }
}
