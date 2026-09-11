package ph.mart.healthapp.feature.profile.ui.settings.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.profile.R
import ph.mart.healthapp.feature.profile.ui.shared.components.AppListRow

private val AvatarSize = 40.dp

/** Smaller than an avatar because it carries no face — but the row around it is still the tap
 * target, so the touch area is unchanged. */
private val SwatchSize = 32.dp

/** One tap, one buddy — the pick reaches every mascot in the app through the theme, so there is
 * nothing to confirm and no second screen to open. The cells share the width evenly rather than
 * sitting at their natural size: five 56dp avatars overflowed a 360dp screen, and a scrolling row
 * would hide a buddy behind no affordance.
 *
 * Five is few enough to draw in the card, which is the whole reason this stays a row where
 * [SettingsColourPicker] became a door. The buddy chosen here is what the Profile header draws at
 * 64dp, which is what makes that header read as a profile at all — the app has no avatar and
 * deliberately no account to hang one on. */
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
 * The colour, on a row rather than in the card: thirty-five swatches is a grid, and a grid that
 * tall pushes Notifications, Connections and Data off the bottom of Settings. So the row carries
 * the answer — the current swatch and its name — and [MascotColourSheet] does the choosing.
 *
 * No [NavChevron]: that composable means "this row leaves the screen" everywhere else in Profile,
 * and a sheet does not leave the screen. The row is a `Role.Button` instead, and its
 * contentDescription names the colour that is on, because a swatch alone says nothing to anyone who
 * cannot separate the fills.
 *
 * `sheetOpen` lives here and nowhere else: which sheet is open is UI-only state with nothing on the
 * other side of it, so it never reaches `SettingsViewModel` — and keeping it here is what leaves
 * `SettingsAppearanceSection`'s signature, and everything above it, untouched.
 */
@Composable
internal fun SettingsColourPicker(
    selected: MascotPalette,
    onSelect: (MascotPalette) -> Unit,
    modifier: Modifier = Modifier,
) {
    var sheetOpen by remember { mutableStateOf(false) }
    val name = stringResource(selected.labelRes)
    // Resolved here rather than inside the semantics lambda, which cannot read a resource.
    val spoken = stringResource(R.string.profile_colour_cell, name)
    AppListRow(
        label = stringResource(R.string.profile_colour),
        sublabel = stringResource(R.string.profile_colour_sub),
        trailing = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Box(
                    modifier = Modifier
                        .size(SwatchSize)
                        .background(mascotSwatchColor(selected), CircleShape)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
                )
            }
        },
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(role = Role.Button) { sheetOpen = true }
            .semantics { contentDescription = spoken },
    )
    if (sheetOpen) {
        MascotColourSheet(
            selected = selected,
            onSelect = onSelect,
            onDismiss = { sheetOpen = false },
        )
    }
}

/** The heading over the buddy row's cells. */
@Composable
private fun PickerLabel(label: String, sublabel: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = sublabel,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** One cell of the buddy row. secondaryContainer is the app's "you are here" fill — the nav pill
 * and the SegmentedToggle chip already use it, and unlike a border it needs no per-character Shape
 * now that every silhouette is drawn on one canvas. It is also why no [MascotPalette] may take that
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
                SettingsBuddyPicker(selected = MascotCharacter.Lala, onSelect = {})
                SettingsColourPicker(selected = MascotPalette.Pink, onSelect = {})
            }
        }
    }
}
