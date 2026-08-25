package ph.mart.healthapp.feature.profile.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.designsystem.component.AppCard
import ph.mart.healthapp.core.designsystem.theme.AppTheme

/** Export and import are the only destructive-ish controls on this screen, so each says exactly
 * what it does in its sublabel. [message] carries an import failure, or a confirmation. */
@Composable
internal fun ProfileDataSection(
    onExport: () -> Unit,
    onImport: () -> Unit,
    modifier: Modifier = Modifier,
    message: String? = null,
    messageIsError: Boolean = false,
) {
    SettingsSection(label = "Data", modifier = modifier) {
        AppCard(onClick = onExport) {
            SettingsRow(label = "Export data (JSON)", sublabel = "Photos are not included in exports")
        }
        AppCard(onClick = onImport) {
            SettingsRow(
                label = "Import data (JSON)",
                sublabel = "Replaces your current profile and food entries",
            )
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
    }
}

@PreviewLightDark
@Composable
private fun ProfileDataSectionPreview() {
    AppTheme {
        Surface {
            ProfileDataSection(
                onExport = {},
                onImport = {},
                message = "That file couldn't be read.",
                messageIsError = true,
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}
