package ph.mart.healthapp.feature.home.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.designsystem.component.AIChip
import ph.mart.healthapp.core.designsystem.component.AIChipVariant
import ph.mart.healthapp.core.designsystem.icon.AppIcons
import ph.mart.healthapp.core.designsystem.theme.AppTheme

/** The only `tertiaryContainer` card on Home — hence [AIChipVariant.OnAccent], which was built in
 * Phase 2 for exactly this placement. [text] is derived from the day's real numbers upstream, not
 * a stored string. */
@Composable
fun AIInsightCard(text: String, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    HomeCard(modifier = modifier, color = MaterialTheme.colorScheme.tertiaryContainer) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AIChip(label = "Insight", variant = AIChipVariant.OnAccent)
            IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                Icon(
                    imageVector = AppIcons.Close,
                    contentDescription = "Dismiss insight",
                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onTertiaryContainer,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@PreviewLightDark
@Composable
private fun AIInsightCardPreview() {
    AppTheme {
        Surface {
            AIInsightCard(
                text = "You're 42g short on protein today.",
                onDismiss = {},
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}
