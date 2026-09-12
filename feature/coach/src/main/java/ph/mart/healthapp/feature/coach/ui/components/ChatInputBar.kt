package ph.mart.healthapp.feature.coach.ui.components

import android.content.Intent
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.designsystem.component.AppTextField
import ph.mart.healthapp.core.designsystem.icon.AppIcons
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.coach.R

/**
 * The question field, the mic, and the send button. The button is disabled on a blank draft and
 * while a send is in flight — the ViewModel guards both anyway, but a live button that does
 * nothing is the worse half of that pair.
 *
 * Speech is the system's own dialog ([RecognizerIntent.ACTION_RECOGNIZE_SPEECH]), the same call
 * `VoiceInputScreen` makes for talk-to-log: no `RECORD_AUDIO`, so no permission screen and nothing
 * to deny. The transcript **fills the field and stops there** — it never sends, because a misheard
 * question would be spent before it could be read, and typing is the same path either way.
 *
 * [speechAvailable] is hoisted only so the previews can draw the mic: the preview renderer has no
 * recognizer installed, and a component preview that can't show its own control is worth one
 * default argument. Where a device genuinely has none the mic is *absent* rather than disabled —
 * Home's supplements-card rule: a control that can't answer shouldn't be there.
 */
@Composable
internal fun ChatInputBar(
    draft: String,
    sending: Boolean,
    onDraftChange: (String) -> Unit,
    onSend: () -> Unit,
    modifier: Modifier = Modifier,
    speechAvailable: Boolean = rememberSpeechAvailable(),
) {
    // The field's own placeholder is the dialog's prompt, so the two read as one screen rather
    // than as two ways of asking the same thing.
    val prompt = stringResource(R.string.coach_input_placeholder)
    val speech = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        result.data
            ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            ?.firstOrNull()
            ?.let { onDraftChange(withSpoken(draft, it)) }
    }

    Surface(color = MaterialTheme.colorScheme.surface, modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppTextField(
                value = draft,
                onValueChange = onDraftChange,
                placeholder = prompt,
                modifier = Modifier.weight(1f),
            )
            if (speechAvailable) {
                IconButton(onClick = { speech.launch(speechIntent(prompt)) }) {
                    Icon(
                        imageVector = AppIcons.Mic,
                        contentDescription = stringResource(R.string.coach_input_speak),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (sending) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
            } else {
                IconButton(onClick = onSend, enabled = draft.isNotBlank()) {
                    Icon(
                        imageVector = AppIcons.Send,
                        contentDescription = stringResource(R.string.coach_input_send),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

@Composable
private fun rememberSpeechAvailable(): Boolean {
    val context = LocalContext.current
    return remember(context) { SpeechRecognizer.isRecognitionAvailable(context) }
}

/**
 * What the field holds after a phrase comes back. It **appends** rather than replaces: a half-typed
 * question is the user's, the same reading `CoachFailure` gives one that failed to send, and a mic
 * that ate it would be a worse mistake than one that needs a space deleted.
 */
internal fun withSpoken(draft: String, spoken: String): String =
    if (draft.isBlank()) spoken else "${draft.trimEnd()} $spoken"

/** The prompt is the system dialog's, so it is passed in — this is not a composition. */
private fun speechIntent(prompt: String): Intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
    putExtra(RecognizerIntent.EXTRA_PROMPT, prompt)
}

@PreviewLightDark
@Composable
private fun ChatInputBarPreview() {
    AppTheme {
        Surface {
            ChatInputBar(
                draft = "What should I eat tonight?",
                sending = false,
                onDraftChange = {},
                onSend = {},
                speechAvailable = true,
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun ChatInputBarSendingPreview() {
    AppTheme {
        Surface {
            ChatInputBar(
                draft = "",
                sending = true,
                onDraftChange = {},
                onSend = {},
                speechAvailable = true,
            )
        }
    }
}

/** A device with no recognizer installed: the field and send, exactly as the bar shipped. */
@PreviewLightDark
@Composable
private fun ChatInputBarNoSpeechPreview() {
    AppTheme {
        Surface {
            ChatInputBar(
                draft = "",
                sending = false,
                onDraftChange = {},
                onSend = {},
                speechAvailable = false,
            )
        }
    }
}
