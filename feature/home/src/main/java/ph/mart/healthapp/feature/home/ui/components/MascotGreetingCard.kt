package ph.mart.healthapp.feature.home.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.designsystem.component.AIChip
import ph.mart.healthapp.core.designsystem.component.AIChipVariant
import ph.mart.healthapp.core.designsystem.component.AppCard
import ph.mart.healthapp.core.designsystem.component.MascotAvatar
import ph.mart.healthapp.core.designsystem.component.MascotSpeechBubble
import ph.mart.healthapp.core.designsystem.component.MascotState
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.home.R

/**
 * Mascot + speech bubble, and the app's one door to the coach.
 *
 * This card rather than the insight card, which would be the more contextual tap: the insight is
 * hidden on day one, hidden when the model has nothing to say, and gone once dismissed, so a door
 * on it is a door that isn't there most days. The greeting is always on a populated Home. The
 * [AIChip] is what makes the tap visible — a card that merely happens to be clickable reads as
 * decoration.
 *
 * The greeting text is passed in rather than read here so the card stays a pure function of its
 * input and the time-of-day rule stays testable in `HomeData.kt`.
 */
@Composable
fun MascotGreetingCard(greeting: String, onOpenCoach: () -> Unit, modifier: Modifier = Modifier) {
    AppCard(modifier = modifier, onClick = onOpenCoach) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MascotAvatar(state = MascotState.Idle, size = 64.dp)
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f),
            ) {
                MascotSpeechBubble(text = greeting)
                AIChip(label = stringResource(R.string.home_ask_coach), variant = AIChipVariant.Default)
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun MascotGreetingCardPreview() {
    AppTheme {
        Surface {
            MascotGreetingCard(
                greeting = "Good morning! Ready for breakfast?",
                onOpenCoach = {},
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}
