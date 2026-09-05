package ph.mart.healthapp.feature.profile.ui.health.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.designsystem.component.AppCard
import ph.mart.healthapp.core.designsystem.component.PrimaryButton
import ph.mart.healthapp.core.designsystem.component.SecondaryButton
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.profile.R

@Composable
internal fun ConnectedPanel(
    importedItems: Int,
    busy: Boolean,
    message: String?,
    messageIsError: Boolean,
    onSync: () -> Unit,
    onDisconnect: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
        AppCard {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(R.string.profile_health_connected),
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = if (importedItems == 0) {
                        stringResource(R.string.profile_health_nothing_imported)
                    } else {
                        stringResource(R.string.profile_health_items_imported, importedItems)
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
            label = stringResource(if (busy) R.string.profile_health_syncing else R.string.profile_health_sync_now),
            onClick = onSync,
            enabled = !busy,
            modifier = Modifier.fillMaxWidth(),
        )
        SecondaryButton(
            label = stringResource(R.string.profile_health_disconnect),
            onClick = onDisconnect,
            enabled = !busy,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = stringResource(R.string.profile_health_revoke_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
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
        )
    }
}
