package ph.mart.healthapp.feature.home.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.data.health.StepDay
import ph.mart.healthapp.core.data.health.formatSteps
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.home.R

/**
 * Today's steps, from Google Health. Same rule as [SleepCard]: one source, so the caller hides the
 * card entirely rather than rendering a zero for a user who never connected.
 *
 * [goal] is the profile's current target — not snapshotted per day, see `Profile.stepGoal` — and it
 * is the bar's denominator, which is what gives the number the user set somewhere it is actually
 * read. There is no status mark: a step goal is a target, but a day is not over at the moment you
 * look at it, and marking a morning `error` for not having walked yet would be the app grading a
 * day in progress.
 *
 * [creditKcal] is what these steps added to the day's calorie budget, already net of any workout
 * that claimed them and already zero when the user has switched the exercise credit off. It rides
 * the bar's caption because the calorie ring's goal moves when it changes, and a number that moves
 * for no visible reason reads as a bug.
 */
@Composable
fun StepsCard(steps: StepDay, goal: Int, creditKcal: Int, wide: Boolean, modifier: Modifier = Modifier) {
    MetricCard(
        label = stringResource(R.string.home_steps_title),
        value = steps.formatSteps(),
        wide = wide,
        modifier = modifier,
    ) {
        MetaBar(
            progress = { if (goal > 0) steps.steps.toFloat() / goal else 0f },
            caption = if (creditKcal > 0) {
                stringResource(R.string.home_steps_goal_credit, formatSteps(goal), creditKcal)
            } else {
                stringResource(R.string.home_steps_of_goal, formatSteps(goal))
            },
        )
    }
}

@PreviewLightDark
@Composable
private fun StepsCardPreview() {
    val day = StepDay(dateEpochDay = 20_000, steps = 8432, burnedKcal = 302)
    AppTheme {
        Surface {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(16.dp)) {
                StepsCard(steps = day, goal = 10_000, creditKcal = 302, wide = true)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    StepsCard(day, goal = 10_000, creditKcal = 302, wide = false, modifier = Modifier.weight(1f))
                    // The same day with the exercise credit switched off.
                    StepsCard(day, goal = 10_000, creditKcal = 0, wide = false, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}
