package ph.mart.healthapp.feature.profile.ui.profile.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.designsystem.component.AppCard
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.profile.R

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
    SettingsSection(label = stringResource(R.string.profile_section_data), modifier = modifier) {
        AppCard(onClick = onExport) {
            SettingsRow(
                label = stringResource(R.string.profile_export),
                sublabel = stringResource(R.string.profile_export_sub),
            )
        }
        AppCard(onClick = onImport) {
            SettingsRow(
                label = stringResource(R.string.profile_import),
                sublabel = stringResource(R.string.profile_import_sub),
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
