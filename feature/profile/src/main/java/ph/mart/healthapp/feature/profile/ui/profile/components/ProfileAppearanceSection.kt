package ph.mart.healthapp.feature.profile.ui.profile.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.designsystem.component.AppCard
import ph.mart.healthapp.core.designsystem.component.MascotAvatar
import ph.mart.healthapp.core.designsystem.component.MascotCharacter
import ph.mart.healthapp.core.designsystem.component.MascotPalette
import ph.mart.healthapp.core.designsystem.component.MascotState
import ph.mart.healthapp.core.designsystem.component.mascotSwatchColor
import ph.mart.healthapp.core.designsystem.theme.AppTheme

private val AvatarSize = 48.dp

/** Smaller than an avatar because it carries no face — but the cell around it is still the tap
 * target, so the row's touch area is unchanged. */
private val SwatchSize = 32.dp

/**
 * Until Dark mode is touched the profile stores null and the app follows the device, so [darkTheme]
 * is resolved by the caller rather than read straight off the profile — the switch has to show what
 * the user is actually looking at, not the absence of a choice. [mascot] and [palette] are resolved
 * the same way and for the same reason.
 */
@Composable
internal fun ProfileAppearanceSection(
    darkTheme: Boolean,
    onSetDarkTheme: (Boolean) -> Unit,
    mascot: MascotCharacter,
    onSelectMascot: (MascotCharacter) -> Unit,
    palette: MascotPalette,
    onSelectMascotPalette: (MascotPalette) -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingsSection(label = "Appearance", modifier = modifier) {
        AppCard {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                SettingsRow(
                    label = "Dark mode",
                    sublabel = "Use the dark colour scheme instead of following your device.",
                    trailing = { Switch(checked = darkTheme, onCheckedChange = onSetDarkTheme) },
                )
                MascotPickerRow(selected = mascot, onSelect = onSelectMascot)
                MascotColourRow(selected = palette, onSelect = onSelectMascotPalette)
            }
        }
    }
}

/** One tap, one buddy — the pick reaches every mascot in the app through the theme, so there is
 * nothing to confirm and no second screen to open. The cells share the width evenly rather than
 * sitting at their natural size: five 56dp avatars overflowed a 360dp screen, and a scrolling row
 * would hide a buddy behind no affordance. */
@Composable
private fun MascotPickerRow(
    selected: MascotCharacter,
    onSelect: (MascotCharacter) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SettingsRow(
            label = "Buddy",
            sublabel = "Who greets you on Home and turns up across the app.",
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MascotCharacter.entries.forEach { character ->
                val isSelected = character == selected
                PickerCell(selected = isSelected, onClick = { onSelect(character) }) {
                    MascotAvatar(state = MascotState.Happy, size = AvatarSize, character = character)
                    Text(
                        text = character.label,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.onSecondaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
            }
        }
    }
}

/** The same row, the other axis: one colour worn by every buddy, shown as a plain swatch. Every
 * circle carries an `outlineVariant` ring whatever its fill — [MascotPalette.Neutral] is a near
 * neighbour of the card behind it, and a swatch nobody can find is not a swatch.
 *
 * No visible label under these, unlike the buddies: the scheme flips in dark mode, so a hue name
 * would be wrong half the time. The palette's name rides a contentDescription instead, since a
 * circle gives TalkBack nothing to read. */
@Composable
private fun MascotColourRow(
    selected: MascotPalette,
    onSelect: (MascotPalette) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SettingsRow(
            label = "Colour",
            sublabel = "Every buddy wears the one you pick.",
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MascotPalette.entries.forEach { palette ->
                PickerCell(
                    selected = palette == selected,
                    onClick = { onSelect(palette) },
                    modifier = Modifier.semantics { contentDescription = "${palette.name} colour" },
                ) {
                    Box(
                        modifier = Modifier
                            .size(SwatchSize)
                            .background(mascotSwatchColor(palette), CircleShape)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
                    )
                }
            }
        }
    }
}

/** One cell of either row. secondaryContainer is the app's "you are here" fill — the nav pill and
 * the SegmentedToggle chip already use it, and unlike a border it needs no per-character Shape now
 * that every silhouette is drawn on one canvas. It is also why no [MascotPalette] may take that
 * role: a mascot that vanished the moment it was chosen is the one thing a picker must not do. */
@Composable
private fun RowScope.PickerCell(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
            .weight(1f)
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
            )
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(vertical = 8.dp),
        content = content,
    )
}

@PreviewLightDark
@Composable
private fun ProfileAppearanceSectionPreview() {
    AppTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            ProfileAppearanceSection(
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
