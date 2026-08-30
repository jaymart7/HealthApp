package ph.mart.healthapp.feature.profile.ui.health

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import org.koin.androidx.compose.koinViewModel
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect
import ph.mart.healthapp.core.data.health.HealthConnection
import ph.mart.healthapp.core.designsystem.component.DockedFabContentPadding
import ph.mart.healthapp.core.designsystem.component.HealthDisclosurePanel
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.profile.ui.health.components.ConnectedPanel
import ph.mart.healthapp.feature.profile.ui.health.components.DisconnectSheet

/**
 * The Google Health connection, one Nav3 level above Profile — so system back returns to the
 * Profile tab, and the disclosure is a screen of its own rather than a row in a settings list.
 *
 * Two faces: the disclosure until the user has granted the scopes, and the connected state after.
 * The disclosure is never skipped — Google's consent screen is only ever raised by the button on
 * it, and [HealthConnectionViewModel.connect] is the only path to that.
 */
@Composable
fun HealthConnectionScreen(
    onBack: () -> Unit,
    viewModel: HealthConnectionViewModel = koinViewModel(),
) {
    val uiState by viewModel.collectAsState()

    val consentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult(),
    ) { result -> viewModel.onConsentResult(result.data) }

    viewModel.collectSideEffect { effect ->
        when (effect) {
            is HealthConnectionSideEffect.LaunchConsent ->
                consentLauncher.launch(IntentSenderRequest.Builder(effect.pendingIntent.intentSender).build())
        }
    }

    HealthConnectionContent(
        uiState = uiState,
        onConnect = viewModel::connect,
        onSync = viewModel::sync,
        onConfirmDisconnect = viewModel::confirmDisconnect,
        onDisconnect = viewModel::disconnect,
        onBack = onBack,
    )
}

@Composable
private fun HealthConnectionContent(
    uiState: HealthConnectionUiState,
    onConnect: () -> Unit,
    onSync: () -> Unit,
    onConfirmDisconnect: (Boolean) -> Unit,
    onDisconnect: (Boolean, Boolean) -> Unit,
    onBack: () -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxSize()) {
        Column(
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp)
                .padding(bottom = DockedFabContentPadding),
        ) {
            when (val connection = uiState.connection) {
                is HealthConnection.Connected -> ConnectedPanel(
                    importedItems = connection.importedItems,
                    busy = uiState.busy,
                    message = uiState.message,
                    messageIsError = uiState.messageIsError,
                    onSync = onSync,
                    onDisconnect = { onConfirmDisconnect(true) },
                    onBack = onBack,
                )

                // Checking shows the disclosure with its button disabled rather than a spinner:
                // the text is the point of the screen and should be readable immediately.
                HealthConnection.Checking,
                is HealthConnection.Disconnected,
                HealthConnection.Unavailable,
                -> HealthDisclosurePanel(
                    onConnect = onConnect,
                    onDismiss = onBack,
                    dismissLabel = "Not now",
                    connectEnabled = !uiState.busy && connection is HealthConnection.Disconnected,
                    message = uiState.message,
                    messageIsError = uiState.messageIsError,
                )
            }
        }

        if (uiState.confirmingDisconnect) {
            DisconnectSheet(
                onDismiss = { onConfirmDisconnect(false) },
                onConfirm = onDisconnect,
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun HealthConnectionDisclosurePreview() {
    AppTheme {
        HealthConnectionContent(
            uiState = HealthConnectionUiState(connection = HealthConnection.Disconnected(pendingIntent = null)),
            onConnect = {},
            onSync = {},
            onConfirmDisconnect = {},
            onDisconnect = { _, _ -> },
            onBack = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun HealthConnectionConnectedPreview() {
    AppTheme {
        HealthConnectionContent(
            uiState = HealthConnectionUiState(
                connection = HealthConnection.Connected(importedItems = 12),
                message = "Imported 3 workouts.",
            ),
            onConnect = {},
            onSync = {},
            onConfirmDisconnect = {},
            onDisconnect = { _, _ -> },
            onBack = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun HealthDisconnectSheetPreview() {
    AppTheme {
        HealthConnectionContent(
            uiState = HealthConnectionUiState(
                connection = HealthConnection.Connected(importedItems = 12),
                confirmingDisconnect = true,
            ),
            onConnect = {},
            onSync = {},
            onConfirmDisconnect = {},
            onDisconnect = { _, _ -> },
            onBack = {},
        )
    }
}
