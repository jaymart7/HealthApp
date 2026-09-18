package ph.mart.healthapp.feature.training.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.designsystem.component.AppTextField
import ph.mart.healthapp.core.designsystem.component.SecondaryButton
import ph.mart.healthapp.core.designsystem.component.TextButton
import ph.mart.healthapp.core.designsystem.component.rememberSpeechAvailable
import ph.mart.healthapp.core.designsystem.component.speechIntent
import ph.mart.healthapp.core.designsystem.component.spokenPhrase
import ph.mart.healthapp.core.designsystem.icon.AppIcons
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.training.R

/**
 * The fastest way into the form above it: a sentence, and the three fields it answers filled in.
 *
 * **A swap-in, not a second screen.** Talk-to-log is a route because a sentence there becomes up to
 * eight priced rows that each need reviewing; one activity needs three fields, and those three
 * fields are already on screen — so what a parse produces here is reviewed in the form itself
 * rather than on a page of its own. Collapsed it is one button; the panel replaces that button and
 * back closes it, which is the sub-level `LogExerciseSheet` wires a handler for.
 *
 * **It never prices anything.** The model answers type, note and duration; the burn is
 * `withEstimate`'s, off the user's own latest weigh-in, exactly as it is for a duration typed by
 * hand. That is `CoachAction.LogExercise`'s rule and this is the second path under it.
 *
 * [message] is the line under the field — "no workout in that one", "that didn't work", "you're
 * offline". Resolved by the caller rather than passed as words, because it crosses a ViewModel
 * boundary on two of those three.
 *
 * [speechAvailable] is hoisted only so the previews can draw the mic: the preview renderer has no
 * recognizer installed, and a component preview that can't show its own control is worth one
 * default argument — `ChatInputBar`'s reason, for the same glyph.
 */
@Composable
internal fun DescribeExerciseField(
    open: Boolean,
    text: String,
    parsing: Boolean,
    onOpen: () -> Unit,
    onClose: () -> Unit,
    onTextChange: (String) -> Unit,
    onEstimate: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
    message: String? = null,
    speechAvailable: Boolean = rememberSpeechAvailable(),
) {
    if (!open) {
        SecondaryButton(
            label = stringResource(R.string.training_exercise_describe),
            onClick = onOpen,
            icon = AppIcons.AiSparkle,
            modifier = modifier,
        )
        return
    }

    // The field's own placeholder is not the dialog's prompt here, the way it is on the coach's
    // composer: the placeholder is an example sentence and the prompt is the question. Saying
    // "45 minute run along the river" out loud to a dialog asking it back would be absurd.
    val prompt = stringResource(R.string.training_exercise_describe_prompt)
    val speech = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        // Replaces rather than appends — `VoiceInputScreen`'s rule, and for its reason: this is a
        // say-the-workout field, not a composer being added to phrase by phrase.
        spokenPhrase(result.data)?.let(onTextChange)
    }
    val speakLabel = stringResource(R.string.training_exercise_describe_speak)

    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = modifier) {
        AppTextField(
            label = prompt,
            value = text,
            onValueChange = onTextChange,
            placeholder = stringResource(R.string.training_exercise_describe_placeholder),
            // A sentence wraps rather than scrolling sideways — the one parameter talk-to-log's
            // field already asks for, and this field holds the same kind of content.
            maxLines = 3,
            // Passed even with no recognizer to draw: the slot reserves its 48dp either way, so a
            // device without one keeps the field's exact width.
            trailing = {
                if (speechAvailable) {
                    IconButton(onClick = { speech.launch(speechIntent(prompt)) }) {
                        Icon(
                            imageVector = AppIcons.Mic,
                            contentDescription = speakLabel,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            },
        )
        if (message != null) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (parsing) {
            // A cancel rather than a bare spinner: a model can hang, and an indicator that cannot
            // be pressed would leave dismissing the whole sheet as the only way out — which costs
            // the user the sentence they typed. `ChatInputBar`'s stop, in a row's worth of space.
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                SecondaryButton(
                    label = stringResource(R.string.training_exercise_describe_cancel),
                    onClick = onCancel,
                )
            }
        } else {
            SecondaryButton(
                label = stringResource(R.string.training_exercise_describe_estimate),
                onClick = onEstimate,
                enabled = text.isNotBlank(),
                icon = AppIcons.AiSparkle,
            )
            TextButton(label = stringResource(R.string.training_exercise_describe_close), onClick = onClose)
        }
    }
}

/** Collapsed: the whole control is one button above a form that works without it. */
@PreviewLightDark
@Composable
private fun DescribeExerciseFieldClosedPreview() {
    AppTheme {
        Surface {
            DescribeExerciseField(
                open = false,
                text = "",
                parsing = false,
                onOpen = {},
                onClose = {},
                onTextChange = {},
                onEstimate = {},
                onCancel = {},
                speechAvailable = true,
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun DescribeExerciseFieldOpenPreview() {
    AppTheme {
        Surface {
            DescribeExerciseField(
                open = true,
                text = "45 minute run along the river",
                parsing = false,
                onOpen = {},
                onClose = {},
                onTextChange = {},
                onEstimate = {},
                onCancel = {},
                speechAvailable = true,
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

/** In flight: the cancel is the control, and the spinner is only the reason it is there. */
@PreviewLightDark
@Composable
private fun DescribeExerciseFieldParsingPreview() {
    AppTheme {
        Surface {
            DescribeExerciseField(
                open = true,
                text = "45 minute run along the river",
                parsing = true,
                onOpen = {},
                onClose = {},
                onTextChange = {},
                onEstimate = {},
                onCancel = {},
                speechAvailable = true,
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

/** The sentence named nothing physical — a real answer, and the sentence stays to be corrected. */
@PreviewLightDark
@Composable
private fun DescribeExerciseFieldMessagePreview() {
    AppTheme {
        Surface {
            DescribeExerciseField(
                open = true,
                text = "two eggs and toast",
                parsing = false,
                onOpen = {},
                onClose = {},
                onTextChange = {},
                onEstimate = {},
                onCancel = {},
                message = stringResource(R.string.training_exercise_describe_none),
                speechAvailable = true,
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}
