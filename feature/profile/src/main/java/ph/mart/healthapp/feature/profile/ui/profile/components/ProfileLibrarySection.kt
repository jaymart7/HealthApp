package ph.mart.healthapp.feature.profile.ui.profile.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.designsystem.component.AppCard
import ph.mart.healthapp.core.designsystem.theme.AppTheme

/**
 * The way into everything the user has saved: meals and recipes on one screen, workout routines on
 * another. Two rows rather than one screen holding both — a routine and a saved meal have nothing
 * to do with each other beyond being reusable, and each screen's own list is what its feature's
 * panels read.
 *
 * Neither row carries a count, for the same reason [ProfileConnectionsSection] caches no connection
 * state: a number here is one more thing that can go stale, and the screen it opens is where
 * counting is honest.
 */
@Composable
internal fun ProfileLibrarySection(
    onOpenLibrary: () -> Unit,
    onOpenRoutines: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingsSection(label = "Library", modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            AppCard(onClick = onOpenLibrary) {
                SettingsRow(
                    label = "Saved meals & recipes",
                    sublabel = "Rename or delete anything you've saved",
                )
            }
            AppCard(onClick = onOpenRoutines) {
                SettingsRow(
                    label = "Workout routines",
                    sublabel = "Set which days you train, rename or delete",
                )
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun ProfileLibrarySectionPreview() {
    AppTheme {
        Surface {
            ProfileLibrarySection(onOpenLibrary = {}, onOpenRoutines = {}, modifier = Modifier.padding(16.dp))
        }
    }
}
