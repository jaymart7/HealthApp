package ph.mart.healthapp.feature.profile.ui.settings.components

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
import ph.mart.healthapp.core.designsystem.theme.tabularNums
import ph.mart.healthapp.feature.profile.R
import ph.mart.healthapp.feature.profile.ui.shared.components.AppListRow

// A version string, not copy: it names a build, and a translator has nothing to do with it.
private const val VERSION_NAME = "1.0.0 (prototype)"

@Composable
internal fun SettingsAboutSection(modifier: Modifier = Modifier) {
    AppCard(modifier = modifier) {
        AppListRow(
            label = stringResource(R.string.profile_version),
            trailing = {
                Text(
                    text = VERSION_NAME,
                    style = MaterialTheme.typography.bodyMedium.tabularNums,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
        )
        Text(
            text = stringResource(R.string.profile_disclaimer),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@PreviewLightDark
@Composable
private fun SettingsAboutSectionPreview() {
    AppTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            SettingsAboutSection(modifier = Modifier.padding(16.dp))
        }
    }
}
