package ph.mart.healthapp.feature.coach.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.designsystem.component.MascotAvatar
import ph.mart.healthapp.core.designsystem.component.MascotSpeechBubble
import ph.mart.healthapp.core.designsystem.component.MascotState
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.coach.R

/**
 * One turn. The coach's side is the mascot's own [MascotSpeechBubble] — a rounded bubble with a
 * left tail beside the avatar is exactly what this screen is, so nothing new was drawn for it, and
 * the buddy the user picked is the one that answers.
 *
 * The user's side is the mirror: `secondaryContainer`, right-aligned, no tail and no avatar. It
 * deliberately does *not* reuse the mascot bubble flipped — the tail points at whoever is
 * speaking, and there is no second face on this screen.
 */
@Composable
internal fun ChatBubble(text: String, fromUser: Boolean, modifier: Modifier = Modifier) {
    if (fromUser) {
        Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.widthIn(max = 280.dp),
            ) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                )
            }
        }
    } else {
        Row(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Top,
        ) {
            MascotAvatar(state = MascotState.Idle, size = 32.dp)
            MascotSpeechBubble(text = text)
        }
    }
}

/**
 * What a send that didn't land says. The reason is `onSurfaceVariant` rather than `error`: the
 * network dropping is not the user doing something wrong, and `error` is reserved for genuinely
 * off-track figures. [insight] is the rule-based line for the same day, so the screen still says
 * something true about today instead of only apologising.
 */
@Composable
internal fun FailureBubble(@StringRes reason: Int, insight: String?, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top,
    ) {
        MascotAvatar(state = MascotState.Sleepy, size = 32.dp)
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = stringResource(reason),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (insight != null) MascotSpeechBubble(text = insight)
        }
    }
}

@PreviewLightDark
@Composable
private fun ChatBubblePreview() {
    AppTheme {
        Surface {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ChatBubble(text = "Am I getting enough protein?", fromUser = true)
                ChatBubble(
                    text = "You're at 62 g of 150 g, so there's plenty of room. A high-protein " +
                        "dinner would close most of that gap.",
                    fromUser = false,
                )
                FailureBubble(
                    reason = R.string.coach_failure_offline,
                    insight = "You're 88g short on protein today.",
                )
            }
        }
    }
}
