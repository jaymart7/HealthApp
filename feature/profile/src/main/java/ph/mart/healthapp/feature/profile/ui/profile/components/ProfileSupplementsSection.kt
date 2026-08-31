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
 * The way into the supplement list. A nav row with no count, for the same reason
 * [ProfileLibrarySection] and [ProfileConnectionsSection] carry none: a number here is one more
 * thing that can go stale, and the screen it opens is where counting is honest.
 */
@Composable
internal fun ProfileSupplementsSection(onOpenSupplements: () -> Unit, modifier: Modifier = Modifier) {
    SettingsSection(label = "Supplements", modifier = modifier) {
        AppCard(onClick = onOpenSupplements) {
            SettingsRow(
                label = "What you take",
                sublabel = "Your daily list — it shows up on Home",
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun ProfileSupplementsSectionPreview() {
    AppTheme {
        Surface {
            ProfileSupplementsSection(onOpenSupplements = {}, modifier = Modifier.padding(16.dp))
        }
    }
}
