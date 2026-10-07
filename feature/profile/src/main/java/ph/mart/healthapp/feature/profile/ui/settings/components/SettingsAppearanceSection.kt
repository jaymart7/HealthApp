package ph.mart.healthapp.feature.profile.ui.settings.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.designsystem.component.AppCard
import ph.mart.healthapp.core.designsystem.component.MascotCharacter
import ph.mart.healthapp.core.designsystem.component.MascotPalette
import ph.mart.healthapp.core.designsystem.component.SegmentedToggle
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.profile.R

/**
 * [darkTheme] is the profile's own nullable field, unresolved: null is "follow the device", and it is
 * one of the three choices rather than a state the first tap destroys. A switch could only write
 * true or false, so once touched there was no way back to the device. [mascot] and [palette] are
 * resolved by the caller, since each always has exactly one value to show.
 *
 * Three blocks, divided: what scheme the app wears, who greets you, and what colour they wear. The
 * dividers are what stop the two picker rows reading as one ten-cell grid.
 */
@Composable
internal fun SettingsAppearanceSection(
    darkTheme: Boolean?,
    onSetDarkTheme: (Boolean?) -> Unit,
    mascot: MascotCharacter,
    onSelectMascot: (MascotCharacter) -> Unit,
    palette: MascotPalette,
    onSelectMascotPalette: (MascotPalette) -> Unit,
    modifier: Modifier = Modifier,
) {
    AppCard(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            ThemeChoice(darkTheme = darkTheme, onSetDarkTheme = onSetDarkTheme)
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            SettingsBuddyPicker(selected = mascot, onSelect = onSelectMascot)
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            SettingsColourPicker(selected = palette, onSelect = onSelectMascotPalette)
        }
    }
}

/** Device, light, dark — the order the field's null, false and true read in. */
private val ThemeChoices = listOf(null, false, true)

/** Units' shape: a title, a sublabel and the toggle under them. */
@Composable
private fun ThemeChoice(darkTheme: Boolean?, onSetDarkTheme: (Boolean?) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = stringResource(R.string.profile_dark_mode),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(R.string.profile_dark_mode_sub),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        SegmentedToggle(
            options = listOf(
                stringResource(R.string.profile_theme_device),
                stringResource(R.string.profile_theme_light),
                stringResource(R.string.profile_theme_dark),
            ),
            selectedIndex = ThemeChoices.indexOf(darkTheme),
            onSelect = { index -> onSetDarkTheme(ThemeChoices[index]) },
            trackColor = MaterialTheme.colorScheme.surfaceContainer,
        )
    }
}

@PreviewLightDark
@Composable
private fun SettingsAppearanceSectionPreview() {
    AppTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            SettingsAppearanceSection(
                darkTheme = null,
                onSetDarkTheme = {},
                mascot = MascotCharacter.Lala,
                onSelectMascot = {},
                palette = MascotPalette.Contrast,
                onSelectMascotPalette = {},
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}
