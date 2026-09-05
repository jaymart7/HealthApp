package ph.mart.healthapp.feature.food.ui.diary.components

import androidx.compose.foundation.clickable
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
import ph.mart.healthapp.core.data.food.FoodSuggestion
import ph.mart.healthapp.core.designsystem.component.FoodItemRow
import ph.mart.healthapp.core.designsystem.component.FoodItemRowVariant
import ph.mart.healthapp.core.designsystem.icon.AppIcons
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.food.R
import ph.mart.healthapp.feature.food.ui.search.components.FoodSearchPanel

/**
 * Starred favorites and recently logged foods, above the search field in the add-entry sheet.
 * Tapping a row seeds the sheet's fields — same contract as a [FoodSearchPanel] hit — while the
 * trailing "+" logs it as-is; the panel itself never writes anything.
 *
 * Renders nothing on a first run, when there is neither a favorite nor a logged food to offer.
 */
@Composable
internal fun FoodSuggestionPanel(
    suggestions: List<FoodSuggestion>,
    onSelect: (FoodSuggestion) -> Unit,
    onLogAgain: (FoodSuggestion) -> Unit,
    onToggleFavorite: (FoodSuggestion, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (suggestions.isEmpty()) return
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = stringResource(R.string.food_recents),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        suggestions.forEach { suggestion ->
            SuggestionRow(
                suggestion = suggestion,
                onClick = { onSelect(suggestion) },
                onLogAgain = { onLogAgain(suggestion) },
                onToggleFavorite = { onToggleFavorite(suggestion, !suggestion.isFavorite) },
            )
        }
    }
}

@Composable
private fun SuggestionRow(
    suggestion: FoodSuggestion,
    onClick: () -> Unit,
    onLogAgain: () -> Unit,
    onToggleFavorite: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 12.dp, top = 8.dp, bottom = 8.dp),
        ) {
            FoodItemRow(
                variant = FoodItemRowVariant.Display,
                name = suggestion.name,
                portionAmount = suggestion.portionAmount,
                portionUnit = suggestion.portionUnit,
                calories = suggestion.calories,
                proteinG = suggestion.proteinG,
                carbsG = suggestion.carbsG,
                fatG = suggestion.fatG,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onToggleFavorite, modifier = Modifier.size(44.dp)) {
                Icon(
                    imageVector = if (suggestion.isFavorite) AppIcons.Favorite.filled else AppIcons.Favorite.outlined,
                    contentDescription = if (suggestion.isFavorite) {
                        stringResource(R.string.food_favorite_remove, suggestion.name)
                    } else {
                        stringResource(R.string.food_favorite_add, suggestion.name)
                    },
                    tint = if (suggestion.isFavorite) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
            IconButton(onClick = onLogAgain, modifier = Modifier.size(44.dp)) {
                Icon(
                    imageVector = AppIcons.Add,
                    contentDescription = stringResource(R.string.food_log_again, suggestion.name),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun FoodSuggestionPanelPreview() {
    AppTheme {
        Surface {
            FoodSuggestionPanel(
                suggestions = listOf(
                    FoodSuggestion("Greek yogurt", 1.0, "cup", 150, 20, 8, 4, isFavorite = true),
                    FoodSuggestion("Grilled chicken breast", 150.0, "g", 210, 32, 2, 8, isFavorite = false),
                ),
                onSelect = {},
                onLogAgain = {},
                onToggleFavorite = { _, _ -> },
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}
