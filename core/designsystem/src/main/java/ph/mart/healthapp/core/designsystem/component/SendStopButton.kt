package ph.mart.healthapp.core.designsystem.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.designsystem.icon.AppIcons
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.core.designsystem.theme.Motion

/**
 * One 48dp circle in three states, and it is one composable rather than three so it cannot move
 * between them. The fill carries whether a send is available and the glyph carries what the tap
 * does; the spinner is the ring *around* the control rather than the control itself, which is what
 * lets a running turn still be stopped.
 *
 * Here since the FAB's quick log became the second composer to send to a model — the coach's
 * `ChatInputBar` was the first, and a model call either one starts can hang the same way.
 * [sendLabel] and [stopLabel] are the caller's: "Stop answering" is the coach's word for it and
 * would be the wrong one for a parse.
 *
 * The moves between the three are the quick log handoff's, and every composer that draws this gets
 * them: fill and glyph colour crossfade, the arrow and the stop swap by scale, and on the way out of
 * a call the ring goes first and the glyph follows — so the stop never looks tappable after the
 * call it stops has ended. Its own timings rather than [Motion]'s, as the quick log's are
 * (`DECISIONS.md` → the quick log redesign).
 */
@Composable
fun SendStopButton(
    sending: Boolean,
    canSend: Boolean,
    onSend: () -> Unit,
    onStop: () -> Unit,
    sendLabel: String,
    stopLabel: String,
    modifier: Modifier = Modifier,
) {
    val filled = !sending && canSend
    val colorSpec = tween<Color>(SWAP_MS, easing = Motion.Standard)
    val fill by animateColorAsState(
        targetValue = if (filled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest,
        animationSpec = colorSpec,
        label = "sendFill",
    )
    val arrowTint by animateColorAsState(
        targetValue = if (filled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.outline,
        animationSpec = colorSpec,
        label = "sendTint",
    )
    Box(contentAlignment = Alignment.Center, modifier = modifier) {
        Surface(
            onClick = if (sending) onStop else onSend,
            enabled = sending || canSend,
            shape = CircleShape,
            color = fill,
            modifier = Modifier.size(48.dp),
        ) {
            AnimatedContent(
                targetState = sending,
                contentAlignment = Alignment.Center,
                transitionSpec = {
                    // Leaving a call waits for the ring to go first.
                    val delay = if (targetState) 0 else RING_OUT_MS
                    scaleIn(tween(SWAP_MS, delay, Motion.Standard)) togetherWith
                        scaleOut(tween(SWAP_MS, delay, Motion.Standard))
                },
                label = "sendGlyph",
            ) { stop ->
                Icon(
                    imageVector = if (stop) AppIcons.Stop else AppIcons.ArrowUp,
                    contentDescription = if (stop) stopLabel else sendLabel,
                    tint = if (stop) MaterialTheme.colorScheme.onSurface else arrowTint,
                    modifier = Modifier.padding(8.dp).size(20.dp),
                )
            }
        }
        // Drawn over the circle rather than beside it, so the control keeps its 48dp and the arc
        // rides its own perimeter. It takes no pointer events, so the stop underneath still works.
        AnimatedVisibility(
            visible = sending,
            enter = fadeIn(tween(SWAP_MS, easing = Motion.Standard)),
            exit = fadeOut(tween(RING_OUT_MS, easing = Motion.Standard)),
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.outlineVariant,
            )
        }
    }
}

/** The handoff's figures: a glyph or colour swap, and the ring's quicker exit. */
private const val SWAP_MS = 150
private const val RING_OUT_MS = 100

/** Empty, ready, running — the three states side by side. */
@PreviewLightDark
@Composable
private fun SendStopButtonPreview() {
    AppTheme {
        Surface {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(16.dp)) {
                SendStopButton(sending = false, canSend = false, onSend = {}, onStop = {}, sendLabel = "Send", stopLabel = "Stop")
                SendStopButton(sending = false, canSend = true, onSend = {}, onStop = {}, sendLabel = "Send", stopLabel = "Stop")
                SendStopButton(sending = true, canSend = true, onSend = {}, onStop = {}, sendLabel = "Send", stopLabel = "Stop")
            }
        }
    }
}
