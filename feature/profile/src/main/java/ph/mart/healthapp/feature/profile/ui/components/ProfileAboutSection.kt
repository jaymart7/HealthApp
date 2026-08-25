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

private const val VERSION_NAME = "1.0.0 (prototype)"

@Composable
internal fun ProfileAboutSection(modifier: Modifier = Modifier) {
    SettingsSection(label = "About", modifier = modifier) {
        AppCard {
            SettingsRow(
                label = "Version",
                trailing = {
                    Text(
                        text = VERSION_NAME,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
            )
            Text(
                text = "Estimates based on your inputs, not medical advice.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun ProfileAboutSectionPreview() {
    AppTheme {
        Surface {
            ProfileAboutSection(modifier = Modifier.padding(16.dp))
        }
    }
}
