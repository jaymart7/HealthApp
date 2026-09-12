package ph.mart.healthapp.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.designsystem.R
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.core.designsystem.theme.tabularNums

/** What a cell prints when nobody has supplied a figure. Never `0` — see [MacroFieldCell]. */
private const val UNSET = "—"

/** Four rows of digits is what a macro never needs and sodium routinely does; the cell is sized
 * for the widest of the three it draws rather than per-nutrient. */
private val CellShape = RoundedCornerShape(16.dp)

/**
 * One typable nutrient figure as a tile: dot, name, value, unit. Three of them side by side replace
 * the stepper rows [MacroInputGroup] draws, wherever the figures belong to a *food* — see
 * [MacroFieldGroup].
 *
 * **A tile rather than a stepper row, because a macro is entered and not nudged.** That was already
 * true of `MacroInputGroup`'s typable field; what the steppers beside it cost was the width, which
 * is why three rows stacked down a form no matter that each one carries a two-digit number. Three
 * tiles take one row's height and leave the value room to be read.
 *
 * **`null` prints [UNSET], and that is the whole reason this takes an `Int?`.** A blank form has
 * nobody's opinion on how much fat is in the thing being logged, and a cell reading `0` there is a
 * claim the app cannot make. Clearing the field returns to `null` rather than to `0`, so the
 * distinction survives a correction. A figure that genuinely *is* zero is typed as one and prints
 * as one. Stored entries keep saying `0` for both — `Nutrients` explains why — so this only ever
 * shows on a form somebody is still filling in.
 *
 * [dotColor] carries the fixed macro mapping (protein = `primary`, carbs = `tertiary`,
 * fat = `secondary`); pass null for the micronutrients, which appear in no bar and so may not
 * borrow a colour that would claim they did.
 */
@Composable
fun MacroFieldCell(
    label: String,
    value: Int?,
    unit: String,
    onValueChange: (Int?) -> Unit,
    modifier: Modifier = Modifier,
    dotColor: Color? = null,
) {
    val focusRequester = remember { FocusRequester() }
    val editLabel = stringResource(R.string.ds_macro_field, label, unit)
    Column(
        modifier = modifier
            .heightIn(min = 72.dp)
            .clip(CellShape)
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CellShape)
            // The whole tile is the target, which is what lets the field stay the width of its own
            // digits. Before the padding, so the ripple covers the border rather than insetting.
            .clickable(onClickLabel = editLabel) { focusRequester.requestFocus() }
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (dotColor != null) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(dotColor),
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = if (dotColor != null) 4.dp else 0.dp),
            )
        }
        Row(verticalAlignment = Alignment.Bottom) {
            // `IntrinsicSize.Min` is load-bearing, not layout garnish. `BasicTextField` fills
            // whatever width it is handed, so left alone it ate the whole row: the unit was then
            // measured against zero and drew *past* the tile's padding onto the border, which no
            // amount of end padding could pull back inside a box it was already outside of. Giving
            // the field a weight instead fixed the overflow and broke the reading — "32" at one end
            // of the tile and "g" at the other are not one figure. So the field is the width of its
            // own digits and the unit sits against it, with the tile carrying the tap target the
            // field was filling for. The floor keeps an empty field tappable and its caret visible.
            Box(modifier = Modifier.width(IntrinsicSize.Min)) {
                // Behind the field rather than inside it: the field holds its own text so a
                // backspace to empty stays empty, and the dash is exactly what "empty" looks like.
                if (value == null) {
                    Text(
                        text = UNSET,
                        style = ValueStyle(),
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
                StepperValueField(
                    value = value?.toString().orEmpty(),
                    // A cleared field is nobody's opinion again, not a zero.
                    onValueChange = { onValueChange(it.toIntOrNull()) },
                    contentDescription = editLabel,
                    textStyle = ValueStyle(),
                    modifier = Modifier
                        .defaultMinSize(minWidth = 24.dp)
                        .focusRequester(focusRequester),
                )
            }
            Text(
                text = unit,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, bottom = 4.dp),
            )
        }
    }
}

/** The value's one type style, shared by the field and the dash standing in for it so the two
 * occupy the same box. */
@Composable
private fun ValueStyle() =
    MaterialTheme.typography.titleLarge.tabularNums.copy(fontWeight = FontWeight.SemiBold)

/**
 * Protein, carbs and fat as three [MacroFieldCell]s in a row — the food-logging replacement for
 * [MacroInputGroup], which stays for the two callers whose macros are a *target* rather than a
 * meal's contents (onboarding's confirm-targets step and Profile's calorie section). A target is
 * nudged toward a split, which is what steppers are for; a food's macros are copied off a label.
 */
@Composable
fun MacroFieldGroup(
    proteinG: Int?,
    carbsG: Int?,
    fatG: Int?,
    onProteinChange: (Int?) -> Unit,
    onCarbsChange: (Int?) -> Unit,
    onFatChange: (Int?) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Unit symbols are not copy — g, mg, kcal read the same in every language.
    val grams = "g"
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        MacroFieldCell(
            label = stringResource(R.string.ds_macro_protein),
            value = proteinG,
            unit = grams,
            onValueChange = onProteinChange,
            dotColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f),
        )
        MacroFieldCell(
            label = stringResource(R.string.ds_macro_carbs),
            value = carbsG,
            unit = grams,
            onValueChange = onCarbsChange,
            dotColor = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.weight(1f),
        )
        MacroFieldCell(
            label = stringResource(R.string.ds_macro_fat),
            value = fatG,
            unit = grams,
            onValueChange = onFatChange,
            dotColor = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.weight(1f),
        )
    }
}

@PreviewLightDark
@Composable
private fun MacroFieldGroupPreview() {
    AppTheme {
        Surface {
            MacroFieldGroup(
                proteinG = 32,
                carbsG = 48,
                fatG = 14,
                onProteinChange = {},
                onCarbsChange = {},
                onFatChange = {},
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

/** The blank form: nobody has said, and three zeroes would be three claims. */
@PreviewLightDark
@Composable
private fun MacroFieldGroupUnsetPreview() {
    AppTheme {
        Surface {
            MacroFieldGroup(
                proteinG = null,
                carbsG = null,
                fatG = null,
                onProteinChange = {},
                onCarbsChange = {},
                onFatChange = {},
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

/** A fat-free food: a typed zero is a figure, and prints as one. */
@PreviewLightDark
@Composable
private fun MacroFieldGroupZeroPreview() {
    AppTheme {
        Surface {
            MacroFieldGroup(
                proteinG = 0,
                carbsG = 21,
                fatG = 0,
                onProteinChange = {},
                onCarbsChange = {},
                onFatChange = {},
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}
