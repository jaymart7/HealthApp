package ph.mart.healthapp.feature.food.ui.diary.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.data.food.FoodEntry
import ph.mart.healthapp.core.data.food.MealType
import ph.mart.healthapp.core.designsystem.component.FoodItemRow
import ph.mart.healthapp.core.designsystem.component.FoodItemRowVariant
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.food.ui.shared.components.EMPTY_SECTION_LABEL
import ph.mart.healthapp.feature.food.ui.shared.components.EntryIndent
import ph.mart.healthapp.feature.food.ui.shared.components.FILTERED_SECTION_LABEL
import ph.mart.healthapp.feature.food.ui.shared.components.SwipeToDeleteRow

/**
 * One meal's slice of the diary: its header, then its entries.
 *
 * [entries] is what the diary's filter left visible, while [subtotalKcal] is the whole section's —
 * so [filteredOut] is what tells the two apart. Without it a filtered section said "nothing here
 * yet" directly beneath a header still reporting 325 kcal, which is a straight contradiction and
 * blames the diary for what the user's own filter did.
 */
@Composable
internal fun MealSection(
    mealType: MealType,
    entries: List<FoodEntry>,
    subtotalKcal: Int,
    expanded: Boolean,
    filteredOut: Boolean,
    onToggle: () -> Unit,
    onAdd: () -> Unit,
    onSave: (() -> Unit)?,
    onDeleteEntry: (FoodEntry) -> Unit,
    onEditEntry: (FoodEntry) -> Unit,
) {
    Column {
        MealSectionHeader(
            label = mealType.name,
            subtotalKcal = subtotalKcal,
            expanded = expanded,
            onToggle = onToggle,
            onAdd = onAdd,
            onSave = onSave,
        )
        if (expanded) {
            if (entries.isEmpty()) {
                Text(
                    text = if (filteredOut) FILTERED_SECTION_LABEL else EMPTY_SECTION_LABEL,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = EntryIndent, end = 16.dp, bottom = 12.dp),
                )
            }
            entries.forEach { entry ->
                key(entry.id) {
                    SwipeToDeleteRow(onDelete = { onDeleteEntry(entry) }) {
                        Box(
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.surface)
                                // After the background so the ripple lands on top of it, before the
                                // padding so the whole row is the target rather than just the text.
                                .clickable(onClickLabel = "Edit entry") { onEditEntry(entry) }
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
                onToggle = {},
                onAdd = {},
                onSave = {},
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
                onToggle = {},
                onAdd = {},
                onSave = {},
                onDeleteEntry = {},
                onEditEntry = {},
            )
        }
    }
}
