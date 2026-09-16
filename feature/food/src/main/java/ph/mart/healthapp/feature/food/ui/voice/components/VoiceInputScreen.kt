package ph.mart.healthapp.feature.food.ui.voice.components

import android.content.Intent
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
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
import ph.mart.healthapp.feature.food.ui.voice.withSpoken

private val EXAMPLE = R.string.food_voice_example

/** How far the sentence field grows before it scrolls instead. Four lines is a plate with four
 * things on it, which is also what the parse is capped at being able to return. */
private const val SENTENCE_LINES = 4

/**
 * The sentence, the slot, and the button that turns one into rows.
 *
 * Speech is the system's own dialog ([RecognizerIntent.ACTION_RECOGNIZE_SPEECH]) rather than an
 * in-app [SpeechRecognizer]: it needs no `RECORD_AUDIO` permission, so there is no permission
 * screen to write and nothing to deny, and the transcript lands in a field that stays editable.
 * Typing is the same path — the mic only fills the field in.
 *
 * The field takes the whole width and wraps to [SENTENCE_LINES], because on this one screen the
 * content is a sentence rather than a value and a 48dp box scrolls it out of sight. The mic and
 * Clear sit in a row beneath it rather than beside it for the same reason — they were costing the
 * sentence 112dp of the width it is read in. Clear is drawn only over text, the mic only where
 * there is a recognizer, and the row is right-aligned so neither moves when the other appears.
 *
 * Under the field, the sentences that have become meals before — tapping one fills the field and
 * stops there. It does not estimate: a parse is a network call, and half the value of a remembered
 * sentence is correcting it before one is spent.
 *
 * The mic is *hidden* where no recognizer is installed rather than shown and failing on tap, the
 * rule Home's supplements card follows: a control that can't answer shouldn't be there. That check
 * is what the manifest's `<queries>` entry exists for.
 */
@Composable
internal fun VoiceInputScreen(
    text: String,
    mealType: MealType,
    recentSentences: List<String>,
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
            ?.let { onTextChange(withSpoken(text, it)) }
    }

    Surface(color = MaterialTheme.colorScheme.surface, modifier = modifier.fillMaxSize()) {
        Column(
            // The estimate button sits under the field, which is where the keyboard lands — and it
            // scrolls to stay reachable, since the field, the recent strip and the keyboard can
            // between them be taller than a short phone. `VoiceReviewScreen` scrolls for the same
            // reason.
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(16.dp),
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

            AppTextField(
                value = text,
                onValueChange = onTextChange,
                placeholder = stringResource(R.string.food_voice_placeholder),
                maxLines = SENTENCE_LINES,
                modifier = Modifier.fillMaxWidth(),
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (text.isNotBlank()) {
                    // No confirm, `HistorySearchField`'s clear button's call — and the strip below
                    // holds the last three sentences, so a cleared one is rarely gone for good.
                    IconButton(
                        onClick = { onTextChange("") },
                        modifier = Modifier.size(48.dp),
                    ) {
                        Icon(
                            imageVector = AppIcons.Close,
                            contentDescription = stringResource(R.string.food_voice_clear),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
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

            if (text.isBlank() && recentSentences.isNotEmpty()) {
                VoiceRecentSentences(sentences = recentSentences, onSelect = onTextChange)
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
            recentSentences = emptyList(),
            onTextChange = {},
            onMealTypeSelect = {},
            onEstimate = {},
        )
    }
}

/** Two dictations' worth, which is what the field has to be able to show: it wraps to
 * [SENTENCE_LINES] and Clear appears beside the mic. */
@PreviewLightDark
@Composable
private fun VoiceInputScreenLongSentencePreview() {
    AppTheme {
        VoiceInputScreen(
            text = "two scrambled eggs, a slice of wholemeal toast with butter and a black coffee, " +
                "a handful of blueberries and a small pot of greek yoghurt",
            mealType = MealType.Breakfast,
            recentSentences = emptyList(),
            onTextChange = {},
            onMealTypeSelect = {},
            onEstimate = {},
        )
    }
}

/** Nothing typed yet and nothing logged before — the button is off, and the example carries the
 * whole instruction. */
@PreviewLightDark
@Composable
private fun VoiceInputScreenEmptyPreview() {
    AppTheme {
        VoiceInputScreen(
            text = "",
            mealType = MealType.Dinner,
            recentSentences = emptyList(),
            onTextChange = {},
            onMealTypeSelect = {},
            onEstimate = {},
        )
    }
}

/** An empty field with meals behind it: the strip is the whole difference from the preview above,
 * and it is gone the moment a key is pressed. */
@PreviewLightDark
@Composable
private fun VoiceInputScreenRecentPreview() {
    AppTheme {
        VoiceInputScreen(
            text = "",
            mealType = MealType.Breakfast,
            recentSentences = listOf(
                "two scrambled eggs, a slice of toast and a black coffee",
                "chicken breast, rice and steamed broccoli",
                "a banana",
            ),
            onTextChange = {},
            onMealTypeSelect = {},
            onEstimate = {},
        )
    }
}
