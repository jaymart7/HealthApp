package ph.mart.healthapp.feature.profile.ui.health.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.designsystem.component.AppCard
import ph.mart.healthapp.core.designsystem.component.PrimaryButton
import ph.mart.healthapp.core.designsystem.component.SecondaryButton
import ph.mart.healthapp.core.designsystem.theme.AppTheme

@Composable
internal fun ConnectedPanel(
    importedItems: Int,
    busy: Boolean,
    message: String?,
    messageIsError: Boolean,
    onSync: () -> Unit,
    onDisconnect: () -> Unit,
    onBack: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Google Health",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        AppCard {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Connected",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = if (importedItems == 0) {
                        "Nothing imported yet. Sync to pull in your workouts."
                    } else {
                        "$importedItems items imported from your watch and connected apps."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (message != null) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = if (messageIsError) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
        PrimaryButton(
            label = if (busy) "Syncing…" else "Sync now",
            onClick = onSync,
            enabled = !busy,
            modifier = Modifier.fillMaxWidth(),
        )
        SecondaryButton(
            label = "Disconnect",
            onClick = onDisconnect,
            enabled = !busy,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = "You can also review or revoke this at myaccount.google.com/permissions.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        SecondaryButton(label = "Back", onClick = onBack, modifier = Modifier.fillMaxWidth())
    }
}

/**
 * Disconnecting revokes the grant either way. The two checkboxes are about the data on each side,
 * and both default to deleting — an integration that leaves health data behind after being
 * switched off is exactly what the security assessment looks for.
 */

@PreviewLightDark
@Composable
private fun ConnectedPanelPreview() {
    AppTheme {
        ConnectedPanel(
            importedItems = 12,
            busy = false,
            message = "Imported 3 workouts.",
            messageIsError = false,
            onSync = {},
            onDisconnect = {},
            onBack = {},
        )
    }
}

/** Mid-sync, and the last attempt failed — the two states that change what the panel says. */
@PreviewLightDark
@Composable
private fun ConnectedPanelBusyPreview() {
    AppTheme {
        ConnectedPanel(
            importedItems = 0,
            busy = true,
            message = "Couldn't reach Google Health.",
            messageIsError = true,
            onSync = {},
            onDisconnect = {},
            onBack = {},
        )
    }
}
