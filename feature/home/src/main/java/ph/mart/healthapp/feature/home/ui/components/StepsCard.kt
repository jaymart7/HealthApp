package ph.mart.healthapp.feature.home.ui.components

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.data.health.StepDay
import ph.mart.healthapp.core.data.health.formatSteps
import ph.mart.healthapp.core.designsystem.component.AppCard
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.core.designsystem.theme.tabularNums
import ph.mart.healthapp.feature.home.R

/**
 * Today's steps, from Google Health. Same rule as [SleepCard]: one source, so the caller hides the
 * card entirely rather than rendering a zero for a user who never connected.
 *
 * [goal] is the profile's current target, shown under the count so the number the user set has a
 * place it is actually read. It is not snapshotted per day — see `Profile.stepGoal`.
 *
 * [creditKcal] is what these steps added to the day's calorie budget, already net of any workout
 * that claimed them and already zero when the user has switched the exercise credit off. It is
 * shown because the calorie ring's goal moves when it changes, and a number that moves for no
 * visible reason reads as a bug.
 */
@Composable
fun StepsCard(steps: StepDay, goal: Int, creditKcal: Int, modifier: Modifier = Modifier) {
    AppCard(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(R.string.home_steps_title),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = steps.formatSteps(),
                    style = MaterialTheme.typography.headlineSmall.tabularNums,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(R.string.home_steps_of_goal, formatSteps(goal)),
                    style = MaterialTheme.typography.bodySmall.tabularNums,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (creditKcal > 0) {
                    Text(
                        text = stringResource(R.string.home_steps_credit, creditKcal),
                        style = MaterialTheme.typography.bodySmall.tabularNums,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Text(
                text = stringResource(R.string.home_from_google_health),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun StepsCardPreview() {
    AppTheme {
        Surface {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(16.dp)) {
                StepsCard(
                    steps = StepDay(dateEpochDay = 20_000, steps = 8432, burnedKcal = 302),
                    goal = 10_000,
                    creditKcal = 302,
                )
                // The same day with the exercise credit switched off.
                StepsCard(
                    steps = StepDay(dateEpochDay = 20_000, steps = 8432, burnedKcal = 302),
                    goal = 10_000,
                    creditKcal = 0,
                )
            }
        }
    }
}
