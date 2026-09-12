package ph.mart.healthapp.feature.food.ui.shared.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import ph.mart.healthapp.core.designsystem.component.SegmentedToggle
import ph.mart.healthapp.core.designsystem.icon.AppIcons
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.core.designsystem.theme.tabularNums
import ph.mart.healthapp.feature.food.R
import kotlin.math.abs
import kotlin.math.roundToLong

/** How long a finger has to stay down before the button starts repeating, and how fast it then
 * goes. Long enough that an ordinary tap is one step; fast enough that holding beats tapping. */
private const val REPEAT_DELAY_MS = 400L
private const val REPEAT_INTERVAL_MS = 80L

/** The units this control offers. Compared, not shown — `portionStep` switches on them and a
 * recipe row is priced in them, so a translated "cup" would take the grams branch. */
private val PORTION_UNITS = listOf("g", "oz", "cup")

/** The two amounts worth one tap. Grams only: half a cup is already one tap of the stepper, and
 * "50 oz" of anything is not a portion.
 *
 * ponytail: two fixed presets, not a per-product serving ("1 bar", "1 breast") — `ScannedProduct`
 * carries no serving description and neither Open Food Facts mapper reads one. Add a third preset
 * off a `servingDescription` field if a product's own serving turns out to be what people reach for.
 */
private val GRAM_PRESETS = listOf(50.0, 150.0)

/**
 * The portion, as a control rather than a text box.
 *
 * This is the one field on the review screen that everybody touches: the database answers per
 * 100 g and almost nothing is eaten in exactly 100 g of it. It used to be a number in a row of
 * numbers, which is why it read as one more thing to check rather than the thing to change. Now it
 * is the largest figure in the card, flanked by targets big enough to hit without looking, with the
 * two amounts worth one tap beneath it.
 *
 * The unit is a [SegmentedToggle] rather than the dropdown it replaces: three options never needed
 * a menu, and a menu costs a tap to find out what the options even are.
 *
 * **The caveat under it is the whole reason the screen can be quiet.** The per-100 g warning used to
 * be a subtitle at the top, a screen away from the number it was about; here it sits against that
 * number and names the factor currently being applied, so "×1.5" is visible at the moment it starts
 * being true. [manualEntry] swaps it for the opposite instruction, because a form nobody seeded has
 * no per-100 g values to scale.
 */
@Composable
internal fun PortionControl(
    amount: Double,
    unit: String,
    manualEntry: Boolean,
    onAmountChange: (Double) -> Unit,
    onUnitChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        CardLabel(stringResource(R.string.food_portion_label))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RoundIconButton(
                icon = AppIcons.Minus,
                contentDescription = stringResource(R.string.food_portion_decrease),
                onStep = { onAmountChange((amount - portionStepFor(unit)).coerceAtLeast(0.0)) },
            )
            Row(
                modifier = Modifier.padding(horizontal = 12.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                Text(
                    text = amount.formatPortion(),
                    style = MaterialTheme.typography.displaySmall.tabularNums,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = unit,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
                )
            }
            RoundIconButton(
                icon = AppIcons.Add,
                contentDescription = stringResource(R.string.food_portion_increase),
                onStep = { onAmountChange(amount + portionStepFor(unit)) },
            )
        }

        SegmentedToggle(
            options = PORTION_UNITS,
            selectedIndex = PORTION_UNITS.indexOf(unit).coerceAtLeast(0),
            onSelect = { onUnitChange(PORTION_UNITS[it]) },
            trackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        )

        if (unit == "g") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                GRAM_PRESETS.forEach { preset ->
                    PresetChip(
                        amount = preset,
                        selected = abs(amount - preset) < 0.5,
                        onClick = { onAmountChange(preset) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        Text(
            text = caveatFor(manualEntry, amount, unit),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * What the numbers in this card are figures *for*.
 *
 * The scale factor is only printable in grams: the seed is per 100 g, so `amount / 100` is the
 * factor — but switching the unit to ounces moves neither the amount nor the values, and a "×1.5"
 * against a number that means ounces would be arithmetic nobody performed.
 */
@Composable
private fun caveatFor(manualEntry: Boolean, amount: Double, unit: String): String {
    if (manualEntry) return stringResource(R.string.food_portion_caveat_manual)
    val base = stringResource(R.string.food_portion_caveat_per_100g)
    val factor = amount / 100.0
    if (unit != "g" || abs(factor - 1.0) < 0.005) return base
    return base + " " + stringResource(R.string.food_portion_scaled, factor.formatFactor())
}

@Composable
private fun PresetChip(amount: Double, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        color = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface,
        contentColor = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
        // The same two border weights the meal chips use: this app has exactly two.
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
        ),
        modifier = modifier
            .heightIn(min = 48.dp)
            .semantics {
                role = Role.RadioButton
                this.selected = selected
            },
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(8.dp)) {
            Text(
                // Not copy: a gram figure and its symbol.
                text = "${amount.formatPortion()} g",
                style = MaterialTheme.typography.labelLarge.tabularNums,
            )
        }
    }
}

/**
 * A 48dp circular target that repeats while held.
 *
 * Holding matters here and nowhere else in the app: every other stepper nudges a figure that is
 * already about right, while this one routinely has to walk 100 g down to 30 or up to 250. The
 * ticker is cancelled by the release, so a plain tap is exactly one step.
 */
@Composable
private fun RoundIconButton(icon: ImageVector, contentDescription: String, onStep: () -> Unit) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier
            .size(48.dp)
            .semantics { role = Role.Button }
            .pointerInput(onStep) {
                detectTapGestures(
                    onPress = {
                        onStep()
                        coroutineScope {
                            val ticker = launch {
                                delay(REPEAT_DELAY_MS)
                                while (isActive) {
                                    onStep()
                                    delay(REPEAT_INTERVAL_MS)
                                }
                            }
                            tryAwaitRelease()
                            ticker.cancel()
                        }
                    },
                )
            },
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(imageVector = icon, contentDescription = contentDescription)
        }
    }
}

/** The card's section labels: small, spaced, and quiet enough that the figure under each one is
 * the thing being read. */
@Composable
internal fun CardLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}

/** Ten grams is a sensible nudge; ten cups is not. The same split
 * [portionStep][ph.mart.healthapp.core.designsystem.component.FoodItemRow] makes, which is internal
 * to its module. */
private fun portionStepFor(unit: String): Double = if (unit == "cup") 0.5 else 10.0

internal fun Double.formatPortion(): String =
    if (this == toLong().toDouble()) toLong().toString() else toString()

/** "1.5", not "1.50" and not "1.4999999999999998". */
private fun Double.formatFactor(): String {
    val rounded = (this * 100).roundToLong() / 100.0
    return rounded.formatPortion()
}

@PreviewLightDark
@Composable
private fun PortionControlPreview() {
    AppTheme {
        Surface {
            PortionControl(
                amount = 150.0,
                unit = "g",
                manualEntry = false,
                onAmountChange = {},
                onUnitChange = {},
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun PortionControlManualPreview() {
    AppTheme {
        Surface {
            PortionControl(
                amount = 100.0,
                unit = "g",
                manualEntry = true,
                onAmountChange = {},
                onUnitChange = {},
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}
