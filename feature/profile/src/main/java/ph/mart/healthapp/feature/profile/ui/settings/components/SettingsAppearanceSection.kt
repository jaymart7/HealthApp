package ph.mart.healthapp.feature.profile.ui.settings.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.designsystem.component.AppCard
import ph.mart.healthapp.core.designsystem.component.MascotCharacter
import ph.mart.healthapp.core.designsystem.component.MascotPalette
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.profile.R
import ph.mart.healthapp.feature.profile.ui.shared.components.AppListRow

/**
 * Until Dark mode is touched the profile stores null and the app follows the device, so [darkTheme]
 * is resolved by the caller rather than read straight off the profile — the switch has to show what
 * the user is actually looking at, not the absence of a choice. [mascot] and [palette] are resolved
 * the same way and for the same reason.
 *
 * Three blocks, divided: what scheme the app wears, who greets you, and what colour they wear. The
 * dividers are what stop the two picker rows reading as one ten-cell grid.
 */
@Composable
internal fun SettingsAppearanceSection(
    darkTheme: Boolean,
    onSetDarkTheme: (Boolean) -> Unit,
    mascot: MascotCharacter,
    onSelectMascot: (MascotCharacter) -> Unit,
    palette: MascotPalette,
    onSelectMascotPalette: (MascotPalette) -> Unit,
    modifier: Modifier = Modifier,
) {
    AppCard(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            AppListRow(
                label = stringResource(R.string.profile_dark_mode),
                sublabel = stringResource(R.string.profile_dark_mode_sub),
                trailing = { Switch(checked = darkTheme, onCheckedChange = onSetDarkTheme) },
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            SettingsBuddyPicker(selected = mascot, onSelect = onSelectMascot)
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            SettingsColourPicker(selected = palette, onSelect = onSelectMascotPalette)
        }
    }
}

@PreviewLightDark
@Composable
private fun SettingsAppearanceSectionPreview() {
    AppTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            SettingsAppearanceSection(
                darkTheme = true,
                onSetDarkTheme = {},
                mascot = MascotCharacter.Sprig,
                onSelectMascot = {},
                palette = MascotPalette.Contrast,
                onSelectMascotPalette = {},
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}
