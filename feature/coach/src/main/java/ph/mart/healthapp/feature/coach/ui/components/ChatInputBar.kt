package ph.mart.healthapp.feature.coach.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.designsystem.component.AppTextField
import ph.mart.healthapp.core.designsystem.icon.AppIcons
import ph.mart.healthapp.core.designsystem.theme.AppTheme

/**
 * The question field and its send button. The button is disabled on a blank draft and while a
 * send is in flight — the ViewModel guards both anyway, but a live button that does nothing is
 * the worse half of that pair.
 */
@Composable
internal fun ChatInputBar(
    draft: String,
    sending: Boolean,
    onDraftChange: (String) -> Unit,
    onSend: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(color = MaterialTheme.colorScheme.surface, modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppTextField(
                value = draft,
                onValueChange = onDraftChange,
                placeholder = "Ask your coach",
                modifier = Modifier.weight(1f),
            )
            if (sending) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
            } else {
                IconButton(onClick = onSend, enabled = draft.isNotBlank()) {
                    Icon(
                        imageVector = AppIcons.Send,
                        contentDescription = "Send",
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun ChatInputBarPreview() {
    AppTheme {
        Surface {
            ChatInputBar(draft = "What should I eat tonight?", sending = false, onDraftChange = {}, onSend = {})
        }
    }
}

@PreviewLightDark
@Composable
private fun ChatInputBarSendingPreview() {
    AppTheme {
        Surface {
            ChatInputBar(draft = "", sending = true, onDraftChange = {}, onSend = {})
        }
    }
}
