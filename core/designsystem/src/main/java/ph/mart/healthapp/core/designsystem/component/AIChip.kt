package ph.mart.healthapp.core.designsystem.component

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.designsystem.icon.AppIcons
import ph.mart.healthapp.core.designsystem.theme.AppTheme

enum class AIChipVariant { Default, OnAccent }

/**
 * `Default`: [MaterialTheme.colorScheme.tertiaryContainer] bg — used on the photo confirmation
 * screen. `OnAccent`: [MaterialTheme.colorScheme.surfaceContainerLowest] bg, for placement on a
 * card that is *itself* a tertiaryContainer surface (Home's AIInsightCard) — the only two places
 * `tertiaryContainer` is used as a background in the app.
 */
@Composable
fun AIChip(label: String, variant: AIChipVariant, modifier: Modifier = Modifier) {
    val (containerColor, contentColor) = when (variant) {
        AIChipVariant.Default -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
        AIChipVariant.OnAccent -> MaterialTheme.colorScheme.surfaceContainerLowest to MaterialTheme.colorScheme.tertiary
    }
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = containerColor,
        contentColor = contentColor,
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(imageVector = AppIcons.AiSparkle, contentDescription = null, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.size(4.dp))
            Text(text = label, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@PreviewLightDark
@Composable
private fun AIChipPreview() {
    AppTheme {
        Surface {
            Row(modifier = Modifier.padding(16.dp)) {
                AIChip(label = "AI detected", variant = AIChipVariant.Default)
                Spacer(modifier = Modifier.size(12.dp))
                AIChip(label = "Insight", variant = AIChipVariant.OnAccent)
            }
        }
    }
}
