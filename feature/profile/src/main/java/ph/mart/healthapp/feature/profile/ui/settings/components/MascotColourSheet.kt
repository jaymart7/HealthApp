package ph.mart.healthapp.feature.profile.ui.settings.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import ph.mart.healthapp.core.designsystem.component.AppBottomSheet
import ph.mart.healthapp.core.designsystem.component.MascotPalette
import ph.mart.healthapp.core.designsystem.component.mascotFeatureColor
import ph.mart.healthapp.core.designsystem.component.mascotSwatchColor
import ph.mart.healthapp.core.designsystem.icon.AppIcons
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.profile.R

private val SwatchSize = 44.dp
private val CellSize = 56.dp

/**
 * Every colour a buddy can wear, in the order [MascotPalette] declares them: the five that resolve
 * from the theme first, then thirty fixed hues walking the wheel once, so the grid reads as a
 * spectrum rather than a bag.
 *
 * A `FlowRow`, never a `LazyVerticalGrid` — [AppBottomSheet] hands its children unbounded height,
 * so a lazy list inside one cannot measure. Thirty-five 56dp cells wrap to six rows on a 360dp
 * phone, which is a sheet, not a screen.
 *
 * Picking applies immediately and leaves the sheet open: the choice reaches every mascot in the app
 * through the theme, so the buddy on the row behind the scrim is the preview, and closing to see
 * what you picked would be the only way to see what you picked. Back dismisses the sheet rather
 * than the screen under it, which comes from [AppBottomSheet]'s `ModalBottomSheet` — no separate
 * handler to wire, the same as `SupplementEditSheet`.
 */
@Composable
internal fun MascotColourSheet(
    selected: MascotPalette,
    onSelect: (MascotPalette) -> Unit,
    onDismiss: () -> Unit,
) {
    AppBottomSheet(onDismiss = onDismiss) {
        Text(
            text = stringResource(R.string.profile_colour),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = stringResource(R.string.profile_colour_sub),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            MascotPalette.entries.forEach { palette ->
                ColourCell(
                    palette = palette,
                    selected = palette == selected,
                    onClick = { onSelect(palette) },
                )
            }
        }
    }
}

/**
 * One swatch. Its own cell rather than `SettingsMascotPickers`' `PickerCell`, which is `RowScope`-
 * bound and shares the width by weight — right for a row of five buddies, wrong for a grid that has
 * to wrap.
 *
 * The `outlineVariant` ring is on every circle whatever its fill, for the reason it always was: the
 * palest hues and [MascotPalette.Neutral] are near neighbours of the sheet behind them, and a
 * swatch nobody can find is not a swatch. The name rides `contentDescription` rather than sitting
 * under the circle — thirty-five labels is a wall of text, and the chosen one is printed on the row
 * that opened this sheet.
 */
@Composable
private fun ColourCell(palette: MascotPalette, selected: Boolean, onClick: () -> Unit) {
    // Resolved here rather than inside the semantics lambda, which cannot read a resource.
    val spoken = stringResource(R.string.profile_colour_cell, stringResource(palette.labelRes))
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(CellSize)
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .semantics { contentDescription = spoken },
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(SwatchSize)
                .background(mascotSwatchColor(palette), CircleShape)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
        ) {
            if (selected) {
                Icon(
                    imageVector = AppIcons.Check,
                    contentDescription = null,
                    tint = mascotFeatureColor(palette),
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun MascotColourSheetPreview() {
    AppTheme {
        MascotColourSheet(selected = MascotPalette.Pink, onSelect = {}, onDismiss = {})
    }
}
