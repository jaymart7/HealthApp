package ph.mart.healthapp.feature.coach.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.designsystem.component.AppTextField
import ph.mart.healthapp.core.designsystem.component.rememberSpeechAvailable
import ph.mart.healthapp.core.designsystem.component.speechIntent
import ph.mart.healthapp.core.designsystem.component.spokenPhrase
import ph.mart.healthapp.core.designsystem.icon.AppIcons
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.coach.R

/**
 * The question field, the mic, and one circle that sends or stops.
 *
 * **The tray is its own surface now** — `surfaceContainerLow` under a 1dp `outlineVariant` rule —
 * rather than the same `surface` as the conversation above it. The rule is what says the field is
 * pinned and the list scrolls under it; without one the composer floated.
 *
 * **The mic moved inside the field** and the circle stayed outside, which is the whole of the new
 * geometry: one control belongs to the *text* and one belongs to the *turn*, and sitting them side
 * by side as two grey glyphs said neither. Where a device has no recognizer the mic is **absent,
 * not disabled** — Home's supplements-card rule — but [AppTextField]'s trailing slot is still
 * passed, so it still reserves its 48dp and the field and the circle keep their exact geometry.
 *
 * **The circle never moves or resizes.** Only its fill and its glyph change: quiet with an `outline`
 * arrow on an empty field, `primary` with an `onPrimary` arrow once there is something to send, and
 * quiet again with a stop glyph and a ring around it while a turn runs. An answer can take seconds
 * and a model can hang; a progress indicator that cannot be pressed leaves leaving the screen as the
 * only way out, and that costs the user the question they typed.
 *
 * Speech is the system's own dialog ([speechIntent]), the same call `VoiceInputScreen` makes for
 * talk-to-log: no `RECORD_AUDIO`, so no permission screen and nothing to deny. The transcript
 * **fills the field and stops there** — it never sends, because a misheard question would be
 * spent before it could be read, and typing is the same path either way.
 *
 * The keyboard's own key sends too — `ImeAction.Send`, wired to the same lambda the circle calls
 * and gated by the same [canSend], so the two can never disagree about whether a send is available.
 *
 * [source] is the door the question arrived through, and non-null is what draws the context chip.
 *
 * [speechAvailable] is hoisted only so the previews can draw the mic: the preview renderer has no
 * recognizer installed, and a component preview that can't show its own control is worth one
 * default argument.
 */
@Composable
internal fun ChatInputBar(
    draft: String,
    sending: Boolean,
    onDraftChange: (String) -> Unit,
    onSend: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier,
    source: String? = null,
    onDismissSource: () -> Unit = {},
    speechAvailable: Boolean = rememberSpeechAvailable(),
) {
    // The field's own placeholder is the dialog's prompt, so the two read as one screen rather
    // than as two ways of asking the same thing.
    val prompt = stringResource(R.string.coach_input_placeholder)
    val speech = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        spokenPhrase(result.data)?.let { onDraftChange(withSpoken(draft, it)) }
    }

    Surface(color = MaterialTheme.colorScheme.surfaceContainerLow, modifier = modifier.fillMaxWidth()) {
        Column {
            HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (source != null) ContextChip(source = source, onDismiss = onDismissSource)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AppTextField(
                        value = draft,
                        onValueChange = onDraftChange,
                        placeholder = prompt,
                        modifier = Modifier.weight(1f),
                        imeAction = ImeAction.Send,
                        onImeAction = onSend.takeIf { canSend(draft, sending) },
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                        border = null,
                        // Passed even with no recognizer to draw: the slot reserves its 48dp either
                        // way, so a device without one keeps the field's exact width rather than
                        // letting the text run to where a glyph would have been.
                        trailing = {
                            if (speechAvailable) {
                                IconButton(onClick = { speech.launch(speechIntent(prompt)) }) {
                                    Icon(
                                        imageVector = AppIcons.Mic,
                                        contentDescription = stringResource(R.string.coach_input_speak),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                            }
                        },
                    )
                    ActionCircle(
                        sending = sending,
                        canSend = canSend(draft, sending),
                        onSend = onSend,
                        onStop = onStop,
                    )
                }
            }
        }
    }
}

/**
 * One 48dp circle in three states, and it is one composable rather than three so it cannot move
 * between them. The fill carries whether a send is available and the glyph carries what the tap
 * does; the spinner is the ring *around* the control rather than the control itself, which is what
 * lets a running turn still be stopped.
 */
@Composable
private fun ActionCircle(sending: Boolean, canSend: Boolean, onSend: () -> Unit, onStop: () -> Unit) {
    val filled = !sending && canSend
    Box(contentAlignment = Alignment.Center) {
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
                    contentDescription = stringResource(
                        if (sending) R.string.coach_input_stop else R.string.coach_input_send,
                    ),
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

/**
 * What the field is pre-filled *about*, when the user arrived through a door elsewhere in the app.
 *
 * The whole chip is the dismiss target rather than the 14dp glyph inside it — it has one action, so
 * a second tappable region inside a 28dp pill would be two targets where there is one intent. The
 * visual stays the chip's own height and `minimumInteractiveComponentSize()` expands the touch box
 * to 48dp, which is the 48dp-touch / 40dp-visual split the profile stepper already ships.
 *
 * Dismissing it **leaves the text**. The label says where the question came from; the question is
 * the user's, and a chip that took it with it would be the mistap the prefill rule exists to make
 * free.
 */
@Composable
private fun ContextChip(source: String, onDismiss: () -> Unit) {
    // A semantics lambda cannot read a resource, so it is resolved a line above the row it labels.
    val dismissLabel = stringResource(R.string.coach_input_context_dismiss, source)
    Surface(
        onClick = onDismiss,
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.minimumInteractiveComponentSize(),
    ) {
        Row(
            modifier = Modifier.padding(start = 10.dp, end = 8.dp, top = 5.dp, bottom = 5.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.coach_input_context, source).uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Icon(
                imageVector = AppIcons.Close,
                contentDescription = dismissLabel,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}

/**
 * Whether a send is available at all — the circle's `enabled` and the keyboard's action key read
 * the same function, so the return key can never start a turn the circle refuses to. [sending] is
 * redundant for the glyph, which is a stop while a turn runs, and load-bearing for the keyboard,
 * which is still up.
 */
internal fun canSend(draft: String, sending: Boolean): Boolean = draft.isNotBlank() && !sending

/**
 * What the field holds after a phrase comes back. It **appends** rather than replaces: a half-typed
 * question is the user's, the same reading `CoachFailure` gives one that failed to send, and a mic
 * that ate it would be a worse mistake than one that needs a space deleted.
 */
internal fun withSpoken(draft: String, spoken: String): String =
    if (draft.isBlank()) spoken else "${draft.trimEnd()} $spoken"

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
                onStop = {},
                speechAvailable = true,
            )
        }
    }
}

/** The quiet circle: nothing typed, so nothing to send. */
@PreviewLightDark
@Composable
private fun ChatInputBarEmptyPreview() {
    AppTheme {
        Surface {
            ChatInputBar(
                draft = "",
                sending = false,
                onDraftChange = {},
                onSend = {},
                onStop = {},
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
                onStop = {},
                speechAvailable = true,
            )
        }
    }
}

/** Arrived from the diary: the question is in the field unsent, and the chip names the door. */
@PreviewLightDark
@Composable
private fun ChatInputBarContextPreview() {
    AppTheme {
        Surface {
            ChatInputBar(
                draft = "How did I do on Tue 9 Sep?",
                sending = false,
                onDraftChange = {},
                onSend = {},
                onStop = {},
                source = "Diary, Tue 9 Sep",
                speechAvailable = true,
            )
        }
    }
}

/** A device with no recognizer installed: the mic is gone and its 48dp slot is not. */
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
                onStop = {},
                speechAvailable = false,
            )
        }
    }
}
