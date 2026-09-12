package ph.mart.healthapp.feature.food.ui.history.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.data.food.FoodEntry
import ph.mart.healthapp.core.data.food.MealType
import ph.mart.healthapp.core.designsystem.component.FoodItemRow
import ph.mart.healthapp.core.designsystem.component.FoodItemRowVariant
import ph.mart.healthapp.core.designsystem.component.formatEpochDay
import ph.mart.healthapp.core.designsystem.icon.AppIcons
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.food.R

/**
 * One day of history: the date, then the rows logged on it.
 *
 * The date is absolute at every age — no "Today"/"Yesterday" here, unlike the diary's own header.
 * A list that spans months is scanned by date rather than read top-down, and two relative labels
 * among forty absolute ones are the two that have to be decoded.
 *
 * Each row is the diary's own [FoodItemRow] plus the meal it was logged in and a "+" that logs it
 * again — `FoodSuggestionPanel`'s row, re-drawn for a row that has a date. Like that panel, this
 * one never writes: the "+" is a callback and the write is the host's.
 */
@Composable
internal fun HistoryDayGroup(
    dateEpochDay: Long,
    entries: List<FoodEntry>,
    onLogAgain: (FoodEntry) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = formatEpochDay(dateEpochDay),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        entries.forEach { entry ->
            HistoryRow(entry = entry, onLogAgain = { onLogAgain(entry) })
        }
    }
}

@Composable
private fun HistoryRow(entry: FoodEntry, onLogAgain: () -> Unit) {
    val mealLabel = stringResource(entry.mealType.labelRes)
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 12.dp, top = 8.dp, bottom = 8.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                FoodItemRow(
                    variant = FoodItemRowVariant.Display,
                    name = entry.name,
                    portionAmount = entry.portionAmount,
                    portionUnit = entry.portionUnit,
                    calories = entry.calories,
                    proteinG = entry.proteinG,
                    carbsG = entry.carbsG,
                    fatG = entry.fatG,
                    // The plate this row was logged with, the 40dp tile the diary gives it. The
                    // copy the "+" writes gets none — see FoodHistoryViewModel.
                    photoPath = entry.photoPath,
                )
                Text(
                    text = mealLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onLogAgain, modifier = Modifier.size(44.dp)) {
                Icon(
                    imageVector = AppIcons.Add,
                    contentDescription = stringResource(R.string.food_log_again, entry.name),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun HistoryDayGroupPreview() {
    AppTheme {
        Surface {
            HistoryDayGroup(
                dateEpochDay = 20_000L,
                entries = listOf(
                    FoodEntry(id = 1, name = "Greek yogurt", dateEpochDay = 20_000L, mealType = MealType.Breakfast, portionAmount = 1.0, portionUnit = "cup", calories = 150, proteinG = 20, carbsG = 8, fatG = 4),
                    FoodEntry(id = 2, name = "Grilled chicken breast", dateEpochDay = 20_000L, mealType = MealType.Lunch, portionAmount = 150.0, portionUnit = "g", calories = 210, proteinG = 32, carbsG = 2, fatG = 8),
                ),
                onLogAgain = {},
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}
