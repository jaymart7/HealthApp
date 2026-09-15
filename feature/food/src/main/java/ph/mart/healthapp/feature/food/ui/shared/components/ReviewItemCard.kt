package ph.mart.healthapp.feature.food.ui.shared.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.data.food.MealType
import ph.mart.healthapp.core.designsystem.component.AppCard
import ph.mart.healthapp.core.designsystem.component.FoodItemRow
import ph.mart.healthapp.core.designsystem.component.FoodItemRowVariant
import ph.mart.healthapp.core.designsystem.component.MacroFieldGroup
import ph.mart.healthapp.core.designsystem.component.MicronutrientInputGroup
import ph.mart.healthapp.core.designsystem.icon.AppIcons
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.food.R
import ph.mart.healthapp.feature.food.ui.shared.AddEntryForm
import ph.mart.healthapp.feature.food.ui.shared.withPortionAmount

/**
 * One estimated food, before any of it is written: the row as it stands, and the whole form behind
 * a tap.
 *
 * In `shared/` because two flows review a list of estimates now — talk-to-log, which reads a
 * sentence, and the photo flow, which reads a plate. It was the voice screen's private card until
 * a photographed plate stopped being a single food; the rule is the one `CLAUDE.md` states, that a
 * component crossing flows inside one feature does not stay in whichever flow declared it first.
 *
 * Collapsed by default and opened one at a time by the caller — the list is the thing being
 * checked, and a screen of five expanded forms is not a list. A portion change reprices through
 * the existing [withPortionAmount], so the numbers below the name stay consistent with it.
 */
@Composable
internal fun ReviewItemCard(
    item: AddEntryForm,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    onChange: (AddEntryForm) -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AppCard(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                onClick = onToggleExpanded,
                color = Color.Transparent,
                modifier = Modifier.weight(1f),
            ) {
                FoodItemRow(
                    variant = FoodItemRowVariant.Display,
                    name = item.name,
                    portionAmount = item.portionAmount,
                    portionUnit = item.portionUnit,
                    calories = item.calories ?: 0,
                    proteinG = item.proteinG ?: 0,
                    carbsG = item.carbsG ?: 0,
                    fatG = item.fatG ?: 0,
                    modifier = Modifier.padding(vertical = 4.dp),
                )
            }
            IconButton(onClick = onRemove, modifier = Modifier.size(40.dp)) {
                Icon(
                    imageVector = AppIcons.Close,
                    contentDescription = stringResource(R.string.food_remove, item.name),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (expanded) {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(top = 12.dp),
            ) {
                FoodItemRow(
                    variant = FoodItemRowVariant.Editable,
                    name = item.name,
                    portionAmount = item.portionAmount,
                    portionUnit = item.portionUnit,
                    calories = item.calories ?: 0,
                    proteinG = item.proteinG ?: 0,
                    carbsG = item.carbsG ?: 0,
                    fatG = item.fatG ?: 0,
                    onNameChange = { onChange(item.copy(name = it)) },
                    onPortionAmountChange = { onChange(item.withPortionAmount(it)) },
                    onPortionUnitChange = { onChange(item.copy(portionUnit = it)) },
                    onCaloriesChange = { onChange(item.copy(calories = it)) },
                )
                MacroFieldGroup(
                    proteinG = item.proteinG,
                    carbsG = item.carbsG,
                    fatG = item.fatG,
                    onProteinChange = { onChange(item.copy(proteinG = it)) },
                    onCarbsChange = { onChange(item.copy(carbsG = it)) },
                    onFatChange = { onChange(item.copy(fatG = it)) },
                )
                MicronutrientInputGroup(
                    fiberG = item.nutrients.fiberG.takeIf { it > 0 },
                    sugarG = item.nutrients.sugarG.takeIf { it > 0 },
                    sodiumMg = item.nutrients.sodiumMg.takeIf { it > 0 },
                    onFiberChange = { onChange(item.copy(nutrients = item.nutrients.copy(fiberG = it ?: 0))) },
                    onSugarChange = { onChange(item.copy(nutrients = item.nutrients.copy(sugarG = it ?: 0))) },
                    onSodiumChange = { onChange(item.copy(nutrients = item.nutrients.copy(sodiumMg = it ?: 0))) },
                )
            }
        }
    }
}

private val PREVIEW_ITEM = AddEntryForm(
    MealType.Lunch, "Grilled chicken breast", 150.0, "g", 210, 32, 2, 8,
)

@PreviewLightDark
@Composable
private fun ReviewItemCardPreview() {
    AppTheme {
        ReviewItemCard(
            item = PREVIEW_ITEM,
            expanded = false,
            onToggleExpanded = {},
            onChange = {},
            onRemove = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun ReviewItemCardExpandedPreview() {
    AppTheme {
        ReviewItemCard(
            item = PREVIEW_ITEM,
            expanded = true,
            onToggleExpanded = {},
            onChange = {},
            onRemove = {},
        )
    }
}
