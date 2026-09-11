package ph.mart.healthapp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.R
import ph.mart.healthapp.core.designsystem.component.AppBottomSheet
import ph.mart.healthapp.core.designsystem.icon.AppIcons
import ph.mart.healthapp.core.designsystem.theme.AppTheme

/** Above this font scale a 40dp leading slot beside a wrapped two-line label is no longer
 * something to centre on — the glyph belongs against the label's first line. */
private const val TOP_ALIGN_FONT_SCALE = 1.3f

/** The FAB's quick-action sheet: Say what you ate / Scan a barcode / Log food / Log exercise /
 * Add photo / Log weight, each routing to a real destination.
 *
 * **The first three are the diary chip row's three food doors**, in its order and drawing its
 * three icons — say, scan, photograph. `Log food` is the camera: it routes to `FoodCaptureRoute`,
 * the same place `onCapturePhoto` sends the chip, which is also what the launcher shortcut's own
 * long label ("Photograph a meal") says. That row scrolls, so once the chips are off screen this
 * sheet is where those three flows still live, and a user who learned them as "say, scan, photo"
 * should not have to relearn the order here.
 *
 * The last three are not food at all — an exercise, a *body* progress photo (`AddPhotoSheet` from
 * `:feature:progress`, no AI and no plate), and a weigh-in. So the rule sits at 3|4, which is the
 * only place a straight line separates the two kinds. The three above it wear a
 * `tertiaryContainer` badge, the three below a bare glyph; nothing needs an exception.
 *
 * The sentence sits above the camera because it is the path that works with the plate already
 * cleared, and because it is the only one of the two that can log a whole meal at once.
 *
 * Nothing about `ShortcutAction` changes — a launcher shortcut for a scan is its own decision, not
 * a consequence of this row existing. */
@Composable
fun QuickActionSheet(
    onDismiss: () -> Unit,
    onSpeakFood: () -> Unit,
    onScanBarcode: () -> Unit,
    onLogFood: () -> Unit,
    onLogExercise: () -> Unit,
    onLogWeight: () -> Unit,
    onAddPhoto: () -> Unit,
) {
    // Zero gutter, and every row pads itself: a row's pressed state layer runs the sheet's full
    // width rather than stopping 16dp short of each edge.
    AppBottomSheet(onDismiss = onDismiss, horizontalPadding = 0.dp) {
        QuickActionRow(
            label = stringResource(R.string.app_quick_speak_food),
            supporting = stringResource(R.string.app_quick_speak_food_detail),
            icon = AppIcons.Mic,
            badged = true,
            onClick = onSpeakFood,
        )
        QuickActionRow(
            label = stringResource(R.string.app_quick_scan_barcode),
            icon = AppIcons.Barcode,
            badged = true,
            onClick = onScanBarcode,
        )
        QuickActionRow(
            label = stringResource(R.string.app_quick_log_food),
            supporting = stringResource(R.string.app_quick_log_food_detail),
            icon = AppIcons.Camera,
            badged = true,
            onClick = onLogFood,
        )
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant,
            // The rule keeps the gutter the rows gave up, or it reads as a full-bleed separator
            // between two sheets rather than a seam inside one list.
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
        QuickActionRow(
            label = stringResource(R.string.app_quick_log_exercise),
            icon = AppIcons.Dumbbell,
            onClick = onLogExercise,
        )
        QuickActionRow(
            label = stringResource(R.string.app_quick_add_photo),
            supporting = stringResource(R.string.app_quick_add_photo_detail),
            icon = AppIcons.AddPhoto,
            onClick = onAddPhoto,
        )
        QuickActionRow(
            label = stringResource(R.string.app_quick_log_weight),
            icon = AppIcons.Weight,
            onClick = onLogWeight,
        )
    }
}

/**
 * One row. [badged] fills the leading slot with a `tertiaryContainer` circle — the three food
 * doors — and leaves it bare otherwise.
 *
 * The leading slot is 40dp wide in **both** cases, so a badged row and a bare one share one
 * optical column and the labels line up down the whole sheet.
 */
@Composable
private fun QuickActionRow(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    supporting: String? = null,
    badged: Boolean = false,
) {
    Surface(
        onClick = onClick,
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = if (LocalDensity.current.fontScale > TOP_ALIGN_FONT_SCALE) {
                Alignment.Top
            } else {
                Alignment.CenterVertically
            },
            // A floor, not a height: a wrapped label grows the row rather than clipping.
            modifier = Modifier
                .heightIn(min = if (supporting != null) 56.dp else 48.dp)
                .padding(horizontal = 16.dp, vertical = 4.dp),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(40.dp)
                    .then(
                        if (badged) {
                            Modifier
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.tertiaryContainer)
                        } else {
                            Modifier
                        },
                    ),
            ) {
                Icon(
                    imageVector = icon,
                    // The label beside it already says this. A description here would have
                    // TalkBack read the word twice for one target — the row is one node.
                    contentDescription = null,
                    tint = if (badged) {
                        MaterialTheme.colorScheme.onTertiaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.size(24.dp),
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (supporting != null) {
                    Text(
                        text = supporting,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@PreviewLightDark
@Preview(name = "200% font", fontScale = 2f)
@Composable
private fun QuickActionSheetPreview() {
    AppTheme {
        QuickActionSheet(
            onDismiss = {},
            onSpeakFood = {},
            onScanBarcode = {},
            onLogFood = {},
            onLogExercise = {},
            onLogWeight = {},
            onAddPhoto = {},
        )
    }
}
