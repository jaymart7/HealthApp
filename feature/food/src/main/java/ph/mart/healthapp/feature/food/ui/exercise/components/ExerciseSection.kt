package ph.mart.healthapp.feature.food.ui.exercise.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
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
import ph.mart.healthapp.core.data.exercise.StrengthSet
import ph.mart.healthapp.core.data.exercise.summaryLabel
import ph.mart.healthapp.core.data.exercise.totalBurnedKcal
import ph.mart.healthapp.core.data.profile.UnitSystem
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.core.designsystem.theme.tabularNums
import ph.mart.healthapp.feature.food.ui.diary.components.MealSectionHeader
import ph.mart.healthapp.feature.food.ui.shared.components.EMPTY_SECTION_LABEL
import ph.mart.healthapp.feature.food.ui.shared.components.EntryIndent
import ph.mart.healthapp.feature.food.ui.shared.components.SwipeToDeleteRow

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
    onDeleteEntry: (ExerciseEntry) -> Unit,
    onEditEntry: (ExerciseEntry) -> Unit,
    unit: UnitSystem = UnitSystem.Metric,
) {
    Column {
        MealSectionHeader(
            label = "Exercise",
            subtotalKcal = entries.totalBurnedKcal(),
            expanded = expanded,
            onToggle = onToggle,
            onAdd = onAdd,
            // Four sections above this one report calories eaten in the same slot. This one
            // reports calories spent, and it raises the day's budget rather than filling it.
            burned = true,
        )
        if (expanded) {
            if (entries.isEmpty()) {
                Text(
                    text = EMPTY_SECTION_LABEL,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = EntryIndent, end = 16.dp, bottom = 12.dp),
                )
            }
            entries.forEach { entry ->
                key(entry.id) {
                    SwipeableExerciseRow(
                        entry = entry,
                        unit = unit,
                        onDelete = { onDeleteEntry(entry) },
                        onEdit = { onEditEntry(entry) },
                    )
                }
            }
        }
    }
}

@Composable
private fun SwipeableExerciseRow(
    entry: ExerciseEntry,
    unit: UnitSystem,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
) {
    SwipeToDeleteRow(onDelete = onDelete) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                // Same order as the meal rows': ripple over the background, target the whole row.
                .clickable(onClickLabel = "Edit activity", onClick = onEdit)
                .padding(start = EntryIndent, end = 16.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(entry.type.label),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = listOfNotNull("${entry.minutes} min", entry.name.takeIf { it.isNotBlank() })
                        .joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                // What was lifted, on its own line — a strength row's whole point, and the only
                // thing that distinguishes it from the cardio rows above it.
                if (entry.sets.isNotEmpty()) {
                    Text(
                        text = entry.sets.summaryLabel(unit),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
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
                    ExerciseEntry(
                        id = 3,
                        type = ExerciseType.Strength,
                        name = "Push day",
                        minutes = 45,
                        burnedKcal = 260,
                        sets = listOf(
                            StrengthSet("Bench press", 8, 60.0),
                            StrengthSet("Bench press", 8, 62.5),
                            StrengthSet("Dip", 10, 0.0),
                        ),
                    ),
                ),
                expanded = true,
                onToggle = {},
                onAdd = {},
                onDeleteEntry = {},
                onEditEntry = {},
            )
        }
    }
}
