package ph.mart.healthapp.feature.profile.ui.profile.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.designsystem.component.AppCard
import ph.mart.healthapp.core.designsystem.theme.AppTheme

/**
 * The way into the saved meals and recipes. A nav row with no count, for the same reason
 * [ProfileConnectionsSection] caches no connection state: a number here is one more thing that can
 * go stale, and the screen it opens is where counting is honest.
 */
@Composable
internal fun ProfileLibrarySection(onOpenLibrary: () -> Unit, modifier: Modifier = Modifier) {
    SettingsSection(label = "Library", modifier = modifier) {
        AppCard(onClick = onOpenLibrary) {
            SettingsRow(
                label = "Saved meals & recipes",
                sublabel = "Rename or delete anything you've saved",
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun ProfileLibrarySectionPreview() {
    AppTheme {
        Surface {
            ProfileLibrarySection(onOpenLibrary = {}, modifier = Modifier.padding(16.dp))
        }
    }
}
