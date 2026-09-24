package ph.mart.healthapp.feature.food.ui.quicklog.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.designsystem.component.AppTextField
import ph.mart.healthapp.core.designsystem.component.SendStopButton
import ph.mart.healthapp.core.designsystem.component.rememberSpeechAvailable
import ph.mart.healthapp.core.designsystem.component.speechIntent
import ph.mart.healthapp.core.designsystem.component.spokenPhrase
import ph.mart.healthapp.core.designsystem.icon.AppIcons
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.food.R
import ph.mart.healthapp.feature.food.ui.shared.components.LabelledActionChip

/** Three lines before it scrolls — `DescribeExerciseField`'s figure, for a sentence of the same
 * length. */
private const val SENTENCE_LINES = 3

/**
 * The field, the send circle, and — until a conversation starts — the two doors that are not
 * sentences: the camera and the barcode.
 *
 * **It takes focus the moment it appears**, so the keyboard is already up when the sheet lands:
 * the FAB is a tap the user made to log something, and a field they then have to tap again is a
 * second tap the sheet exists to save. `SearchViewBar`'s rule, skipped under `@Preview`.
 *
 * The composer is `ChatInputBar`'s: the mic inside the field (the system dialog, so no
 * `RECORD_AUDIO`), [SendStopButton] outside it, and the keyboard's own Send gated by the same
 * [canSend] the circle is. The circle turns into a stop while the model is working, which is the
 * one way out of a hung call that does not cost the sentence.
 *
 * The camera and barcode chips go once a conversation starts — at that point the user has chosen
 * to type, and the space is the review's. Back to a blank start brings them back.
 */
@Composable
internal fun QuickLogInputBar(
    text: String,
    placeholder: String,
    thinking: Boolean,
    canSend: Boolean,
    showShortcuts: Boolean,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    onCancel: () -> Unit,
    onCapturePhoto: () -> Unit,
    onScanBarcode: () -> Unit,
    modifier: Modifier = Modifier,
    speechAvailable: Boolean = rememberSpeechAvailable(),
) {
    val prompt = stringResource(R.string.food_quick_prompt)
    val speech = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        // Replaces rather than appends — `VoiceInputScreen`'s rule.
        spokenPhrase(result.data)?.let(onTextChange)
    }
    val focusRequester = remember { FocusRequester() }
    val inspection = LocalInspectionMode.current
    LaunchedEffect(Unit) { if (!inspection) focusRequester.requestFocus() }

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppTextField(
                value = text,
                onValueChange = onTextChange,
                placeholder = placeholder,
                maxLines = SENTENCE_LINES,
                imeAction = ImeAction.Send,
                onImeAction = onSend.takeIf { canSend },
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                border = null,
                modifier = Modifier.weight(1f).focusRequester(focusRequester),
                // Passed even with no recognizer, so the slot keeps its 48dp and the field its width.
                trailing = {
                    if (speechAvailable) {
                        IconButton(onClick = { speech.launch(speechIntent(prompt)) }) {
                            Icon(
                                imageVector = AppIcons.Mic,
                                contentDescription = stringResource(R.string.food_quick_speak),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                },
            )
            SendStopButton(
                sending = thinking,
                canSend = canSend,
                onSend = onSend,
                onStop = onCancel,
                sendLabel = stringResource(R.string.food_quick_send),
                stopLabel = stringResource(R.string.food_quick_cancel),
            )
        }
        if (showShortcuts) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LabelledActionChip(
                    label = stringResource(R.string.food_chip_photo),
                    icon = AppIcons.Camera,
                    onClick = onCapturePhoto,
                    modifier = Modifier.weight(1f),
                )
                LabelledActionChip(
                    label = stringResource(R.string.food_chip_scan),
                    icon = AppIcons.Barcode,
                    onClick = onScanBarcode,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun QuickLogInputBarPreview() {
    AppTheme {
        Surface {
            QuickLogInputBar(
                text = "",
                placeholder = stringResource(R.string.food_quick_placeholder),
                thinking = false,
                canSend = false,
                showShortcuts = true,
                onTextChange = {},
                onSend = {},
                onCancel = {},
                onCapturePhoto = {},
                onScanBarcode = {},
                speechAvailable = true,
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

/** Mid-call: the circle is the stop, and the shortcuts have made way for the conversation. */
@PreviewLightDark
@Composable
private fun QuickLogInputBarThinkingPreview() {
    AppTheme {
        Surface {
            QuickLogInputBar(
                text = "",
                placeholder = stringResource(R.string.food_quick_answer_placeholder),
                thinking = true,
                canSend = false,
                showShortcuts = false,
                onTextChange = {},
                onSend = {},
                onCancel = {},
                onCapturePhoto = {},
                onScanBarcode = {},
                speechAvailable = true,
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}
