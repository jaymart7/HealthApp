package ph.mart.healthapp.feature.home.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.designsystem.component.AppCard
import ph.mart.healthapp.core.designsystem.component.MascotAvatar
import ph.mart.healthapp.core.designsystem.component.MascotSpeechBubble
import ph.mart.healthapp.core.designsystem.component.MascotState
import ph.mart.healthapp.core.designsystem.theme.AppTheme

/** Mascot + speech bubble. The greeting text is passed in rather than read here so the card stays
 * a pure function of its input and the time-of-day rule stays testable in `HomeData.kt`. */
@Composable
fun MascotGreetingCard(greeting: String, modifier: Modifier = Modifier) {
    AppCard(modifier = modifier) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MascotAvatar(state = MascotState.Idle, size = 64.dp)
            MascotSpeechBubble(text = greeting, modifier = Modifier.weight(1f))
        }
    }
}

@PreviewLightDark
@Composable
private fun MascotGreetingCardPreview() {
    AppTheme {
        Surface {
            MascotGreetingCard(greeting = "Good morning! Ready for breakfast?", modifier = Modifier.padding(16.dp))
        }
    }
}
