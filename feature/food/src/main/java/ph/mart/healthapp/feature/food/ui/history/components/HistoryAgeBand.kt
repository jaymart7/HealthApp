package ph.mart.healthapp.feature.food.ui.history.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ph.mart.healthapp.core.designsystem.theme.AppTheme

/**
 * The coarsest heading in the list — "This week", "Earlier this month", "August".
 *
 * A rule runs off the end of the label rather than the label sitting alone, because this has to
 * read as *above* the day headings beneath it while being quieter than all of them. Uppercase and
 * tracked-out does that without spending a larger size on it.
 */
@Composable
internal fun HistoryAgeBand(label: String, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .height(28.dp)
            .padding(horizontal = 16.dp),
    ) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Medium,
                // Tracked out, because uppercase at 11sp is a solid block otherwise.
                letterSpacing = 0.9.sp,
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        HorizontalDivider(
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant,
            modifier = Modifier.weight(1f),
        )
    }
}

@PreviewLightDark
@Composable
private fun HistoryAgeBandPreview() {
    AppTheme {
        Surface {
            HistoryAgeBand(label = "Earlier this month")
        }
    }
}
