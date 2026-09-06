package ph.mart.healthapp.feature.profile.ui.settings.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.designsystem.component.MascotAvatar
import ph.mart.healthapp.core.designsystem.component.MascotCharacter
import ph.mart.healthapp.core.designsystem.component.MascotPalette
import ph.mart.healthapp.core.designsystem.component.MascotState
import ph.mart.healthapp.core.designsystem.component.mascotSwatchColor
import ph.mart.healthapp.core.designsystem.icon.AppIcons
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.profile.R

private val AvatarSize = 40.dp

/** Smaller than an avatar because it carries no face — but the cell around it is still the tap
 * target, so the row's touch area is unchanged. */
private val SwatchSize = 32.dp

/** One tap, one buddy — the pick reaches every mascot in the app through the theme, so there is
 * nothing to confirm and no second screen to open. The cells share the width evenly rather than
 * sitting at their natural size: five 56dp avatars overflowed a 360dp screen, and a scrolling row
 * would hide a buddy behind no affordance.
 *
 * The buddy chosen here is what the Profile header draws at 64dp, which is what makes that header
 * read as a profile at all — the app has no avatar and deliberately no account to hang one on. */
@Composable
internal fun SettingsBuddyPicker(
    selected: MascotCharacter,
    onSelect: (MascotCharacter) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = modifier) {
        PickerLabel(
            label = stringResource(R.string.profile_buddy),
            sublabel = stringResource(R.string.profile_buddy_sub),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MascotCharacter.entries.forEach { character ->
                val isSelected = character == selected
                PickerCell(selected = isSelected, onClick = { onSelect(character) }) {
                    MascotAvatar(state = MascotState.Happy, size = AvatarSize, character = character)
                    Text(
                        text = character.label,
                        style = MaterialTheme.typography.labelSmall,
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

/**
 * The same row, the other axis: one colour worn by every buddy, shown as a plain swatch. Every
 * circle carries an `outlineVariant` ring whatever its fill — [MascotPalette.Neutral] is a near
 * neighbour of the card behind it, and a swatch nobody can find is not a swatch.
 *
 * The selected palette's **name is printed beside the label**, and the chosen swatch also carries a
 * check: a row of five circles that says which one is on by fill alone is a row that means nothing
 * to anyone who cannot separate the fills. There is still no label *under* each swatch — the scheme
 * flips in dark mode, so a hue name would be wrong half the time — so every cell keeps the name on
 * its contentDescription as well.
 */
@Composable
internal fun SettingsColourPicker(
    selected: MascotPalette,
    onSelect: (MascotPalette) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = modifier) {
        PickerLabel(
            label = stringResource(R.string.profile_colour),
            sublabel = stringResource(R.string.profile_colour_sub),
            trailingValue = stringResource(selected.label()),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MascotPalette.entries.forEach { palette ->
                val isSelected = palette == selected
                // Resolved here rather than inside the semantics lambda, which cannot read a
                // resource — the same one-line-above pattern every clearAndSetSemantics in this
                // app uses.
                val spoken = stringResource(R.string.profile_colour_cell, stringResource(palette.label()))
                PickerCell(
                    selected = isSelected,
                    onClick = { onSelect(palette) },
                    modifier = Modifier.semantics { contentDescription = spoken },
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(SwatchSize)
                            .background(mascotSwatchColor(palette), CircleShape)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
                    ) {
                        if (isSelected) {
                            Icon(
                                imageVector = AppIcons.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * The five palette names. Resources rather than the enum's `name`, which is a persisted token —
 * `mascotPaletteName` on the profile row is that string, and translating it would rename the
 * stored value. Deliberately the existing vocabulary (Soft / Bold / Muted / Contrast / Neutral)
 * rather than new hue names: these describe how strongly the buddy is drawn, not what colour it
 * is, which is the only description that stays true when the scheme flips in dark mode.
 */
@androidx.annotation.StringRes
private fun MascotPalette.label(): Int = when (this) {
    MascotPalette.Soft -> R.string.profile_settings_colour_soft
    MascotPalette.Bold -> R.string.profile_settings_colour_bold
    MascotPalette.Muted -> R.string.profile_settings_colour_muted
    MascotPalette.Contrast -> R.string.profile_settings_colour_contrast
    MascotPalette.Neutral -> R.string.profile_settings_colour_neutral
}

/** The heading over either picker's row of cells: a label, a sublabel, and — for colour — the name
 * of what is currently chosen, so the swatches are never the only thing carrying the answer. */
@Composable
private fun PickerLabel(label: String, sublabel: String, trailingValue: String? = null) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (trailingValue != null) {
                Text(
                    text = trailingValue,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Text(
            text = sublabel,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
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
            .heightIn(min = 48.dp)
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
private fun SettingsBuddyPickerPreview() {
    AppTheme {
        Surface(color = MaterialTheme.colorScheme.surfaceContainerLow) {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(16.dp),
            ) {
                SettingsBuddyPicker(selected = MascotCharacter.Sprig, onSelect = {})
                SettingsColourPicker(selected = MascotPalette.Contrast, onSelect = {})
            }
        }
    }
}
