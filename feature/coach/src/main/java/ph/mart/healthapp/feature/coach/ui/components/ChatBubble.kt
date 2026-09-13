package ph.mart.healthapp.feature.coach.ui.components

import androidx.annotation.StringRes
import android.content.ClipData
import android.content.ClipboardManager
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.designsystem.component.MascotAvatar
import ph.mart.healthapp.core.designsystem.component.MascotSpeechBubble
import ph.mart.healthapp.core.designsystem.component.MascotState
import ph.mart.healthapp.core.designsystem.component.shareText
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
 *
 * [announce] marks this bubble a polite live region, and the screen sets it on the newest coach
 * message only: a finished answer arriving is the one thing on this screen a screen reader user
 * would otherwise have to go looking for. Deliberately *not* on [StreamingBubble] — a live region
 * over text that grows per chunk makes TalkBack restart the whole answer on every chunk.
 *
 * **Long-pressing the coach's side opens Copy / Share / Ask again**, the last only when
 * [onAskAgain] is non-null. The user's own side has no menu: their question is already theirs, and
 * the one thing worth doing to it — asking it again — is what the answer's menu does.
 */
@Composable
internal fun ChatBubble(
    text: String,
    fromUser: Boolean,
    modifier: Modifier = Modifier,
    announce: Boolean = false,
    onAskAgain: (() -> Unit)? = null,
) {
    val announced = if (announce) {
        modifier.semantics { liveRegion = LiveRegionMode.Polite }
    } else {
        modifier
    }
    if (fromUser) {
        Row(modifier = announced.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
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
            modifier = announced.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Top,
        ) {
            MascotAvatar(state = MascotState.Idle, size = 32.dp)
            AnswerActions(text = text, onAskAgain = onAskAgain) { open ->
                MascotSpeechBubble(
                    text = text,
                    modifier = Modifier.combinedClickable(
                        // A plain tap does nothing: the bubble is text, not a control, and the
                        // ripple is what says the long press is there at all.
                        onClick = {},
                        onLongClick = open,
                        onLongClickLabel = stringResource(R.string.coach_bubble_actions),
                    ),
                )
            }
        }
    }
}

/**
 * The menu behind a long press on an answer.
 *
 * The clipboard is the platform's, not Compose's: `LocalClipboardManager` is deprecated and its
 * replacement is a suspending API with a moving shape, while two lines of `ClipData` have been
 * stable for a decade. Android 13 and up show their own "copied" confirmation, which is why
 * nothing here raises a snackbar.
 */
@Composable
private fun AnswerActions(
    text: String,
    onAskAgain: (() -> Unit)?,
    bubble: @Composable (open: () -> Unit) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    val context = LocalContext.current
    Box {
        bubble { open = true }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.coach_bubble_copy)) },
                onClick = {
                    context.getSystemService(ClipboardManager::class.java)
                        ?.setPrimaryClip(ClipData.newPlainText(null, text))
                    open = false
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.coach_bubble_share)) },
                onClick = {
                    shareText(context, text)
                    open = false
                },
            )
            if (onAskAgain != null) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.coach_bubble_ask_again)) },
                    onClick = {
                        onAskAgain()
                        open = false
                    },
                )
            }
        }
    }
}

/**
 * The answer as it arrives, and the wait before it: [text] is null until the first chunk lands.
 *
 * One `Row` across both, deliberately — the avatar stays a single node for the whole turn, so
 * `MascotAvatar`'s state spring plays the `Thinking → Idle` move when the answer starts instead of
 * remounting as a cut. It is the coach's side of [ChatBubble], which is what the finished turn
 * becomes once Room has the rows.
 */
@Composable
internal fun StreamingBubble(text: String?, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top,
    ) {
        MascotAvatar(
            state = if (text == null) MascotState.Thinking else MascotState.Idle,
            size = 32.dp,
        )
        if (text != null) MascotSpeechBubble(text = text)
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
                StreamingBubble(text = null)
                StreamingBubble(text = "You're at 62 g of 150 g, so there's plenty of")
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
