package ph.mart.healthapp.feature.food.ui.library.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.data.food.Nutrients
import ph.mart.healthapp.core.designsystem.component.MacroFieldGroup
import ph.mart.healthapp.core.designsystem.component.MicronutrientInputGroup
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.food.R
import ph.mart.healthapp.feature.food.ui.shared.AddEntryForm
import ph.mart.healthapp.feature.food.ui.shared.components.CardLabel
import ph.mart.healthapp.feature.food.ui.shared.components.SubjectCard

/**
 * A food the user owns as a form to check — the scan review's subject card, macro fields and
 * micronutrient group, minus the meal chips and the Log button: keeping a food and logging it are
 * two intentions, and this screen only has the first.
 *
 * [manualEntry] is true when nothing seeded the form, which swaps the portion caveat for "enter the
 * values for this portion" and hides the presets.
 */
@Composable
internal fun FoodReview(
    food: AddEntryForm,
    manualEntry: Boolean,
    onFoodChange: (AddEntryForm) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SubjectCard(form = food, manualEntry = manualEntry, onFormChange = onFoodChange)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            CardLabel(stringResource(R.string.food_macros))
            MacroFieldGroup(
                proteinG = food.proteinG,
                carbsG = food.carbsG,
                fatG = food.fatG,
                onProteinChange = { onFoodChange(food.copy(proteinG = it)) },
                onCarbsChange = { onFoodChange(food.copy(carbsG = it)) },
                onFatChange = { onFoodChange(food.copy(fatG = it)) },
            )
        }
        MicronutrientInputGroup(
            fiberG = food.nutrients.fiberG.takeIf { it > 0 },
            sugarG = food.nutrients.sugarG.takeIf { it > 0 },
            sodiumMg = food.nutrients.sodiumMg.takeIf { it > 0 },
            onFiberChange = { onFoodChange(food.copy(nutrients = food.nutrients.copy(fiberG = it ?: 0))) },
            onSugarChange = { onFoodChange(food.copy(nutrients = food.nutrients.copy(sugarG = it ?: 0))) },
            onSodiumChange = { onFoodChange(food.copy(nutrients = food.nutrients.copy(sodiumMg = it ?: 0))) },
        )
    }
}

@PreviewLightDark
@Composable
private fun FoodReviewBlankPreview() {
    AppTheme {
        Surface {
            FoodReview(food = AddEntryForm(), manualEntry = true, onFoodChange = {}, modifier = Modifier.padding(16.dp))
        }
    }
}

@PreviewLightDark
@Composable
private fun FoodReviewFilledPreview() {
    AppTheme {
        Surface {
            FoodReview(
                food = AddEntryForm(
                    name = "Mum's adobo",
                    portionAmount = 1.0,
                    portionUnit = "serving",
                    calories = 420,
                    proteinG = 28,
                    carbsG = 12,
                    fatG = 28,
                    nutrients = Nutrients(sodiumMg = 900),
                ),
                manualEntry = false,
                onFoodChange = {},
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}
