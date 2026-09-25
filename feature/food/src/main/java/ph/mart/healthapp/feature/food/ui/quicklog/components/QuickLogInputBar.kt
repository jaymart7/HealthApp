package ph.mart.healthapp.feature.food.ui.quicklog.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.designsystem.component.AppTextField
import ph.mart.healthapp.core.designsystem.component.SendStopButton
import ph.mart.healthapp.core.designsystem.component.rememberSpeechAvailable
import ph.mart.healthapp.core.designsystem.component.speechIntent
import ph.mart.healthapp.core.designsystem.component.spokenPhrase
import ph.mart.healthapp.core.designsystem.icon.AppIcons
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.core.designsystem.theme.Motion
import ph.mart.healthapp.feature.food.R
import ph.mart.healthapp.feature.food.ui.shared.components.LabelledActionChip

/** Two lines even when blank, so the field reads as a place for a sentence; four before it scrolls. */
private const val SENTENCE_MIN_LINES = 2
private const val SENTENCE_MAX_LINES = 4

/**
 * The field, the send circle, and under them the ways in that are not sentences.
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
 * **The words leave the field visibly.** Over the field's text sits the one end of the send flight
 * — an empty box keyed to the turn the field will become — and the user's bubble in the thread is
 * the other, so a send grows the bubble out of the field and a cancel or a dead end flies it back
 * ([quickLogShared]). [sentText] is the ghost of what was just sent, fading from the field while the
 * bubble arrives, because the state has already emptied it.
 *
 * Under the field: Photo · Scan until a conversation starts, since a plate and a barcode are not
 * sentences; the space is the conversation's after that. [onContinueInCoach], once a conversation
 * has started, is the door to the coach — a third chip beside Photo · Scan after a first send that
 * came to nothing, and a text button of its own when it would be the only chip (A6), because one
 * outlined box under the field reads as a second field. Nothing is under the field while a call
 * runs: there is nothing to do then but wait or stop it.
 *
 * **Photo attaches rather than leaving.** It offers the system camera or the gallery, and the
 * picture lands above the field as a thumbnail with its own ✕, so the words typed next are *about*
 * it — "only half the rice". The full-screen camera flow stays on the diary's own Photo chip.
 */
@Composable
internal fun QuickLogInputBar(
    text: String,
    placeholder: String?,
    thinking: Boolean,
    canSend: Boolean,
    showShortcuts: Boolean,
    turnCount: Int,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    onCancel: () -> Unit,
    onTakePhoto: () -> Unit,
    onPickPhoto: () -> Unit,
    onScanBarcode: () -> Unit,
    modifier: Modifier = Modifier,
    sentText: String? = null,
    photo: ImageBitmap? = null,
    onRemovePhoto: () -> Unit = {},
    onContinueInCoach: (() -> Unit)? = null,
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

    Column(modifier = modifier.fillMaxWidth()) {
        val shownPhoto = retainLast(photo)
        AnimatedVisibility(
            visible = photo != null,
            enter = expandVertically(tween(QuickLogMotion.Enter, easing = Motion.EmphasizedDecelerate)) +
                fadeIn(tween(QuickLogMotion.Swap, QuickLogMotion.Enter - QuickLogMotion.Swap)),
            exit = shrinkVertically(tween(QuickLogMotion.Exit, easing = Motion.EmphasizedAccelerate)) +
                fadeOut(tween(QuickLogMotion.Exit, easing = Motion.EmphasizedAccelerate)),
        ) {
            shownPhoto?.let { AttachedPhoto(photo = it, onRemove = onRemovePhoto, modifier = Modifier.padding(bottom = 12.dp)) }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            Box(modifier = Modifier.weight(1f)) {
                AppTextField(
                    value = text,
                    onValueChange = onTextChange,
                    placeholder = placeholder,
                    maxLines = SENTENCE_MAX_LINES,
                    minLines = SENTENCE_MIN_LINES,
                    imeAction = ImeAction.Send,
                    onImeAction = onSend.takeIf { canSend },
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    border = null,
                    modifier = Modifier.focusRequester(focusRequester),
                    // Passed even with no recognizer, so the slot keeps its 48dp and the field its width.
                    trailing = {
                        if (speechAvailable) {
                            IconButton(onClick = { speech.launch(speechIntent(prompt)) }) {
                                Icon(
                                    imageVector = AppIcons.Mic,
                                    contentDescription = stringResource(R.string.food_quick_speak),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    },
                )
                FlightSource(
                    // The turn this text becomes on send; mid-call, the one it just became.
                    index = if (thinking) turnCount - 1 else turnCount,
                    visible = !thinking && text.isNotBlank(),
                    modifier = Modifier.matchParentSize(),
                )
                if (thinking && sentText != null) {
                    key(turnCount) { SentGhost(text = sentText, modifier = Modifier.matchParentSize()) }
                }
            }
            SendStopButton(
                sending = thinking,
                canSend = canSend,
                onSend = onSend,
                onStop = onCancel,
                sendLabel = stringResource(R.string.food_quick_send),
                stopLabel = stringResource(R.string.food_quick_cancel),
            )
        }
        val chips = showShortcuts && !thinking
        val coach = onContinueInCoach.takeIf { !thinking }
        AnimatedVisibility(
            visible = chips,
            enter = expandVertically(tween(QuickLogMotion.Enter, easing = Motion.EmphasizedDecelerate)) +
                fadeIn(tween(QuickLogMotion.Swap, QuickLogMotion.Enter - QuickLogMotion.Swap)),
            exit = shrinkVertically(tween(QuickLogMotion.Exit, easing = Motion.EmphasizedAccelerate)) +
                fadeOut(tween(QuickLogMotion.Fade)),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 12.dp)) {
                PhotoChip(onTakePhoto = onTakePhoto, onPickPhoto = onPickPhoto, modifier = Modifier.weight(1f))
                LabelledActionChip(
                    label = stringResource(R.string.food_chip_scan),
                    icon = AppIcons.Barcode,
                    onClick = onScanBarcode,
                    modifier = Modifier.weight(1f),
                )
                if (coach != null) {
                    LabelledActionChip(
                        label = stringResource(R.string.food_quick_ask_coach),
                        icon = AppIcons.AiSparkle,
                        onClick = coach,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
        val shownCoach = retainLast(coach)
        AnimatedVisibility(
            visible = !chips && coach != null,
            enter = fadeIn(tween(QuickLogMotion.Swap, easing = Motion.Standard)) +
                expandVertically(tween(QuickLogMotion.Enter, easing = Motion.EmphasizedDecelerate)),
            exit = fadeOut(tween(QuickLogMotion.Fade)) +
                shrinkVertically(tween(QuickLogMotion.Exit, easing = Motion.EmphasizedAccelerate)),
        ) {
            AskCoachButton(onClick = { shownCoach?.invoke() }, modifier = Modifier.padding(top = 12.dp))
        }
    }
}

/**
 * The field's end of the send flight: nothing to see, only a shape keyed to the turn the field's
 * text is about to become. Keyed by [index] so that on send the *same* node leaves as the bubble
 * with that index arrives — the pairing a shared-bounds transition needs.
 */
@Composable
private fun FlightSource(index: Int, visible: Boolean, modifier: Modifier = Modifier) {
    key(index) {
        AnimatedVisibility(
            visible = visible,
            // Invisible either way; the durations only keep the node laid out while it flies, so
            // the flight starts from where the field is *now* — the sheet has just grown around it.
            enter = fadeIn(tween(QuickLogMotion.Travel)),
            exit = fadeOut(tween(QuickLogMotion.Travel)),
            modifier = modifier,
        ) {
            Box(modifier = Modifier.fillMaxSize().quickLogShared(turnKey(index), this))
        }
    }
}

/** What was just sent, left where it was typed and faded out while its bubble grows elsewhere. */
@Composable
private fun SentGhost(text: String, modifier: Modifier = Modifier) {
    val alpha = remember { Animatable(1f) }
    LaunchedEffect(Unit) { alpha.animateTo(0f, tween(QuickLogMotion.Fade, easing = Motion.Standard)) }
    Box(contentAlignment = Alignment.CenterStart, modifier = modifier.padding(start = 16.dp, end = 48.dp)) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.graphicsLayer { this.alpha = alpha.value },
        )
    }
}

/** Ask coach on its own: a text button, sparkle in `tertiary` and label in `primary`, pulled back
 * 12dp so the sparkle sits on the gutter's line rather than the button's padding. */
@Composable
private fun AskCoachButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    TextButton(
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 12.dp),
        modifier = modifier
            .offset(x = (-12).dp)
            .height(48.dp),
    ) {
        Icon(
            imageVector = AppIcons.AiSparkle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = stringResource(R.string.food_quick_ask_coach),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 8.dp),
        )
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

/**
 * The plate, small, with the one thing to do to it. The ✕ is a 24dp `inverseSurface` badge on the
 * thumbnail's corner, inside a full 48dp target — the box is sized to hold that whole target, since
 * a touch outside a node's bounds never reaches it.
 */
@Composable
private fun AttachedPhoto(photo: ImageBitmap, onRemove: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier = modifier.size(width = 80.dp, height = 72.dp)) {
        Image(
            bitmap = photo,
            contentDescription = stringResource(R.string.food_quick_photo_attached),
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .size(64.dp)
                .clip(RoundedCornerShape(12.dp)),
        )
        IconButton(onClick = onRemove, modifier = Modifier.align(Alignment.TopEnd)) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(24.dp)
                    .background(MaterialTheme.colorScheme.inverseSurface, CircleShape),
            ) {
                Icon(
                    imageVector = AppIcons.Close,
                    contentDescription = stringResource(R.string.food_quick_remove_photo),
                    tint = MaterialTheme.colorScheme.inverseOnSurface,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

/**
 * The one line that says why there is nothing to log, or what to trust — first in the bar, right
 * over the field the user's eyes are on after a send (A4), where it used to sit under the rows. A
 * polite live region, so TalkBack hears it without being interrupted. The glyph is `error` only for
 * a call that failed; everything else is information, not a fault.
 */
@Composable
internal fun QuickLogMessageLine(@StringRes message: Int, modifier: Modifier = Modifier) {
    val failed = message == R.string.food_quick_failed
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
        modifier = modifier
            .fillMaxWidth()
            .semantics { liveRegion = LiveRegionMode.Polite }
            .padding(vertical = 4.dp),
    ) {
        Icon(
            imageVector = messageIcon(message),
            contentDescription = null,
            tint = if (failed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
        Text(
            text = stringResource(message),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun messageIcon(@StringRes message: Int) = when (message) {
    R.string.food_quick_failed -> AppIcons.Error
    R.string.food_quick_offline, R.string.food_quick_offline_matched -> AppIcons.CloudOff
    R.string.food_quick_camera_denied -> AppIcons.NoPhotography
    else -> AppIcons.Info
}

@PreviewLightDark
@Composable
private fun QuickLogInputBarPreview() {
    AppTheme {
        Surface(color = MaterialTheme.colorScheme.surfaceContainerLow) {
            QuickLogInputBar(
                text = "",
                placeholder = stringResource(R.string.food_quick_placeholder),
                thinking = false,
                canSend = false,
                showShortcuts = true,
                turnCount = 0,
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

/** Mid-call: the circle is the stop, no placeholder, and nothing under the field. */
@PreviewLightDark
@Composable
private fun QuickLogInputBarThinkingPreview() {
    AppTheme {
        Surface(color = MaterialTheme.colorScheme.surfaceContainerLow) {
            QuickLogInputBar(
                text = "",
                placeholder = null,
                thinking = true,
                canSend = false,
                showShortcuts = false,
                turnCount = 1,
                onTextChange = {},
                onSend = {},
                onCancel = {},
                onTakePhoto = {},
                onPickPhoto = {},
                onScanBarcode = {},
                onContinueInCoach = {},
                speechAvailable = true,
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

/** Mid-conversation: Ask coach alone, so a text button rather than a lone chip. */
@PreviewLightDark
@Composable
private fun QuickLogInputBarCoachPreview() {
    AppTheme {
        Surface(color = MaterialTheme.colorScheme.surfaceContainerLow) {
            QuickLogInputBar(
                text = "",
                placeholder = stringResource(R.string.food_quick_answer_placeholder),
                thinking = false,
                canSend = false,
                showShortcuts = false,
                turnCount = 2,
                onTextChange = {},
                onSend = {},
                onCancel = {},
                onTakePhoto = {},
                onPickPhoto = {},
                onScanBarcode = {},
                onContinueInCoach = {},
                speechAvailable = true,
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

/** A first send that came to nothing: the words back, and the coach as a third chip. */
@PreviewLightDark
@Composable
private fun QuickLogInputBarDeadEndPreview() {
    AppTheme {
        Surface(color = MaterialTheme.colorScheme.surfaceContainerLow) {
            Column(modifier = Modifier.padding(16.dp)) {
                QuickLogMessageLine(message = R.string.food_quick_nothing, modifier = Modifier.padding(bottom = 12.dp))
                QuickLogInputBar(
                    text = "feeling pretty good today",
                    placeholder = stringResource(R.string.food_quick_placeholder),
                    thinking = false,
                    canSend = true,
                    showShortcuts = true,
                    turnCount = 0,
                    onTextChange = {},
                    onSend = {},
                    onCancel = {},
                    onTakePhoto = {},
                    onPickPhoto = {},
                    onScanBarcode = {},
                    onContinueInCoach = {},
                    speechAvailable = true,
                )
            }
        }
    }
}

/** A plate attached: the thumbnail and its badge above the field, and the field asking for extras. */
@PreviewLightDark
@Composable
private fun QuickLogInputBarPhotoPreview() {
    AppTheme {
        Surface(color = MaterialTheme.colorScheme.surfaceContainerLow) {
            QuickLogInputBar(
                text = "",
                placeholder = stringResource(R.string.food_quick_photo_placeholder),
                thinking = false,
                canSend = true,
                showShortcuts = true,
                turnCount = 0,
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

/** Each message, with its glyph — the failed one alone in the error role. */
@PreviewLightDark
@Composable
private fun QuickLogMessageLinePreview() {
    AppTheme {
        Surface(color = MaterialTheme.colorScheme.surfaceContainerLow) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(16.dp)) {
                listOf(
                    R.string.food_quick_nothing,
                    R.string.food_quick_failed,
                    R.string.food_quick_offline,
                    R.string.food_quick_offline_matched,
                    R.string.food_quick_camera_denied,
                    R.string.food_quick_removed,
                ).forEach { QuickLogMessageLine(message = it) }
            }
        }
    }
}

private const val PREVIEW_PHOTO_PX = 160
