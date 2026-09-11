package ph.mart.healthapp.feature.training.ui.training.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.data.exercise.ExerciseEntry
import ph.mart.healthapp.core.data.exercise.ExerciseType
import ph.mart.healthapp.core.data.exercise.totalBurnedKcal
import ph.mart.healthapp.core.data.profile.UnitSystem
import ph.mart.healthapp.core.designsystem.component.AppCard
import ph.mart.healthapp.core.designsystem.component.PrimaryButton
import ph.mart.healthapp.core.designsystem.component.SecondaryButton
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.core.designsystem.theme.tabularNums
import ph.mart.healthapp.feature.training.R

/**
 * What has been logged since midnight, and the two doors that log more. The doors sit here rather
 * than only on the FAB because this is the tab about doing: the FAB's quick-action sheet is a
 * shortcut from anywhere, and a tab that shows today's sessions should be able to add one without
 * a detour through it.
 *
 * The burn total is the card's headline figure and the diary's subtotal is the same number — both
 * read `totalBurnedKcal()`, so neither can drift.
 */
@Composable
internal fun TodaySessionsCard(
    entries: List<ExerciseEntry>,
    unit: UnitSystem,
    onLogActivity: () -> Unit,
    onLift: () -> Unit,
    onEditEntry: (ExerciseEntry) -> Unit,
    modifier: Modifier = Modifier,
) {
    AppCard(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = stringResource(R.string.training_today_title),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            if (entries.isNotEmpty()) {
                Text(
                    text = stringResource(
                        R.string.training_today_summary,
                        entries.totalBurnedKcal(),
                        entries.sumOf { it.minutes },
                    ),
                    style = MaterialTheme.typography.titleSmall.tabularNums,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }

        if (entries.isEmpty()) {
            Text(
                text = stringResource(R.string.training_today_none),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 8.dp),
            )
        } else {
            Column(modifier = Modifier.padding(top = 4.dp)) {
                entries.forEach { entry ->
                    key(entry.id) {
                        SessionRow(entry = entry, unit = unit, onClick = { onEditEntry(entry) })
                    }
                }
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        ) {
            PrimaryButton(
                label = stringResource(R.string.training_log_activity),
                onClick = onLogActivity,
                modifier = Modifier.weight(1f),
            )
            SecondaryButton(
                label = stringResource(R.string.training_lift),
                onClick = onLift,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun TodaySessionsCardPreview() {
    AppTheme {
        Surface {
            TodaySessionsCard(
                entries = listOf(
                    ExerciseEntry(id = 1, type = ExerciseType.Run, name = "Riverside loop", minutes = 30, burnedKcal = 310),
                    ExerciseEntry(id = 2, type = ExerciseType.Yoga, minutes = 25, burnedKcal = 110),
                ),
                unit = UnitSystem.Metric,
                onLogActivity = {},
                onLift = {},
                onEditEntry = {},
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

/** Nothing logged yet — the state the tab spends most of a morning in. */
@PreviewLightDark
@Composable
private fun TodaySessionsCardEmptyPreview() {
    AppTheme {
        Surface {
            TodaySessionsCard(
                entries = emptyList(),
                unit = UnitSystem.Metric,
                onLogActivity = {},
                onLift = {},
                onEditEntry = {},
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}
