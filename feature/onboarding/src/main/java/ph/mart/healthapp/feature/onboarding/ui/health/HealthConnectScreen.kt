package ph.mart.healthapp.feature.onboarding.ui.health

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import org.koin.androidx.compose.koinViewModel
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect
import ph.mart.healthapp.core.designsystem.component.HealthDisclosurePanel
import ph.mart.healthapp.core.designsystem.component.MascotState
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.onboarding.R
import ph.mart.healthapp.feature.onboarding.ui.shared.components.OnboardingStep

/**
 * Onboarding step 5 of 6 — the Google Health disclosure, before the profile is even written.
 *
 * It sits here rather than after Confirm because finishing onboarding writes the profile, and
 * `AppRoot` swaps the whole wizard out the moment that lands. Skipping costs nothing: the same
 * disclosure and the same connect flow live in Profile → Connections for later, which is why the
 * panel itself is in `:core:designsystem` and the step only supplies the chrome around it.
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
        declined = uiState.declined,
        onConnect = viewModel::connect,
        onSkip = onNext,
        onBack = onBack,
    )
}

@Composable
private fun HealthConnectContent(
    canConnect: Boolean,
    @StringRes message: Int?,
    messageIsError: Boolean,
    declined: Boolean,
    onConnect: () -> Unit,
    onSkip: () -> Unit,
    onBack: () -> Unit,
) {
    OnboardingStep(
        step = 5,
        mascotState = MascotState.Idle,
        line = stringResource(R.string.onboarding_health_bubble),
        headline = stringResource(R.string.onboarding_health_title),
        onBack = onBack,
    ) {
        HealthDisclosurePanel(
            onConnect = onConnect,
            onDismiss = onSkip,
            dismissLabel = stringResource(R.string.onboarding_health_skip),
            // The step chrome already carries the headline; a second one inside the panel would
            // say the same thing twice.
            title = null,
            connectEnabled = canConnect,
            declined = declined,
            message = message?.let { stringResource(it) },
            messageIsError = messageIsError,
        )
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
                declined = false,
                onConnect = {},
                onSkip = {},
                onBack = {},
            )
        }
    }
}

/** No Play services and no account: Connect drops out and the way forward relabels. */
@PreviewLightDark
@Composable
private fun HealthConnectScreenUnavailablePreview() {
    AppTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            HealthConnectContent(
                canConnect = false,
                message = R.string.onboarding_health_unavailable,
                messageIsError = true,
                declined = false,
                onConnect = {},
                onSkip = {},
                onBack = {},
            )
        }
    }
}

/** Asked and answered: continuing becomes the filled button, retrying drops to a text button. */
@PreviewLightDark
@Composable
private fun HealthConnectScreenDeclinedPreview() {
    AppTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            HealthConnectContent(
                canConnect = true,
                message = R.string.onboarding_health_declined,
                messageIsError = false,
                declined = true,
                onConnect = {},
                onSkip = {},
                onBack = {},
            )
        }
    }
}
