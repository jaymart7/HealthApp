package ph.mart.healthapp.feature.food.ui.history.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.data.food.FoodEntry
import ph.mart.healthapp.core.data.food.MealType
import ph.mart.healthapp.core.designsystem.component.FoodItemRow
import ph.mart.healthapp.core.designsystem.component.FoodItemRowVariant
import ph.mart.healthapp.core.designsystem.component.formatEpochDay
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.food.R

/**
 * One day of history: the date, then the rows logged on it.
 *
 * The date is absolute at every age — no "Today"/"Yesterday" here, unlike the diary's own header.
 * A list that spans months is scanned by date rather than read top-down, and two relative labels
 * among forty absolute ones are the two that have to be decoded.
 *
 * Each row is the diary's own [FoodItemRow] plus the meal it was logged in, and the **whole card**
 * is the tap: it opens that row for review, where the meal, the portion and the figures can be
 * corrected before anything is written. It used to be a "+" in the corner that logged the row the
 * moment it was touched. Like `FoodSuggestionPanel`, this still never writes — [onSelect] is a
 * callback and the write is the host's, two steps further on.
 */
@Composable
internal fun HistoryDayGroup(
    dateEpochDay: Long,
    entries: List<FoodEntry>,
    onSelect: (FoodEntry) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = formatEpochDay(dateEpochDay),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        entries.forEach { entry ->
            HistoryRow(entry = entry, onSelect = { onSelect(entry) })
        }
    }
}

@Composable
private fun HistoryRow(entry: FoodEntry, onSelect: () -> Unit) {
    val mealLabel = stringResource(entry.mealType.labelRes)
    // Resolved here because a semantics lambda cannot read a resource. The row's own text is what
    // the card announces; this is the sentence that says what activating it does.
    val reviewLabel = stringResource(R.string.food_history_review, entry.name)
    Surface(
        // Surface's own onClick overload, so the ripple is clipped to the card's corners — the
        // reasoning AppCard states at its own onClick branch.
        onClick = onSelect,
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier
            .fillMaxWidth()
            .semantics { onClick(label = reviewLabel, action = null) },
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            FoodItemRow(
                variant = FoodItemRowVariant.Display,
                name = entry.name,
                portionAmount = entry.portionAmount,
                portionUnit = entry.portionUnit,
                calories = entry.calories,
                proteinG = entry.proteinG,
                carbsG = entry.carbsG,
                fatG = entry.fatG,
                // The plate this row was logged with, the 40dp tile the diary gives it. The copy
                // the review writes gets none — see FoodEntry.toReviewForm().
                photoPath = entry.photoPath,
            )
            Text(
                text = mealLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
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
                onSelect = {},
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}
