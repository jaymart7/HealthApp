package ph.mart.healthapp.feature.progress.ui.shared.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.core.designsystem.theme.tabularNums

/** [Small] is a grid tile's corner; [Medium] is a full-frame one — compare, timelapse, the PNG. */
internal enum class OverlayLabelSize { Small, Medium }

/**
 * The pill that sits over a progress photo — a date in the corner of a grid tile or a comparison
 * frame, a weight in the corner of a timelapse one. One component rather than three near-identical
 * private ones, so the surfaces can't drift apart in how a caption reads over an image.
 *
 * The plate is **opaque** `surfaceContainerLowest` with `onSurface` text, never a translucent
 * scrim over muted text: a photo can be light or dark under any corner of any frame, and an
 * alpha-blended label is legible over exactly one of those. It is the plate that guarantees the
 * contrast, so the type on it needs no alpha of its own.
 *
 * [tabular] is for a number: a weight jittering as the player cycles frames is exactly what
 * tabular figures exist to stop.
 */
@Composable
internal fun PhotoOverlayLabel(
    text: String,
    modifier: Modifier = Modifier,
    size: OverlayLabelSize = OverlayLabelSize.Medium,
    tabular: Boolean = false,
) {
    val style = when (size) {
        OverlayLabelSize.Small -> MaterialTheme.typography.labelSmall
        OverlayLabelSize.Medium -> MaterialTheme.typography.labelMedium
    }
    val horizontal = if (size == OverlayLabelSize.Small) 8.dp else 12.dp
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = RoundedCornerShape(8.dp),
        shadowElevation = 1.dp,
        modifier = modifier,
    ) {
        Text(
            text = text,
            style = if (tabular) style.tabularNums else style,
            modifier = Modifier.padding(horizontal = horizontal, vertical = 4.dp),
        )
    }
}

@PreviewLightDark
@Composable
private fun PhotoOverlayLabelPreview() {
    AppTheme {
        Surface {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PhotoOverlayLabel(text = "12 Mar", size = OverlayLabelSize.Small)
                PhotoOverlayLabel(text = "12 Mar 2026")
                PhotoOverlayLabel(text = "76.9 kg", tabular = true)
            }
        }
    }
}
