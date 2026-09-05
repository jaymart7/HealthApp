package ph.mart.healthapp.feature.food.ui.diary.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.data.food.FoodEntry
import ph.mart.healthapp.core.data.food.MealType
import ph.mart.healthapp.core.designsystem.component.AppCard
import ph.mart.healthapp.core.designsystem.component.FoodItemRow
import ph.mart.healthapp.core.designsystem.component.FoodItemRowVariant
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.core.designsystem.theme.Motion
import ph.mart.healthapp.feature.food.R
import ph.mart.healthapp.feature.food.ui.shared.components.EntryIndent
import ph.mart.healthapp.feature.food.ui.shared.components.SwipeToDeleteRow
import ph.mart.healthapp.feature.food.ui.shared.labelRes

/** The section cards' corner. Tighter than [AppCard]'s own 20dp because four of these stack down
 * one screen, where a card is a band in a run rather than a block on its own. */
internal val SectionCorner = RoundedCornerShape(16.dp)

/**
 * One meal's slice of the diary: its header, then its entries, on a `surfaceContainerLow` card.
 *
 * The card is what the redesign buys. Five sections used to run together on one flat `surface`, so
 * a subtotal and the row beneath it belonged to the same block only by proximity; lifting each
 * meal one tone off the background is what makes a section a thing you can point at — and it is
 * what leaves the exercise block's *outline* free to mean the opposite (see `ExerciseSection`).
 *
 * [entries] is what the diary's filter left visible, while [subtotalKcal] is the whole section's —
 * so [filteredOut] is what tells the two apart. Without it a filtered section said "nothing here
 * yet" directly beneath a header still reporting 325 kcal, which is a straight contradiction and
 * blames the diary for what the user's own filter did.
 *
 * [dayIsEmpty] suppresses that line entirely. On a bare day `EmptyDiaryDay` speaks once for the
 * whole screen; this line is for a single empty section on an otherwise populated day. Without the
 * flag the two both fire, which is the state the mascot block was added to replace and never
 * actually did.
 */
@Composable
internal fun MealSection(
    mealType: MealType,
    entries: List<FoodEntry>,
    subtotalKcal: Int,
    expanded: Boolean,
    filteredOut: Boolean,
    dayIsEmpty: Boolean,
    onToggle: () -> Unit,
    onAdd: () -> Unit,
    onSave: (() -> Unit)?,
    onDeleteEntry: (FoodEntry) -> Unit,
    onEditEntry: (FoodEntry) -> Unit,
) {
    val label = stringResource(mealType.labelRes())
    AppCard(shape = SectionCorner, contentPadding = PaddingValues(0.dp)) {
        MealSectionHeader(
            label = label,
            expanded = expanded,
            contentDescription = stringResource(R.string.food_section_spoken, label, subtotalKcal),
            // Formatted here rather than inside the header, so the exercise section can sign its
            // own. Zero prints nothing at all.
            subtotalText = if (subtotalKcal == 0) null else stringResource(R.string.food_section_kcal, subtotalKcal),
            onToggle = onToggle,
            onAdd = onAdd,
            onSave = onSave,
        )
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(tween(Motion.State, easing = Motion.Standard)),
            exit = shrinkVertically(tween(Motion.State, easing = Motion.Standard)),
        ) {
            Column(modifier = Modifier.padding(bottom = 4.dp)) {
                val editEntry = stringResource(R.string.food_edit_entry)
                if (entries.isEmpty() && !dayIsEmpty) {
                    Text(
                        text = stringResource(
                            if (filteredOut) R.string.food_section_filtered else R.string.food_section_empty,
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = EntryIndent, end = 16.dp, bottom = 8.dp),
                    )
                }
                entries.forEach { entry ->
                    key(entry.id) {
                        SwipeToDeleteRow(onDelete = { onDeleteEntry(entry) }) {
                            Box(
                                modifier = Modifier
                                    // The card's own tone, not `surface`: the row has to be opaque
                                    // so the delete reveal stays hidden behind it, and anything but
                                    // the card's fill would draw a visible band across the card.
                                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
                                    // After the background so the ripple lands on top of it, before
                                    // the padding so the whole row is the target, not just the text.
                                    .clickable(onClickLabel = editEntry) { onEditEntry(entry) }
                                    .padding(start = EntryIndent, end = 16.dp, top = 8.dp, bottom = 8.dp),
                            ) {
                                FoodItemRow(
                                    variant = FoodItemRowVariant.Display,
                                    name = entry.name,
                                    portionAmount = entry.portionAmount,
                                    portionUnit = entry.portionUnit,
                                    calories = entry.calories,
                                    proteinG = entry.proteinG,
                                    carbsG = entry.carbsG,
                                    fatG = entry.fatG,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun MealSectionPreview() {
    AppTheme {
        Surface {
            MealSection(
                mealType = MealType.Breakfast,
                entries = listOf(
                    FoodEntry(id = 1, name = "Greek yogurt", mealType = MealType.Breakfast, portionAmount = 1.0, portionUnit = "cup", calories = 150, proteinG = 20, carbsG = 8, fatG = 4),
                ),
                subtotalKcal = 150,
                expanded = true,
                filteredOut = false,
                dayIsEmpty = false,
                onToggle = {},
                onAdd = {},
                onSave = {},
                onDeleteEntry = {},
                onEditEntry = {},
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun MealSectionEmptyPreview() {
    AppTheme {
        Surface {
            MealSection(
                mealType = MealType.Dinner,
                entries = emptyList(),
                subtotalKcal = 0,
                expanded = true,
                filteredOut = false,
                dayIsEmpty = false,
                onToggle = {},
                onAdd = {},
                // Nothing logged means nothing to snapshot — the header hides its save affordance.
                onSave = null,
                onDeleteEntry = {},
                onEditEntry = {},
            )
        }
    }
}

/** The filter hid this section's only entry. The subtotal stays — the food is still logged — so
 * the line beneath it has to say which of the two is true. */
@PreviewLightDark
@Composable
private fun MealSectionFilteredPreview() {
    AppTheme {
        Surface {
            MealSection(
                mealType = MealType.Lunch,
                entries = emptyList(),
                subtotalKcal = 480,
                expanded = true,
                filteredOut = true,
                dayIsEmpty = false,
                onToggle = {},
                onAdd = {},
                onSave = {},
                onDeleteEntry = {},
                onEditEntry = {},
            )
        }
    }
}

/** A bare day: the card still renders, its "+" is still reachable, and it says nothing — the
 * mascot block between Lunch and Dinner is the one line that speaks. */
@PreviewLightDark
@Composable
private fun MealSectionDayEmptyPreview() {
    AppTheme {
        Surface {
            MealSection(
                mealType = MealType.Snacks,
                entries = emptyList(),
                subtotalKcal = 0,
                expanded = true,
                filteredOut = false,
                dayIsEmpty = true,
                onToggle = {},
                onAdd = {},
                onSave = null,
                onDeleteEntry = {},
                onEditEntry = {},
            )
        }
    }
}

/** Collapsed: the subtotal is the whole of what the section says. */
@PreviewLightDark
@Composable
private fun MealSectionCollapsedPreview() {
    AppTheme {
        Surface {
            MealSection(
                mealType = MealType.Snacks,
                entries = listOf(
                    FoodEntry(id = 1, name = "Mixed nuts", mealType = MealType.Snacks, portionAmount = 40.0, portionUnit = "g", calories = 250, proteinG = 8, carbsG = 8, fatG = 22),
                ),
                subtotalKcal = 250,
                expanded = false,
                filteredOut = false,
                dayIsEmpty = false,
                onToggle = {},
                onAdd = {},
                onSave = {},
                onDeleteEntry = {},
                onEditEntry = {},
            )
        }
    }
}
