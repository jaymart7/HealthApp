package ph.mart.healthapp.feature.food.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.data.exercise.ExerciseEntry
import ph.mart.healthapp.core.data.exercise.ExerciseType
import ph.mart.healthapp.core.data.exercise.totalBurnedKcal
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.core.designsystem.theme.tabularNums

/**
 * The diary's fifth section — same collapsible header and swipe-to-delete as the meal sections,
 * but its own row: an activity has a duration where a food has a portion, and no macros at all,
 * so `FoodItemRow` would be mostly empty columns.
 */
@Composable
internal fun ExerciseSection(
    entries: List<ExerciseEntry>,
    expanded: Boolean,
    onToggle: () -> Unit,
    onAdd: () -> Unit,
    onDeleteEntry: (Long) -> Unit,
) {
    Column {
        MealSectionHeader(
            label = "Exercise",
            subtotalKcal = entries.totalBurnedKcal(),
            expanded = expanded,
            onToggle = onToggle,
            onAdd = onAdd,
        )
        if (expanded) {
            if (entries.isEmpty()) {
                Text(
                    text = "Nothing logged for Exercise yet.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 38.dp, end = 16.dp, bottom = 12.dp),
                )
            }
            entries.forEach { entry ->
                key(entry.id) {
                    SwipeableExerciseRow(entry = entry, onDelete = { onDeleteEntry(entry.id) })
                }
            }
        }
    }
}

@Composable
private fun SwipeableExerciseRow(entry: ExerciseEntry, onDelete: () -> Unit) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) onDelete()
            true
        },
    )
    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            Row(
                modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.error),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Delete",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onError,
                    modifier = Modifier.padding(end = 24.dp),
                )
            }
        },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(start = 38.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.type.label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = listOfNotNull("${entry.minutes} min", entry.name.takeIf { it.isNotBlank() })
                        .joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = "${entry.burnedKcal} kcal",
                style = MaterialTheme.typography.bodyMedium.tabularNums,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun ExerciseSectionPreview() {
    AppTheme {
        Surface {
            ExerciseSection(
                entries = listOf(
                    ExerciseEntry(id = 1, type = ExerciseType.Run, name = "Riverside loop", minutes = 30, burnedKcal = 363),
                    ExerciseEntry(id = 2, type = ExerciseType.Yoga, minutes = 45, burnedKcal = 166),
                ),
                expanded = true,
                onToggle = {},
                onAdd = {},
                onDeleteEntry = {},
            )
        }
    }
}
