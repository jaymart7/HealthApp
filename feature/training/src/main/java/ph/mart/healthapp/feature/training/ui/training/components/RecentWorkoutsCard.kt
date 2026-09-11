package ph.mart.healthapp.feature.training.ui.training.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.data.exercise.ExerciseEntry
import ph.mart.healthapp.core.data.exercise.ExerciseType
import ph.mart.healthapp.core.data.exercise.StrengthSet
import ph.mart.healthapp.core.data.profile.UnitSystem
import ph.mart.healthapp.core.designsystem.component.AppCard
import ph.mart.healthapp.core.designsystem.component.formatEpochDay
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.training.R

/**
 * The last few sessions behind today, newest first — see `recentSessions()`, which is where today's
 * own rows are dropped so the two cards never show the same workout twice.
 *
 * Read-only on purpose, and there is no "see all": this tab is today and doing, and the whole
 * history — with its charts, its volume and its personal records — is the Progress tab's Training
 * group. A row here is a reminder of what was done, not a second way into it.
 */
@Composable
internal fun RecentWorkoutsCard(
    entries: List<ExerciseEntry>,
    unit: UnitSystem,
    modifier: Modifier = Modifier,
) {
    AppCard(modifier = modifier) {
        Text(
            text = stringResource(R.string.training_recent_title),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
        )
        Column(modifier = Modifier.padding(top = 4.dp)) {
            entries.forEach { entry ->
                key(entry.id) {
                    // The full date, not a weekday: five rows can reach back past a week, and
                    // "Mon" then names two different Mondays in one card.
                    SessionRow(entry = entry, unit = unit, leading = formatEpochDay(entry.dateEpochDay))
                }
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun RecentWorkoutsCardPreview() {
    AppTheme {
        Surface {
            RecentWorkoutsCard(
                entries = listOf(
                    ExerciseEntry(
                        id = 3,
                        dateEpochDay = 20_600,
                        type = ExerciseType.Strength,
                        name = "Push day",
                        minutes = 55,
                        burnedKcal = 280,
                        sets = listOf(
                            StrengthSet("Bench press", 8, 60.0),
                            StrengthSet("Bench press", 8, 60.0),
                            StrengthSet("Overhead press", 8, 35.0),
                        ),
                    ),
                    ExerciseEntry(id = 4, dateEpochDay = 20_599, type = ExerciseType.Run, minutes = 42, burnedKcal = 430),
                ),
                unit = UnitSystem.Metric,
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}
