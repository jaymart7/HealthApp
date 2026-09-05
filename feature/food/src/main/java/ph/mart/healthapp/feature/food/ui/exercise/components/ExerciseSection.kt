package ph.mart.healthapp.feature.food.ui.exercise.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.data.exercise.ExerciseEntry
import ph.mart.healthapp.core.data.exercise.ExerciseType
import ph.mart.healthapp.core.data.exercise.StrengthSet
import ph.mart.healthapp.core.data.exercise.summaryLabel
import ph.mart.healthapp.core.data.exercise.totalBurnedKcal
import ph.mart.healthapp.core.data.profile.UnitSystem
import ph.mart.healthapp.core.designsystem.component.AppCard
import ph.mart.healthapp.core.designsystem.icon.AppIcons
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.core.designsystem.theme.Motion
import ph.mart.healthapp.core.designsystem.theme.tabularNums
import ph.mart.healthapp.feature.food.R
import ph.mart.healthapp.feature.food.ui.diary.components.MealSectionHeader
import ph.mart.healthapp.feature.food.ui.diary.components.SectionCorner
import ph.mart.healthapp.feature.food.ui.shared.components.EntryIndent
import ph.mart.healthapp.feature.food.ui.shared.components.SwipeToDeleteRow

/**
 * The diary's exercise block — same collapsible header and swipe-to-delete as the meal sections,
 * but its own row: an activity has a duration where a food has a portion, and no macros at all, so
 * `FoodItemRow` would be mostly empty columns.
 *
 * It used to be, visually, a fifth meal — same flat surface, same header, a subtotal in the same
 * slot that happened to say "burned". Four things now say otherwise and none of them spends a
 * colour: it sits below a labelled rule rather than inside the run of meals, its container is
 * **transparent with a 1dp `outlineVariant` border** — the exact inverse of the meal cards'
 * filled-no-border — it carries a run glyph, and its subtotal is signed. Calories here are spent,
 * not eaten, and they raise the day's budget rather than filling it.
 */
@Composable
internal fun ExerciseSection(
    entries: List<ExerciseEntry>,
    expanded: Boolean,
    dayIsEmpty: Boolean,
    onToggle: () -> Unit,
    onAdd: () -> Unit,
    onDeleteEntry: (ExerciseEntry) -> Unit,
    onEditEntry: (ExerciseEntry) -> Unit,
    unit: UnitSystem = UnitSystem.Metric,
) {
    val burned = entries.totalBurnedKcal()
    AppCard(
        color = Color.Transparent,
        shape = SectionCorner,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        contentPadding = PaddingValues(0.dp),
    ) {
        MealSectionHeader(
            label = stringResource(R.string.food_exercise),
            expanded = expanded,
            contentDescription = stringResource(R.string.food_exercise_spoken, burned),
            subtotalText = if (burned == 0) null else stringResource(R.string.food_exercise_subtotal, burned),
            leadingIcon = AppIcons.Run,
            onToggle = onToggle,
            onAdd = onAdd,
            // No bookmark: a workout is not a thing you save and re-log as a set of ingredients.
        )
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(tween(Motion.State, easing = Motion.Standard)),
            exit = shrinkVertically(tween(Motion.State, easing = Motion.Standard)),
        ) {
            Column(modifier = Modifier.padding(bottom = 4.dp)) {
                if (entries.isEmpty() && !dayIsEmpty) {
                    Text(
                        text = stringResource(R.string.food_section_empty),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = EntryIndent, end = 16.dp, bottom = 8.dp),
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
}

@Composable
private fun SwipeableExerciseRow(
    entry: ExerciseEntry,
    unit: UnitSystem,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
) {
    val editActivity = stringResource(R.string.food_edit_activity)
    SwipeToDeleteRow(onDelete = onDelete) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                // `surface`, not the meal cards' `surfaceContainerLow`: this container is
                // transparent, so the row's opaque backing — the thing that keeps the delete
                // reveal hidden — has to be the screen's own tone.
                .background(MaterialTheme.colorScheme.surface)
                // Same order as the meal rows': ripple over the background, target the whole row.
                .clickable(onClickLabel = editActivity, onClick = onEdit)
                .padding(start = EntryIndent, end = 16.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(entry.type.label),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = listOfNotNull(
                        stringResource(R.string.food_exercise_row, entry.minutes),
                        entry.name.takeIf { it.isNotBlank() },
                    )
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
                text = stringResource(R.string.food_exercise_row_kcal, entry.burnedKcal),
                style = MaterialTheme.typography.titleMedium.tabularNums,
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
                dayIsEmpty = false,
                onToggle = {},
                onAdd = {},
                onDeleteEntry = {},
                onEditEntry = {},
            )
        }
    }
}

/** Nothing logged. The outline has to still read as a container rather than as a stray line. */
@PreviewLightDark
@Composable
private fun ExerciseSectionEmptyPreview() {
    AppTheme {
        Surface {
            ExerciseSection(
                entries = emptyList(),
                expanded = true,
                dayIsEmpty = false,
                onToggle = {},
                onAdd = {},
                onDeleteEntry = {},
                onEditEntry = {},
            )
        }
    }
}
