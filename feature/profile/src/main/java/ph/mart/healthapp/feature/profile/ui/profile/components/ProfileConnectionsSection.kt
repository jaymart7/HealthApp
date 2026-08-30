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
 * The way into the Google Health connection. Deliberately just a nav row: connection state is
 * whatever Google says right now, so it is resolved on the screen that shows it rather than
 * cached into a sublabel here that a revocation elsewhere would quietly turn into a lie.
 */
@Composable
internal fun ProfileConnectionsSection(onOpenHealth: () -> Unit, modifier: Modifier = Modifier) {
    SettingsSection(label = "Connections", modifier = modifier) {
        AppCard(onClick = onOpenHealth) {
            SettingsRow(
                label = "Google Health",
                sublabel = "Import workouts, weigh-ins and sleep; send your food and water",
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun ProfileConnectionsSectionPreview() {
    AppTheme {
        Surface {
            ProfileConnectionsSection(onOpenHealth = {}, modifier = Modifier.padding(16.dp))
        }
    }
}
