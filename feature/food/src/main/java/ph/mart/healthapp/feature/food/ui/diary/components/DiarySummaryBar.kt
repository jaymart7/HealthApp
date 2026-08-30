package ph.mart.healthapp.feature.food.ui.diary.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.designsystem.component.MacroBar
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.core.designsystem.theme.tabularNums

/** Screen-specific, non-scrolling summary pinned above the meal list: consumed/goal, remaining
 * kcal, and the goal's macro split (reuses [MacroBar] goal-only, same as Profile's Goals card). */
@Composable
fun DiarySummaryBar(
    consumedKcal: Int,
    goalKcal: Int,
    proteinGoalG: Int,
    carbsGoalG: Int,
    fatGoalG: Int,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
            Text(
                text = "$consumedKcal / $goalKcal kcal",
                // Both figures update as the day fills; a proportional digit makes the left-hand
                // one twitch while the right-hand one sits still.
                style = MaterialTheme.typography.bodySmall.tabularNums,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                // "-212 left" is a grade, and a minus sign is not how anyone reads a day. The
                // colour stays onSurface either way: over budget is not an error.
                text = remainingLabel(consumedKcal = consumedKcal, goalKcal = goalKcal),
                style = MaterialTheme.typography.titleSmall.tabularNums,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        MacroBar(proteinG = proteinGoalG, carbsG = carbsGoalG, fatG = fatGoalG)
    }
}

internal fun remainingLabel(consumedKcal: Int, goalKcal: Int): String {
    val remaining = goalKcal - consumedKcal
    return if (remaining < 0) "${-remaining} over" else "$remaining left"
}

@PreviewLightDark
@Composable
private fun DiarySummaryBarPreview() {
    AppTheme {
        Surface {
            DiarySummaryBar(
                consumedKcal = 940,
                goalKcal = 1941,
                proteinGoalG = 146,
                carbsGoalG = 194,
                fatGoalG = 65,
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

/** Past the goal — the case the old "-212 left" wording read as a scolding. */
@PreviewLightDark
@Composable
private fun DiarySummaryBarOverPreview() {
    AppTheme {
        Surface {
            DiarySummaryBar(
                consumedKcal = 2153,
                goalKcal = 1941,
                proteinGoalG = 146,
                carbsGoalG = 194,
                fatGoalG = 65,
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}
