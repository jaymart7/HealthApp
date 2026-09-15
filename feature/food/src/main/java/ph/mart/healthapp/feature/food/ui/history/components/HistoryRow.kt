package ph.mart.healthapp.feature.food.ui.history.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewLightDark
import ph.mart.healthapp.core.data.food.FoodEntry
import ph.mart.healthapp.core.data.food.MealType
import ph.mart.healthapp.core.designsystem.component.FoodItemRow
import ph.mart.healthapp.core.designsystem.component.FoodItemRowVariant
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.food.R

/**
 * One logged row in the history list.
 *
 * **The whole row is the tap**, and it opens the row for review — where the meal, the portion and
 * the figures can be corrected before anything is written. It used to be a "+" in the corner that
 * logged the row the moment it was touched. Like `FoodSuggestionPanel`, this still never writes:
 * [onSelect] is a callback and the write is the host's, two steps further on.
 *
 * The card this used to sit in is gone. Forty cards is forty containers to look past; the rows now
 * sit on rules, with the day headings doing the separating that the cards were failing to do.
 * `surface` rather than a fill, so the state layer the tap draws is the only thing that ever tints
 * a row — which is what makes "pressed" legible at all in a list this uniform.
 */
@Composable
internal fun HistoryRow(
    entry: FoodEntry,
    query: String,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val mealLabel = stringResource(entry.mealType.labelRes)
    // Resolved here because a semantics lambda cannot read a resource. The row's own text is what
    // it announces; this is the sentence that says what activating it does.
    val reviewLabel = stringResource(R.string.food_history_review, entry.name)
    Surface(
        // Surface's own onClick overload, so the state layer covers the row rather than a shape
        // inside it — the reasoning AppCard states at its own onClick branch.
        onClick = onSelect,
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier
            .fillMaxWidth()
            .semantics { onClick(label = reviewLabel, action = null) },
    ) {
        FoodItemRow(
            variant = FoodItemRowVariant.SearchResult,
            name = entry.name,
            portionAmount = entry.portionAmount,
            portionUnit = entry.portionUnit,
            calories = entry.calories,
            proteinG = entry.proteinG,
            carbsG = entry.carbsG,
            fatG = entry.fatG,
            // The plate this row was logged with. The copy the review writes gets none — see
            // FoodEntry.toReviewForm().
            photoPath = entry.photoPath,
            mealLabel = mealLabel,
            highlight = query,
        )
    }
}

@PreviewLightDark
@Composable
private fun HistoryRowPreview() {
    AppTheme {
        Surface {
            Column {
                HistoryRow(
                    entry = FoodEntry(id = 1, name = "Grilled chicken breast", dateEpochDay = 20_000L, mealType = MealType.Lunch, portionAmount = 150.0, portionUnit = "g", calories = 412, proteinG = 38, carbsG = 0, fatG = 9),
                    query = "chick",
                    onSelect = {},
                )
                HistoryRow(
                    entry = FoodEntry(id = 2, name = "Chicken & rice bowl", dateEpochDay = 20_000L, mealType = MealType.Dinner, portionAmount = 1.0, portionUnit = "bowl", calories = 520, proteinG = 34, carbsG = 61, fatG = 14),
                    query = "chick",
                    onSelect = {},
                )
            }
        }
    }
}
