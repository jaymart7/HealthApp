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
 * The way into the Home card editor. A nav row with no summary of what's hidden, for the same
 * reason [ProfileSupplementsSection] carries no count: a number here is one more thing that can
 * go stale, and the screen it opens is where counting is honest.
 *
 * It sits under Appearance because that is what it is — the second choice about how the app
 * looks, beside the theme and the mascot.
 */
@Composable
internal fun ProfileHomeLayoutSection(onOpenHomeLayout: () -> Unit, modifier: Modifier = Modifier) {
    SettingsSection(label = "Home layout", modifier = modifier) {
        AppCard(onClick = onOpenHomeLayout) {
            SettingsRow(
                label = "Cards on Home",
                sublabel = "Choose which ones show, and in what order",
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun ProfileHomeLayoutSectionPreview() {
    AppTheme {
        Surface {
            ProfileHomeLayoutSection(onOpenHomeLayout = {}, modifier = Modifier.padding(16.dp))
        }
    }
}
