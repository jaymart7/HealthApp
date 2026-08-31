package ph.mart.healthapp.feature.profile.ui.profile.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
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
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.designsystem.component.AppCard
import ph.mart.healthapp.core.designsystem.component.MascotAvatar
import ph.mart.healthapp.core.designsystem.component.MascotCharacter
import ph.mart.healthapp.core.designsystem.component.MascotState
import ph.mart.healthapp.core.designsystem.theme.AppTheme

private val AvatarSize = 48.dp

/**
 * Until Dark mode is touched the profile stores null and the app follows the device, so [darkTheme]
 * is resolved by the caller rather than read straight off the profile — the switch has to show what
 * the user is actually looking at, not the absence of a choice. [mascot] is resolved the same way
 * and for the same reason.
 */
@Composable
internal fun ProfileAppearanceSection(
    darkTheme: Boolean,
    onSetDarkTheme: (Boolean) -> Unit,
    mascot: MascotCharacter,
    onSelectMascot: (MascotCharacter) -> Unit,
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
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    // secondaryContainer is the app's "you are here" fill — the nav pill and the
                    // SegmentedToggle chip already use it, and unlike a border it needs no
                    // per-character Shape now that every silhouette is drawn on one canvas.
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (isSelected) {
                                MaterialTheme.colorScheme.secondaryContainer
                            } else {
                                Color.Transparent
                            },
                        )
                        .selectable(
                            selected = isSelected,
                            role = Role.RadioButton,
                            onClick = { onSelect(character) },
                        )
                        .padding(vertical = 8.dp),
                ) {
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
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}
