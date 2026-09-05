package ph.mart.healthapp.feature.food.ui.voice.components

import android.content.Intent
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.data.food.MealType
import ph.mart.healthapp.core.designsystem.component.AppTextField
import ph.mart.healthapp.core.designsystem.component.PrimaryButton
import ph.mart.healthapp.core.designsystem.icon.AppIcons
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.food.R
import ph.mart.healthapp.feature.food.ui.shared.components.MealTypeChipRow

private val EXAMPLE = R.string.food_voice_example

/**
 * The sentence, the slot, and the button that turns one into rows.
 *
 * Speech is the system's own dialog ([RecognizerIntent.ACTION_RECOGNIZE_SPEECH]) rather than an
 * in-app [SpeechRecognizer]: it needs no `RECORD_AUDIO` permission, so there is no permission
 * screen to write and nothing to deny, and the transcript lands in a field that stays editable.
 * Typing is the same path — the mic only fills the field in.
 *
 * The mic is *hidden* where no recognizer is installed rather than shown and failing on tap, the
 * rule Home's supplements card follows: a control that can't answer shouldn't be there. That check
 * is what the manifest's `<queries>` entry exists for.
 */
@Composable
internal fun VoiceInputScreen(
    text: String,
    mealType: MealType,
    onTextChange: (String) -> Unit,
    onMealTypeSelect: (MealType) -> Unit,
    onEstimate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    // The same words the screen's own heading uses, so the dialog reads as part of it.
    val prompt = stringResource(R.string.food_voice_prompt)
    val speechAvailable = remember(context) { SpeechRecognizer.isRecognitionAvailable(context) }
    val speech = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        result.data
            ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            ?.firstOrNull()
            ?.let(onTextChange)
    }

    Surface(color = MaterialTheme.colorScheme.surface, modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(R.string.food_voice_prompt),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(EXAMPLE),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AppTextField(
                    value = text,
                    onValueChange = onTextChange,
                    placeholder = stringResource(R.string.food_voice_placeholder),
                    modifier = Modifier.weight(1f),
                )
                if (speechAvailable) {
                    IconButton(
                        onClick = { speech.launch(speechIntent(prompt)) },
                        modifier = Modifier.size(48.dp),
                    ) {
                        Icon(
                            imageVector = AppIcons.Mic,
                            contentDescription = stringResource(R.string.food_voice_speak),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            MealTypeChipRow(selected = mealType, onSelect = onMealTypeSelect)

            PrimaryButton(
                label = stringResource(R.string.food_voice_estimate),
                onClick = onEstimate,
                enabled = text.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** The prompt is the system dialog's, so it is passed in — this is not a composition. */
private fun speechIntent(prompt: String): Intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
    putExtra(RecognizerIntent.EXTRA_PROMPT, prompt)
}

@PreviewLightDark
@Composable
private fun VoiceInputScreenPreview() {
    AppTheme {
        VoiceInputScreen(
            text = "two scrambled eggs, a slice of toast and a black coffee",
            mealType = MealType.Breakfast,
            onTextChange = {},
            onMealTypeSelect = {},
            onEstimate = {},
        )
    }
}

/** Nothing typed yet — the button is off, and the example carries the whole instruction. */
@PreviewLightDark
@Composable
private fun VoiceInputScreenEmptyPreview() {
    AppTheme {
        VoiceInputScreen(
            text = "",
            mealType = MealType.Dinner,
            onTextChange = {},
            onMealTypeSelect = {},
            onEstimate = {},
        )
    }
}
