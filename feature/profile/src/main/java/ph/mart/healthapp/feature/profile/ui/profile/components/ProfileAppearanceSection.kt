package ph.mart.healthapp.feature.profile.ui.profile.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.designsystem.component.AppCard
import ph.mart.healthapp.core.designsystem.theme.AppTheme

/**
 * Until this is touched the profile stores null and the app follows the device, so [darkTheme] is
 * resolved by the caller rather than read straight off the profile — the switch has to show what
 * the user is actually looking at, not the absence of a choice.
 */
@Composable
internal fun ProfileAppearanceSection(
    darkTheme: Boolean,
    onSetDarkTheme: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingsSection(label = "Appearance", modifier = modifier) {
        AppCard {
            SettingsRow(
                label = "Dark mode",
                sublabel = "Use the dark colour scheme instead of following your device.",
                trailing = { Switch(checked = darkTheme, onCheckedChange = onSetDarkTheme) },
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun ProfileAppearanceSectionPreview() {
    AppTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            ProfileAppearanceSection(
                darkTheme = true,
                onSetDarkTheme = {},
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}
