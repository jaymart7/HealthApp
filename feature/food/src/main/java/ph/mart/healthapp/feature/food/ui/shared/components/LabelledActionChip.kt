package ph.mart.healthapp.feature.food.ui.shared.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.designsystem.icon.AppIcons
import ph.mart.healthapp.core.designsystem.theme.AppTheme

/**
 * An icon with its name under a 1dp outline — the diary's three logging doors.
 *
 * It replaced three bare `IconButton`s in the pinned area, which asked the user to know what a
 * microphone, a QR square and a camera each did to a *diary* before tapping one. The label is the
 * whole point; the icon is what makes the row scannable once the label has been read once.
 *
 * **Never filled.** The docked FAB is the one filled affordance on this screen, and a chip that
 * matched it would be a second primary action competing with it. Transparent on `surface` with an
 * `outlineVariant` border is the quietest container the system has that is still a container.
 *
 * Feature-local rather than in `:core:designsystem`: one screen draws it. It moves there the day a
 * second one does, per the ≥2-screens rule — not on the promise of one.
 */
@Composable
internal fun LabelledActionChip(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = Color.Transparent,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier.heightIn(min = 48.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
            // 48dp is the floor, not the height: a third of a narrow screen holds "Photo" at the
            // default font scale and not much past it, so the chip grows rather than clipping.
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
        ) {
            Icon(
                imageVector = icon,
                // The label beside it already says this. A description here would have TalkBack
                // read the word twice for one target.
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun LabelledActionChipRowPreview() {
    AppTheme {
        Surface {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().padding(16.dp),
            ) {
                LabelledActionChip(label = "Say it", icon = AppIcons.Mic, onClick = {}, modifier = Modifier.weight(1f))
                LabelledActionChip(label = "Scan", icon = AppIcons.Barcode, onClick = {}, modifier = Modifier.weight(1f))
                LabelledActionChip(label = "Photo", icon = AppIcons.Camera, onClick = {}, modifier = Modifier.weight(1f))
            }
        }
    }
}
