package ph.mart.healthapp.feature.food.ui.ideas.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.data.food.MealIdea
import ph.mart.healthapp.core.designsystem.component.AppCard
import ph.mart.healthapp.core.designsystem.component.FoodItemRow
import ph.mart.healthapp.core.designsystem.component.FoodItemRowVariant
import ph.mart.healthapp.core.designsystem.theme.AppTheme

/**
 * One suggestion, drawn as the diary already draws a food: the whole card is the tap target, and
 * the tap seeds the add-entry sheet rather than logging — an estimate has to be adjustable before
 * it becomes a row, the same contract the photo flow's confirmation screen has.
 *
 * [FoodItemRow] rather than a fourth hand-drawn food row: an idea, a diary entry, a search hit and
 * a recipe all read the same way, and the macro line's semantic colours are already right there.
 */
@Composable
internal fun MealIdeaCard(idea: MealIdea, onSelect: () -> Unit, modifier: Modifier = Modifier) {
    AppCard(onClick = onSelect, modifier = modifier) {
        FoodItemRow(
            variant = FoodItemRowVariant.Display,
            name = idea.name,
            portionAmount = idea.portionAmount,
            portionUnit = idea.portionUnit,
            calories = idea.calories,
            proteinG = idea.proteinG,
            carbsG = idea.carbsG,
            fatG = idea.fatG,
        )
    }
}

@PreviewLightDark
@Composable
private fun MealIdeaCardPreview() {
    AppTheme {
        Surface {
            MealIdeaCard(
                idea = MealIdea(
                    name = "Greek yogurt with berries",
                    portionAmount = 1.0,
                    portionUnit = "cup",
                    calories = 220,
                    proteinG = 22,
                    carbsG = 24,
                    fatG = 4,
                ),
                onSelect = {},
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}
