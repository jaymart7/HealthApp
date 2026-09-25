package ph.mart.healthapp.feature.food.ui.quicklog.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
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
 *
 * **Photo attaches rather than leaving.** It offers the system camera or the gallery, and the
 * picture lands above the field as a thumbnail with its own ✕, so the words typed next are *about*
 * it — "only half the rice". The full-screen camera flow stays on the diary's own Photo chip.
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
    onTakePhoto: () -> Unit,
    onPickPhoto: () -> Unit,
    onScanBarcode: () -> Unit,
    modifier: Modifier = Modifier,
    photo: ImageBitmap? = null,
    onRemovePhoto: () -> Unit = {},
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
        if (photo != null) AttachedPhoto(photo = photo, onRemove = onRemovePhoto)
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
                PhotoChip(onTakePhoto = onTakePhoto, onPickPhoto = onPickPhoto, modifier = Modifier.weight(1f))
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

/** The chip and its two-item menu — anchored to the chip, so the choice opens where the tap was. */
@Composable
private fun PhotoChip(onTakePhoto: () -> Unit, onPickPhoto: () -> Unit, modifier: Modifier = Modifier) {
    var open by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        LabelledActionChip(
            label = stringResource(R.string.food_chip_photo),
            icon = AppIcons.Camera,
            onClick = { open = true },
            modifier = Modifier.fillMaxWidth(),
        )
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.food_quick_take_photo)) },
                leadingIcon = { Icon(AppIcons.Camera, contentDescription = null) },
                onClick = {
                    open = false
                    onTakePhoto()
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.food_quick_pick_photo)) },
                leadingIcon = { Icon(AppIcons.Gallery, contentDescription = null) },
                onClick = {
                    open = false
                    onPickPhoto()
                },
            )
        }
    }
}

/** The plate, small, with the one thing to do to it. The ✕ is a full 48dp target of its own. */
@Composable
private fun AttachedPhoto(photo: ImageBitmap, onRemove: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Image(
            bitmap = photo,
            contentDescription = stringResource(R.string.food_quick_photo_attached),
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(12.dp)),
        )
        IconButton(onClick = onRemove) {
            Icon(
                imageVector = AppIcons.Close,
                contentDescription = stringResource(R.string.food_quick_remove_photo),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
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
                onTakePhoto = {},
                onPickPhoto = {},
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
                onTakePhoto = {},
                onPickPhoto = {},
                onScanBarcode = {},
                speechAvailable = true,
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

/** A plate attached: the thumbnail and its ✕ above the field, and the field asking for extras only. */
@PreviewLightDark
@Composable
private fun QuickLogInputBarPhotoPreview() {
    AppTheme {
        Surface {
            QuickLogInputBar(
                text = "",
                placeholder = stringResource(R.string.food_quick_photo_placeholder),
                thinking = false,
                canSend = true,
                showShortcuts = true,
                onTextChange = {},
                onSend = {},
                onCancel = {},
                onTakePhoto = {},
                onPickPhoto = {},
                onScanBarcode = {},
                photo = ImageBitmap(PREVIEW_PHOTO_PX, PREVIEW_PHOTO_PX),
                speechAvailable = true,
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

private const val PREVIEW_PHOTO_PX = 160
