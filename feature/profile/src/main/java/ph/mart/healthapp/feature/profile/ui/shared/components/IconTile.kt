package ph.mart.healthapp.feature.profile.ui.shared.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.designsystem.icon.AppIcons
import ph.mart.healthapp.core.designsystem.theme.AppTheme

/**
 * A 40dp rounded square holding one 20dp glyph — the leading half of an [AppListRow].
 *
 * Two fills, and the difference carries meaning rather than decoration: [accent] true is
 * `secondaryContainer`, the app's "this goes somewhere" tone, worn by every row that pushes a
 * route; false is `surfaceContainer`, worn by the Day-targets rows, which stay put and are only
 * labelling their own stepper. A tile is never the tap target — the row around it is.
 */
@Composable
internal fun IconTile(
    icon: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    accent: Boolean = true,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (accent) {
                    MaterialTheme.colorScheme.secondaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainer
                },
            ),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (accent) {
                MaterialTheme.colorScheme.onSecondaryContainer
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.size(20.dp),
        )
    }
}

@PreviewLightDark
@Composable
private fun IconTilePreview() {
    AppTheme {
        Surface {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(16.dp),
            ) {
                IconTile(icon = AppIcons.Supplement, contentDescription = null)
                IconTile(icon = AppIcons.Water, contentDescription = null, accent = false)
            }
        }
    }
}
