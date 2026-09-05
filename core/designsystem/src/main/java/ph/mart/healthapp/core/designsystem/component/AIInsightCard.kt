package ph.mart.healthapp.core.designsystem.component

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.designsystem.R
import ph.mart.healthapp.core.designsystem.icon.AppIcons
import ph.mart.healthapp.core.designsystem.theme.AppTheme

/**
 * The AI accent card — `tertiaryContainer` + `onTertiaryContainer`, hence [AIChipVariant.OnAccent],
 * which was built in Phase 2 for exactly this placement. [text] is always derived from real numbers
 * upstream, never a stored string.
 *
 * Promoted out of `:feature:home` when Progress's overview and its Weight detail drew the identical
 * card, per CLAUDE.md's "used in ≥2 screens → `:core:designsystem`, never duplicated" rule — the
 * path [AppCard] and [BadgeDot] took. It stays the app's only `tertiaryContainer` background
 * besides [AIChip]'s own `Default` variant, and each screen draws **one** of it.
 *
 * The two optional parameters are what let one card serve both callers without a fork: Home's
 * insight is dismissible and has no second line, Progress's has a sub-line and nothing to dismiss.
 * [headlineStyle] defaults to Home's `bodyMedium` so its card is unchanged by the move; Progress
 * passes `titleMedium`, where the headline *is* the card.
 */
@Composable
fun AIInsightCard(
    text: String,
    modifier: Modifier = Modifier,
    subline: String? = null,
    onDismiss: (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    headlineStyle: TextStyle = MaterialTheme.typography.bodyMedium,
) {
    AppCard(
        modifier = modifier,
        color = MaterialTheme.colorScheme.tertiaryContainer,
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AIChip(label = stringResource(R.string.ds_insight), variant = AIChipVariant.OnAccent)
            if (onDismiss != null) {
                IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                    Icon(
                        imageVector = AppIcons.Close,
                        contentDescription = stringResource(R.string.ds_insight_dismiss),
                        tint = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
        Text(
            text = text,
            style = headlineStyle,
            color = MaterialTheme.colorScheme.onTertiaryContainer,
            modifier = Modifier.padding(top = 8.dp),
        )
        if (subline != null) {
            Text(
                text = subline,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
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

/** Progress's shape: a headline that carries the card, a sub-line, and nothing to dismiss. */
@PreviewLightDark
@Composable
private fun AIInsightCardWithSublinePreview() {
    AppTheme {
        Surface {
            AIInsightCard(
                text = "On the last 30 days' trend, you'll hit 72.0 kg around 21 Sep.",
                subline = "Trending 0.4 kg down per week.",
                headlineStyle = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}
