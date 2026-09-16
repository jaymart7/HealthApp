package ph.mart.healthapp.feature.food.ui.voice.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import ph.mart.healthapp.core.data.food.MealType
import ph.mart.healthapp.core.designsystem.component.MascotAvatar
import ph.mart.healthapp.core.designsystem.component.MascotState
import ph.mart.healthapp.core.designsystem.component.SecondaryButton
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.food.R

/**
 * The two-to-six seconds between the sentence and the rows, spent showing the sentence.
 *
 * It was a bare centred spinner with one line under it. That is the wrong screen for this wait:
 * the user has just committed words they may have mis-dictated, and the one thing they can usefully
 * do while the call is in flight is read them back. Every second spent on a spinner over hidden
 * words is a second that could have caught "a slice of toast" coming back as "a slice of toast and
 * a black coffee" — and the parse does not have to finish for the fix to be free.
 *
 * So [onEdit] and [onCancel] do the same thing the back gesture already did: cancel the call and
 * return to the sentence with it and the slot intact. Cancel was previously reachable only by a
 * gesture, which is not an affordance.
 */
@Composable
internal fun VoiceParsingScreen(
    sentence: String,
    mealType: MealType,
    onEdit: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(color = MaterialTheme.colorScheme.surface, modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SentenceCard(
                sentence = sentence,
                mealType = mealType,
                caveat = stringResource(R.string.food_voice_parsing_caveat),
                onEdit = onEdit,
            )

            LinearProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(2.dp)),
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                MascotAvatar(state = MascotState.Thinking, size = 64.dp)
                Text(
                    text = stringResource(R.string.food_voice_parsing),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = stringResource(R.string.food_voice_parsing_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.widthIn(max = 280.dp),
                )
            }

            SecondaryButton(
                label = stringResource(R.string.food_voice_cancel),
                onClick = onCancel,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun VoiceParsingScreenPreview() {
    AppTheme {
        VoiceParsingScreen(
            sentence = "two scrambled eggs, a slice of toast and a black coffee",
            mealType = MealType.Breakfast,
            onEdit = {},
            onCancel = {},
        )
    }
}

/** The length the card has to stay readable at while the eye is proofreading rather than
 * reading — four lines of sentence, and the status block still on screen under it. */
@PreviewLightDark
@Composable
private fun VoiceParsingScreenLongPreview() {
    AppTheme {
        VoiceParsingScreen(
            sentence = "two scrambled eggs, a slice of wholemeal toast with butter and a black " +
                "coffee, a handful of blueberries and a small pot of greek yoghurt",
            mealType = MealType.Breakfast,
            onEdit = {},
            onCancel = {},
        )
    }
}
