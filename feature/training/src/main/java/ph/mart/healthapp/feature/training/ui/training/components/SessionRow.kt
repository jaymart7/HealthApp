package ph.mart.healthapp.feature.training.ui.training.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
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
import ph.mart.healthapp.core.data.exercise.ExerciseEntry
import ph.mart.healthapp.core.data.exercise.ExerciseType
import ph.mart.healthapp.core.data.exercise.StrengthSet
import ph.mart.healthapp.core.data.exercise.summaryLabel
import ph.mart.healthapp.core.data.profile.UnitSystem
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.core.designsystem.theme.tabularNums
import ph.mart.healthapp.feature.training.R

/** 44dp, the app's tap-target floor — a row this one opens a sheet from has to clear it. */
private val RowMinHeight = 44.dp

/**
 * One logged session, drawn by both of this tab's cards. [leading] is the day, and only the Recent
 * card passes one: every row in the Today card is today, so a date column there would say the same
 * word four times.
 *
 * [onClick] is null for a row that opens nothing — Recent's rows are a glance, not a form. Editing
 * a past session stays where it already is, on the day's own diary.
 */
@Composable
internal fun SessionRow(
    entry: ExerciseEntry,
    unit: UnitSystem,
    modifier: Modifier = Modifier,
    leading: String? = null,
    onClick: (() -> Unit)? = null,
) {
    val title = entry.name.ifBlank { stringResource(entry.type.label) }
    // Resolved before the join: `joinToString` is not inline, so a `stringResource` inside its
    // lambda would not compile.
    val minutes = stringResource(R.string.training_row_minutes, entry.minutes)
    val sets = entry.sets.summaryLabel(unit).takeIf { entry.sets.isNotEmpty() }
    val editLabel = stringResource(R.string.training_edit_activity, title)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = RowMinHeight)
            .let { if (onClick == null) it else it.clickable(onClickLabel = editLabel, onClick = onClick) }
            .padding(vertical = 4.dp),
    ) {
        if (leading != null) {
            Text(
                text = leading,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = 12.dp),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = listOfNotNull(minutes, sets).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = stringResource(R.string.training_row_kcal, entry.burnedKcal),
            style = MaterialTheme.typography.titleMedium.tabularNums,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@PreviewLightDark
@Composable
private fun SessionRowPreview() {
    AppTheme {
        Surface {
            Column(modifier = Modifier.padding(16.dp)) {
                SessionRow(
                    entry = ExerciseEntry(
                        id = 1,
                        type = ExerciseType.Run,
                        name = "Riverside loop",
                        minutes = 30,
                        burnedKcal = 310,
                    ),
                    unit = UnitSystem.Metric,
                    onClick = {},
                )
                SessionRow(
                    entry = ExerciseEntry(
                        id = 2,
                        type = ExerciseType.Strength,
                        minutes = 55,
                        burnedKcal = 280,
                        sets = listOf(StrengthSet("Bench press", 8, 60.0), StrengthSet("Bench press", 8, 60.0)),
                    ),
                    unit = UnitSystem.Metric,
                    leading = "Sep 8, 2026",
                )
            }
        }
    }
}
