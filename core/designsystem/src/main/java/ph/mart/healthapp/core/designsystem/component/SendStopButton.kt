package ph.mart.healthapp.core.designsystem.component

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.designsystem.icon.AppIcons
import ph.mart.healthapp.core.designsystem.theme.AppTheme

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
    Box(contentAlignment = Alignment.Center, modifier = modifier) {
        Surface(
            onClick = if (sending) onStop else onSend,
            enabled = sending || canSend,
            shape = CircleShape,
            color = if (filled) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.surfaceContainerHighest
            },
            modifier = Modifier.size(48.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = if (sending) AppIcons.Stop else AppIcons.ArrowUp,
                    contentDescription = if (sending) stopLabel else sendLabel,
                    tint = when {
                        sending -> MaterialTheme.colorScheme.onSurface
                        filled -> MaterialTheme.colorScheme.onPrimary
                        else -> MaterialTheme.colorScheme.outline
                    },
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        // Drawn over the circle rather than beside it, so the control keeps its 48dp and the arc
        // rides its own perimeter. It takes no pointer events, so the stop underneath still works.
        if (sending) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.outlineVariant,
            )
        }
    }
}

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
