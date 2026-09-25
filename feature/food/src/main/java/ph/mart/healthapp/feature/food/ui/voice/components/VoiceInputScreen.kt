package ph.mart.healthapp.feature.food.ui.voice.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.data.food.MealType
import ph.mart.healthapp.core.designsystem.component.AppTextField
import ph.mart.healthapp.core.designsystem.component.DockedActionBar
import ph.mart.healthapp.core.designsystem.component.PrimaryButton
import ph.mart.healthapp.core.designsystem.component.rememberSpeechAvailable
import ph.mart.healthapp.core.designsystem.component.speechIntent
import ph.mart.healthapp.core.designsystem.component.spokenPhrase
import ph.mart.healthapp.core.designsystem.icon.AppIcons
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.food.R
import ph.mart.healthapp.feature.food.ui.shared.components.MealTypeChipRow

private val EXAMPLE = R.string.food_voice_example

/** How far the sentence field grows before it scrolls instead. Four lines is a plate with four
 * things on it, which is also what the parse is capped at being able to return. */
private const val SENTENCE_LINES = 4

/**
 * The sentence, the slot, and the button that turns one into rows.
 *
 * Speech is the system's own dialog ([speechIntent]) rather than an in-app `SpeechRecognizer`:
 * it needs no `RECORD_AUDIO` permission, so there is no permission screen to write and nothing
 * to deny, and the transcript lands in a field that stays editable. Typing is the same path —
 * the mic only fills the field in.
 *
 * **The mic is the screen, not a glyph on it.** It used to be a 48dp grey icon button in a
 * right-aligned row beside an identical grey Clear, which is the wrong weight for the fastest path
 * into the flow. [SpeakCard] is the first thing on the screen, and the field under it is where
 * typing and correcting happen. Clear went *into* the field as [AppTextField]'s trailing slot,
 * where it is unmistakably about the text rather than a sibling of the mic.
 *
 * **Speech replaces the field rather than appending to it.** The append rule and its join belonged
 * to a mic that was the only way to add a second phrase; with the card saying what a second tap
 * does, the transcript is simply the new sentence, and an unwanted replacement is one undo away in
 * a field the user is already looking at. That retires `withSpoken` and its test with it.
 *
 * Under the field, the sentences that have become meals before — tapping one fills the field and
 * stops there. It does not estimate: a parse is a network call, and half the value of a remembered
 * sentence is correcting it before one is spent.
 *
 * Estimate is docked ([DockedActionBar]) rather than being the last item in the column: with the
 * keyboard up, a wrapped sentence, a recents strip and the chip row above it, it was reliably below
 * the fold on a short phone.
 *
 * The card is *not composed at all* where no recognizer is installed, the rule Home's supplements
 * card follows: a control that can't answer shouldn't be there. That check is what the manifest's
 * `<queries>` entry exists for.
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
    // The same words the screen's own heading uses, so the dialog reads as part of it.
    val prompt = stringResource(R.string.food_voice_prompt)
    val speechAvailable = rememberSpeechAvailable()
    val speech = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        spokenPhrase(result.data)?.let(onTextChange)
    }

    Surface(color = MaterialTheme.colorScheme.surface, modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
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

                if (speechAvailable) {
                    SpeakCard(
                        hasText = text.isNotBlank(),
                        onClick = { speech.launch(speechIntent(prompt)) },
                    )
                }

                AppTextField(
                    value = text,
                    onValueChange = onTextChange,
                    placeholder = stringResource(R.string.food_voice_placeholder),
                    maxLines = SENTENCE_LINES,
                    modifier = Modifier.fillMaxWidth(),
                    trailing = if (text.isNotBlank()) {
                        {
                            // No confirm, `HistorySearchField`'s clear button's call — and the strip
                            // below holds the last three sentences, so a cleared one is rarely gone
                            // for good.
                            IconButton(onClick = { onTextChange("") }, modifier = Modifier.size(48.dp)) {
                                Icon(
                                    imageVector = AppIcons.Close,
                                    contentDescription = stringResource(R.string.food_voice_clear),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    } else {
                        null
                    },
                )

                if (text.isBlank() && recentSentences.isNotEmpty()) {
                    RecentSentences(sentences = recentSentences, onSelect = onTextChange)
                }

                MealTypeChipRow(selected = mealType, onSelect = onMealTypeSelect)
            }

            DockedActionBar {
                PrimaryButton(
                    label = stringResource(R.string.food_voice_estimate),
                    onClick = onEstimate,
                    enabled = text.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
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
 * [SENTENCE_LINES] and Clear stays pinned to the top of the box as it grows. */
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

/** Nothing typed yet and nothing logged before — the button is off, and the card carries the whole
 * instruction. */
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
