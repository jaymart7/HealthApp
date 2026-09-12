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

/**
 * The pill that sits over a progress photo — a date in the corner of a comparison frame, a date or
 * a weight in the corner of a timelapse one. One component rather than two near-identical private
 * ones, so the two overlays can't drift apart in how a caption reads over an image.
 *
 * [tabular] is for a number: a weight jittering as the player cycles frames is exactly what
 * tabular figures exist to stop.
 */
@Composable
internal fun PhotoOverlayLabel(text: String, modifier: Modifier = Modifier, tabular: Boolean = false) {
    Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(8.dp), modifier = modifier) {
        Text(
            text = text,
            style = if (tabular) MaterialTheme.typography.labelMedium.tabularNums else MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}

@PreviewLightDark
@Composable
private fun PhotoOverlayLabelPreview() {
    AppTheme {
        Surface {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PhotoOverlayLabel(text = "12 Mar")
                PhotoOverlayLabel(text = "76.9 kg", tabular = true)
            }
        }
    }
}
