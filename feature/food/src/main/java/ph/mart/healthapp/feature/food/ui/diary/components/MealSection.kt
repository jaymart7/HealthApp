package ph.mart.healthapp.feature.food.ui.diary.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
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
import ph.mart.healthapp.core.data.food.FoodEntry
import ph.mart.healthapp.core.data.food.MealType
import ph.mart.healthapp.core.designsystem.component.FoodItemRow
import ph.mart.healthapp.core.designsystem.component.FoodItemRowVariant
import ph.mart.healthapp.core.designsystem.theme.AppTheme

@Composable
internal fun MealSection(
    mealType: MealType,
    entries: List<FoodEntry>,
    subtotalKcal: Int,
    expanded: Boolean,
    onToggle: () -> Unit,
    onAdd: () -> Unit,
    onSave: (() -> Unit)?,
    onDeleteEntry: (FoodEntry) -> Unit,
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
                    text = "Nothing logged for ${mealType.name} yet.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 38.dp, end = 16.dp, bottom = 12.dp),
                )
            }
            entries.forEach { entry ->
                key(entry.id) {
                    SwipeableFoodEntryRow(entry = entry, onDelete = { onDeleteEntry(entry) })
                }
            }
        }
    }
}

@Composable
private fun SwipeableFoodEntryRow(entry: FoodEntry, onDelete: () -> Unit) {
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
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.error),
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
        Box(modifier = Modifier.background(MaterialTheme.colorScheme.surface).padding(start = 38.dp, end = 16.dp, top = 8.dp, bottom = 8.dp)) {
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

@PreviewLightDark
@Composable
private fun MealSectionPreview() {
    AppTheme {
        MealSection(
            mealType = MealType.Breakfast,
            entries = listOf(
                FoodEntry(id = 1, name = "Greek yogurt", mealType = MealType.Breakfast, portionAmount = 1.0, portionUnit = "cup", calories = 150, proteinG = 20, carbsG = 8, fatG = 4),
            ),
            subtotalKcal = 150,
            expanded = true,
            onToggle = {},
            onAdd = {},
            onSave = {},
            onDeleteEntry = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun MealSectionEmptyPreview() {
    AppTheme {
        MealSection(
            mealType = MealType.Dinner,
            entries = emptyList(),
            subtotalKcal = 0,
            expanded = true,
            onToggle = {},
            onAdd = {},
            // Nothing logged means nothing to snapshot — the header hides its save affordance.
            onSave = null,
            onDeleteEntry = {},
        )
    }
}
