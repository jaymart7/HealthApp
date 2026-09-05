package ph.mart.healthapp.feature.coach.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.designsystem.component.MascotAvatar
import ph.mart.healthapp.core.designsystem.component.MascotSpeechBubble
import ph.mart.healthapp.core.designsystem.component.MascotState
import ph.mart.healthapp.core.designsystem.component.SecondaryButton
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.coach.R
import ph.mart.healthapp.feature.coach.ui.STARTERS

/**
 * A conversation nobody has started. The three starters are the point: they are questions today's
 * numbers can actually answer, so tapping one teaches what the coach knows without a paragraph
 * explaining it.
 */
@Composable
internal fun CoachEmptyState(onStarter: (String) -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        MascotAvatar(state = MascotState.Idle, size = 64.dp)
        MascotSpeechBubble(text = stringResource(R.string.coach_empty_greeting))
        Text(
            text = stringResource(R.string.coach_empty_limits),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        STARTERS.forEach { starter ->
            // Resolved here, and it is the resolved text that gets sent — the question the user
            // pressed is the question the coach is asked.
            val text = stringResource(starter)
            SecondaryButton(label = text, onClick = { onStarter(text) })
        }
    }
}

@PreviewLightDark
@Composable
private fun CoachEmptyStatePreview() {
    AppTheme {
        Surface {
            CoachEmptyState(onStarter = {}, modifier = Modifier.padding(16.dp))
        }
    }
}
