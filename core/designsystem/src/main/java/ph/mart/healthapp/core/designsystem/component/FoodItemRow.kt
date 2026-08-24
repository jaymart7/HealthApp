package ph.mart.healthapp.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.core.designsystem.theme.tabularNums

enum class FoodItemRowVariant { Display, Editable }

/**
 * One component, mode-switched, per the prototype's [FoodItemRowVariant.Display] (food diary
 * rows) / [FoodItemRowVariant.Editable] (Confirmation screen, Phase 5) split — never forked into
 * two components. [proteinG]/[carbsG]/[fatG] are always display-only here; macro editing is a
 * separately-composed [MacroInputGroup], per the prototype's Confirmation screen layout.
 */
@Composable
fun FoodItemRow(
    variant: FoodItemRowVariant,
    name: String,
    portionAmount: Double,
    portionUnit: String,
    calories: Int,
    proteinG: Int,
    carbsG: Int,
    fatG: Int,
    modifier: Modifier = Modifier,
    onNameChange: (String) -> Unit = {},
    onPortionAmountChange: (Double) -> Unit = {},
    onPortionUnitChange: (String) -> Unit = {},
    onCaloriesChange: (Int) -> Unit = {},
    portionUnitOptions: List<String> = listOf("g", "oz", "cup"),
) {
    when (variant) {
        FoodItemRowVariant.Display -> DisplayRow(name, portionAmount, portionUnit, calories, proteinG, carbsG, fatG, modifier)
        FoodItemRowVariant.Editable -> EditableRow(
            name, portionAmount, portionUnit, calories, portionUnitOptions,
            onNameChange, onPortionAmountChange, onPortionUnitChange, onCaloriesChange, modifier,
        )
    }
}

@Composable
private fun DisplayRow(
    name: String,
    portionAmount: Double,
    portionUnit: String,
    calories: Int,
    proteinG: Int,
    carbsG: Int,
    fatG: Int,
    modifier: Modifier,
) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(text = name, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
            Text(
                text = "${portionAmount.formatPortion()} $portionUnit · P ${proteinG}g · C ${carbsG}g · F ${fatG}g",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = "$calories kcal",
            style = MaterialTheme.typography.titleMedium.tabularNums,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun EditableRow(
    name: String,
    portionAmount: Double,
    portionUnit: String,
    calories: Int,
    portionUnitOptions: List<String>,
    onNameChange: (String) -> Unit,
    onPortionAmountChange: (Double) -> Unit,
    onPortionUnitChange: (String) -> Unit,
    onCaloriesChange: (Int) -> Unit,
    modifier: Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        AppTextField(label = "Food", value = name, onValueChange = onNameChange)

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(text = "Portion", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
                    .padding(start = 16.dp, top = 4.dp, bottom = 4.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = portionAmount.formatPortion(),
                    style = MaterialTheme.typography.titleMedium.tabularNums,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.padding(end = 4.dp),
                ) {
                    Row(modifier = Modifier.padding(2.dp)) {
                        portionUnitOptions.forEach { unit ->
                            val selected = unit == portionUnit
                            Surface(
                                onClick = { onPortionUnitChange(unit) },
                                shape = RoundedCornerShape(999.dp),
                                color = if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
                                contentColor = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                            ) {
                                Text(
                                    text = unit,
                                    style = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                )
                            }
                        }
                    }
                }
                StepperButton(symbol = "−", onClick = { onPortionAmountChange((portionAmount - 10).coerceAtLeast(0.0)) })
                StepperButton(symbol = "+", onClick = { onPortionAmountChange(portionAmount + 10) })
            }
        }

        NumericStepperField(
            label = "Calories",
            value = calories.toString(),
            unitSuffix = "kcal",
            onIncrement = { onCaloriesChange(calories + 10) },
            onDecrement = { onCaloriesChange((calories - 10).coerceAtLeast(0)) },
        )
    }
}

private fun Double.formatPortion(): String = if (this == this.toLong().toDouble()) toLong().toString() else toString()

@PreviewLightDark
@Composable
private fun FoodItemRowDisplayPreview() {
    AppTheme {
        Surface {
            FoodItemRow(
                variant = FoodItemRowVariant.Display,
                name = "Grilled chicken breast",
                portionAmount = 150.0,
                portionUnit = "g",
                calories = 210,
                proteinG = 32,
                carbsG = 2,
                fatG = 8,
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun FoodItemRowEditablePreview() {
    AppTheme {
        Surface {
            FoodItemRow(
                variant = FoodItemRowVariant.Editable,
                name = "Grilled chicken breast",
                portionAmount = 150.0,
                portionUnit = "g",
                calories = 210,
                proteinG = 32,
                carbsG = 2,
                fatG = 8,
                onNameChange = {},
                onPortionAmountChange = {},
                onPortionUnitChange = {},
                onCaloriesChange = {},
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}
