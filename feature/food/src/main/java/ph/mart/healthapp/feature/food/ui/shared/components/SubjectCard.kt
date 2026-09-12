package ph.mart.healthapp.feature.food.ui.shared.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.designsystem.icon.AppIcons
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.core.designsystem.theme.tabularNums
import ph.mart.healthapp.feature.food.R
import ph.mart.healthapp.feature.food.ui.shared.AddEntryForm
import ph.mart.healthapp.feature.food.ui.shared.withPortionAmount

private val CardShape = RoundedCornerShape(24.dp)

/**
 * The thing being reviewed: what it is, how much of it, and what that comes to. Nothing else.
 *
 * The review screen used to be nine input boxes of equal weight, which made checking a barcode
 * match a reading exercise — the name, the portion and the calories are what somebody actually
 * verifies, and they sat in the same grey rows as the sodium. Pulling exactly those three into the
 * screen's **only** card is what demotes everything below it to corrections, without hiding
 * anything: the macros are a tap away in the same scroll, the micronutrients one tap further.
 *
 * It is a card and not a section rule because it is the subject, and a subject wants an edge.
 */
@Composable
internal fun SubjectCard(
    form: AddEntryForm,
    manualEntry: Boolean,
    onFormChange: (AddEntryForm) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CardShape)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        InlineTitleField(
            name = form.name,
            onNameChange = { onFormChange(form.copy(name = it)) },
        )
        PortionControl(
            amount = form.portionAmount,
            unit = form.portionUnit,
            manualEntry = manualEntry,
            // Repricing is the point: moving the amount without moving the values writes a
            // 100 g figure against 30 g of food, in the direction that inflates the day.
            onAmountChange = { onFormChange(form.withPortionAmount(it)) },
            onUnitChange = { onFormChange(form.copy(portionUnit = it)) },
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        CaloriesRow(
            calories = form.calories,
            onCaloriesChange = { onFormChange(form.copy(calories = it)) },
        )
    }
}

/**
 * The food's name as the card's heading, editable in place.
 *
 * A labelled "Food" text box said the name was a field like the others. It is the title of the
 * thing on screen, so it is typeset as one — and the underline plus the pencil are what still say
 * it can be changed, which a bare heading would not.
 */
@Composable
private fun InlineTitleField(name: String, onNameChange: (String) -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
            Box(modifier = Modifier.weight(1f)) {
                if (name.isEmpty()) {
                    Text(
                        text = stringResource(R.string.food_name_placeholder),
                        style = TitleStyle(),
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
                BasicTextField(
                    value = name,
                    onValueChange = onNameChange,
                    singleLine = true,
                    textStyle = TitleStyle().copy(color = MaterialTheme.colorScheme.onSurface),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { focused = it.isFocused },
                )
            }
            Icon(
                imageVector = AppIcons.Edit,
                // The field beside it is already the control; this only says the title is one.
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
        // A rule, not a box: the title is a heading that happens to be editable, and a full
        // outline would make it a text field again.
        HorizontalDivider(
            thickness = if (focused) 2.dp else 1.dp,
            color = if (focused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
        )
    }
}

/**
 * What the portion comes to.
 *
 * It *follows* the portion — `withPortionAmount` reprices it, and every macro and nutrient with it —
 * and the hint says so, because a figure that changes when you touch something else needs to
 * explain itself once. Typing over it is a correction and the correction survives: the rescale
 * works off the current pair, so a value fixed at 100 g is still right at 150 g rather than being
 * detached and quietly left behind.
 *
 * Null prints a dash. A form the user is filling in has no calorie figure until they supply one,
 * and `0` there would be a claim; the Log button is what tells them it is still needed, since a
 * named entry with no calories is a valid quick add and not an error.
 */
@Composable
private fun CaloriesRow(calories: Int?, onCaloriesChange: (Int?) -> Unit) {
    val focusRequester = remember { FocusRequester() }
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            CardLabel(stringResource(R.string.food_calories_label))
            Row(verticalAlignment = Alignment.Bottom) {
                // Sized to its digits for the reason `MacroFieldCell`'s is: left to fill, the field
                // takes the whole row and "kcal" measures against zero, drawing over the edit button
                // rather than beside the number. The button is already this row's large tap target,
                // so the field gives up nothing by being the width of what it holds.
                Box(modifier = Modifier.width(IntrinsicSize.Min)) {
                    if (calories == null) {
                        Text(
                            text = "—",
                            style = MaterialTheme.typography.displaySmall.tabularNums,
                            color = MaterialTheme.colorScheme.outline,
                        )
                    }
                    BasicTextField(
                        value = calories?.toString().orEmpty(),
                        onValueChange = { raw ->
                            onCaloriesChange(raw.filter { it.isDigit() }.take(6).toIntOrNull())
                        },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.displaySmall.tabularNums
                            .copy(color = MaterialTheme.colorScheme.onSurface),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done,
                        ),
                        modifier = Modifier
                            .defaultMinSize(minWidth = 32.dp)
                            .focusRequester(focusRequester),
                    )
                }
                Text(
                    // Not copy.
                    text = "kcal",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
                )
            }
            Text(
                text = stringResource(R.string.food_calories_follows_portion),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        // A 36sp number does not read as a text field, and nothing else in this card is tapped to
        // type. The button is the affordance; it hands focus to the field beside it.
        Surface(
            onClick = { focusRequester.requestFocus() },
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(48.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = AppIcons.Edit,
                    contentDescription = stringResource(R.string.food_calories_edit),
                    modifier = Modifier.size(22.dp),
                )
            }
        }
    }
}

@Composable
private fun TitleStyle() =
    MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold)

@PreviewLightDark
@Composable
private fun SubjectCardFoundPreview() {
    AppTheme {
        Surface {
            SubjectCard(
                form = AddEntryForm(
                    name = "Nutella",
                    portionAmount = 150.0,
                    portionUnit = "g",
                    calories = 809,
                ),
                manualEntry = false,
                onFormChange = {},
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun SubjectCardManualPreview() {
    AppTheme {
        Surface {
            SubjectCard(
                form = AddEntryForm(),
                manualEntry = true,
                onFormChange = {},
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}
