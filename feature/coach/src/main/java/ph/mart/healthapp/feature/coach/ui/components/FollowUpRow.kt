package ph.mart.healthapp.feature.coach.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.designsystem.component.SecondaryButton
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.coach.R

/**
 * What to ask next, under the newest answer. The empty state's starters one turn later, and the
 * same outlined pill — a follow-up is an opener that arrived late, not a new kind of control.
 *
 * `FlowRow`, not `Row`: these are whole questions, and at a large font scale two of them are wider
 * than the screen. It sits inside the conversation rather than above the input bar, so it scrolls
 * away like everything else the coach said and never competes with the field for the thumb.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun FollowUpRow(
    followUps: List<Int>,
    onAsk: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        followUps.forEach { followUp ->
            // Resolved here, and the resolved text is what gets sent — the question the user
            // pressed is the question the coach is asked, the rule `CoachEmptyState` set.
            val text = stringResource(followUp)
            SecondaryButton(label = text, onClick = { onAsk(text) })
        }
    }
}

@PreviewLightDark
@Composable
private fun FollowUpRowPreview() {
    AppTheme {
        Surface {
            FollowUpRow(
                followUps = listOf(
                    R.string.coach_followup_protein,
                    R.string.coach_followup_water,
                    R.string.coach_starter_week,
                ),
                onAsk = {},
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}
