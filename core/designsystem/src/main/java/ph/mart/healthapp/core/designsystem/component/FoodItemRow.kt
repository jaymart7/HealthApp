package ph.mart.healthapp.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.designsystem.R
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
    // Stays in Kotlin: these values are compared, not just shown — `portionStep` switches on
    // them, and a translated "cup" would silently take the 10-per-tap branch meant for grams.
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

/**
 * How far one tap of the portion stepper moves, per unit. Ten grams is a sensible nudge; ten cups
 * is not, and ten servings is nonsense — a stepper that steps in the wrong unit is why a seeded
 * recipe row could never be turned into half a portion.
 */
internal fun portionStep(unit: String): Double = when (unit) {
    "g", "oz" -> 10.0
    else -> 0.5
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
                text = macroLine(portionAmount, portionUnit, proteinG, carbsG, fatG),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = stringResource(R.string.ds_food_calories_value, calories),
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
        AppTextField(label = stringResource(R.string.ds_food_name), value = name, onValueChange = onNameChange)

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = stringResource(R.string.ds_food_portion), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
                    .padding(start = 16.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StepperValueField(
                    value = portionAmount.formatPortion(),
                    onValueChange = { onPortionAmountChange(it.toDoubleOrNull() ?: 0.0) },
                    contentDescription = stringResource(R.string.ds_food_portion_amount, portionUnit),
                    decimal = true,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = portionUnit,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                StepperButton(
                    symbol = "−",
                    label = stringResource(R.string.ds_food_portion_decrease),
                    onClick = {
                        onPortionAmountChange((portionAmount - portionStep(portionUnit)).coerceAtLeast(0.0))
                    },
                )
                StepperButton(
                    symbol = "+",
                    label = stringResource(R.string.ds_food_portion_increase),
                    onClick = { onPortionAmountChange(portionAmount + portionStep(portionUnit)) },
                )
            }
            // Its own row rather than crammed into the one above: four unit pills, a value field
            // and two steppers do not share 48dp of width, and they share it even less once the
            // system font scale goes up. [SegmentedToggle] is the system's own single-select row —
            // it brings the track that tells the unselected units apart from plain text, the
            // selection semantics, and a width floor that scrolls rather than squeezes.
            SegmentedToggle(
                options = portionUnitOptions,
                selectedIndex = portionUnitOptions.indexOf(portionUnit).coerceAtLeast(0),
                onSelect = { index -> onPortionUnitChange(portionUnitOptions[index]) },
                // This row is the first place the toggle sits on a bottom sheet, which is already
                // surfaceContainerLow — its default track would vanish into the sheet. One step up
                // the tone ladder is how the system separates a surface from what it carries.
                trackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            )
        }

        NumericStepperField(
            label = stringResource(R.string.ds_food_calories),
            value = calories.toString(),
            unitSuffix = "kcal",
            onIncrement = { onCaloriesChange(calories + 10) },
            onDecrement = { onCaloriesChange((calories - 10).coerceAtLeast(0)) },
            onValueChange = { onCaloriesChange(it.toIntOrNull() ?: 0) },
        )
    }
}

/**
 * "150 g · P 32g · C 2g · F 8g", with each macro's initial in that macro's own colour.
 *
 * The app has a fixed colour for protein, carbs and fat and spends it on bars and charts, while
 * the most-repeated element in the whole product — a diary row — said all three in the same grey.
 * Colouring the *letter* rather than the number is what keeps the line quiet: the letter is the
 * label the Fixed Macro Rule requires beside every macro colour, so the colour lands exactly on
 * the glyph that already carries the meaning, and the figures stay one uniform weight to scan
 * down. SemiBold because sage and moss are neighbours at 12sp, and a marker has to read as chosen.
 */
@Composable
private fun macroLine(
    portionAmount: Double,
    portionUnit: String,
    proteinG: Int,
    carbsG: Int,
    fatG: Int,
): AnnotatedString = buildAnnotatedString {
    append("${portionAmount.formatPortion()} $portionUnit")
    macroToken("P", proteinG, MaterialTheme.colorScheme.primary)
    macroToken("C", carbsG, MaterialTheme.colorScheme.tertiary)
    macroToken("F", fatG, MaterialTheme.colorScheme.secondary)
}

private fun AnnotatedString.Builder.macroToken(initial: String, grams: Int, color: Color) {
    append(" · ")
    withStyle(SpanStyle(color = color, fontWeight = FontWeight.SemiBold)) { append(initial) }
    append(" ${grams}g")
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

/** The seeded-recipe shape: a serving, where the stepper has to move in halves rather than tens. */
@PreviewLightDark
@Composable
private fun FoodItemRowEditableServingPreview() {
    AppTheme {
        Surface {
            FoodItemRow(
                variant = FoodItemRowVariant.Editable,
                name = "Chili",
                portionAmount = 1.0,
                portionUnit = "serving",
                calories = 395,
                proteinG = 32,
                carbsG = 20,
                fatG = 21,
                onNameChange = {},
                onPortionAmountChange = {},
                onPortionUnitChange = {},
                onCaloriesChange = {},
                portionUnitOptions = listOf("g", "oz", "cup", "serving"),
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}
