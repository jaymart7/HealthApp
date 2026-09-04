package ph.mart.healthapp.feature.profile.ui.health

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import org.koin.androidx.compose.koinViewModel
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect
import ph.mart.healthapp.core.data.health.HealthConnectState
import ph.mart.healthapp.core.data.health.HealthConnection
import ph.mart.healthapp.core.data.health.HealthMetric
import ph.mart.healthapp.core.designsystem.component.HealthDisclosurePanel
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.profile.ui.health.components.ConnectedPanel
import ph.mart.healthapp.feature.profile.ui.health.components.DisconnectSheet
import ph.mart.healthapp.feature.profile.ui.health.components.HealthConnectPanel

/**
 * FitPulse's health connections, one Nav3 level above Profile — so system back returns to the
 * Profile tab, and the disclosure is a screen of its own rather than a row in a settings list.
 *
 * **Two providers, one screen.** Health Connect is drawn first because it is the one that wins
 * wherever it is granted (see `cloudMetrics`); the Google Health API sits below it with its own
 * disclosure and its own consent flow. Neither disclosure is ever skipped — each provider's consent
 * UI is only reachable from the button on its own passage.
 *
 * This screen is also the target of Health Connect's "why does this app want my data?" intent, on
 * both the Android 13 and the Android 14+ path — see the manifest and `ShortcutAction.HealthSync`.
 */
@Composable
fun HealthConnectionScreen(
    onBack: () -> Unit,
    viewModel: HealthConnectionViewModel = koinViewModel(),
) {
    val uiState by viewModel.collectAsState()

    val context = LocalContext.current

    val consentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult(),
    ) { result -> viewModel.onConsentResult(result.data) }

    // The contract is built by the repository, not here, so this module never imports
    // `androidx.health.connect`. Remembered because a launcher's contract must be stable across
    // recomposition, and this one is rebuilt on every call.
    val permissionLauncher = rememberLauncherForActivityResult(
        remember(viewModel) { viewModel.permissionContract() },
    ) { granted -> viewModel.onConnectPermissionsResult(granted) }

    viewModel.collectSideEffect { effect ->
        when (effect) {
            is HealthConnectionSideEffect.LaunchConsent ->
                consentLauncher.launch(IntentSenderRequest.Builder(effect.pendingIntent.intentSender).build())

            is HealthConnectionSideEffect.RequestConnectPermissions ->
                permissionLauncher.launch(effect.permissions)

            HealthConnectionSideEffect.OpenHealthConnectListing -> {
                // No Play Store on the device is a real case (a sideloaded build, an emulator
                // without Play), and it is not worth a crash — the panel simply stays as it was.
                runCatching {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse(HEALTH_CONNECT_LISTING))
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    )
                }.onFailure { if (it !is ActivityNotFoundException) throw it }
            }
        }
    }

    HealthConnectionContent(
        uiState = uiState,
        onConnect = viewModel::connect,
        onAllowConnect = viewModel::requestConnectPermissions,
        onSync = viewModel::sync,
        onConfirmDisconnect = viewModel::confirmDisconnect,
        onDisconnect = viewModel::disconnect,
        onBack = onBack,
    )
}

/** Health Connect's own listing. A permission can be revoked from there, which is why
 *  [HealthConnectionViewModel.refresh] re-asks rather than trusting what it last saw. */
private const val HEALTH_CONNECT_LISTING =
    "market://details?id=com.google.android.apps.healthdata"

@Composable
private fun HealthConnectionContent(
    uiState: HealthConnectionUiState,
    onConnect: () -> Unit,
    onAllowConnect: () -> Unit,
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
                .padding(horizontal = 16.dp, vertical = 16.dp),
        ) {
            // First, because it is the provider that wins. It draws nothing at all where Health
            // Connect is unsupported, which is what leaves an Android 8 phone with the screen it
            // has always had.
            HealthConnectPanel(
                state = uiState.connect,
                busy = uiState.busy,
                onAllow = onAllowConnect,
                onOpenPlayStore = onAllowConnect,
            )

            when (val connection = uiState.connection) {
                is HealthConnection.Connected -> ConnectedPanel(
                    importedItems = connection.importedItems,
                    busy = uiState.busy,
                    message = uiState.message,
                    messageIsError = uiState.messageIsError,
                    onSync = onSync,
                    onDisconnect = { onConfirmDisconnect(true) },
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

/** Both providers at once — the composition is the thing this screen adds, so it gets a preview
 *  of its own beside the two panels' own. */
@PreviewLightDark
@Composable
private fun HealthConnectionBothProvidersPreview() {
    AppTheme {
        HealthConnectionContent(
            uiState = HealthConnectionUiState(
                connection = HealthConnection.Connected(importedItems = 12),
                connect = HealthConnectState.Available(
                    granted = setOf(HealthMetric.Steps, HealthMetric.Sleep, HealthMetric.Exercise),
                ),
            ),
            onConnect = {},
            onAllowConnect = {},
            onSync = {},
            onConfirmDisconnect = {},
            onDisconnect = { _, _ -> },
            onBack = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun HealthConnectionDisclosurePreview() {
    AppTheme {
        HealthConnectionContent(
            uiState = HealthConnectionUiState(connection = HealthConnection.Disconnected(pendingIntent = null)),
            onConnect = {},
            onAllowConnect = {},
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
            onAllowConnect = {},
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
            onAllowConnect = {},
            onSync = {},
            onConfirmDisconnect = {},
            onDisconnect = { _, _ -> },
            onBack = {},
        )
    }
}
