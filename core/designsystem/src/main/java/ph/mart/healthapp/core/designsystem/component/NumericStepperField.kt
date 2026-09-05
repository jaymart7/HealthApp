package ph.mart.healthapp.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.designsystem.R
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.core.designsystem.theme.tabularNums

/** Six digits covers every figure this app records (a day's calories, a body weight, a portion in
 * grams) and keeps the parse inside [Int] no matter what is pasted in. */
private const val MAX_VALUE_DIGITS = 6

/**
 * Value + unit + circular +/- steppers, tabular-nums, 48dp minimum height,
 * [MaterialTheme.colorScheme.surfaceContainerLow] bg.
 *
 * Pass [onValueChange] to make the value typable. The steppers are for nudging a figure that is
 * already about right; typing is how a figure gets *entered*, and without it a 320 kcal quick add
 * costs 32 taps. Callers that genuinely only nudge (a water goal, a servings count) leave it null
 * and keep the read-only value.
 */
@Composable
fun NumericStepperField(
    label: String,
    value: String,
    unitSuffix: String,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    modifier: Modifier = Modifier,
    error: String? = null,
    onValueChange: ((String) -> Unit)? = null,
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                // No vertical padding: the 48dp stepper target fills the row's full height rather
                // than being inset to 40dp by it. The visible circle stays 40dp either way.
                .heightIn(min = 48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                .padding(start = 16.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (onValueChange == null) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium.tabularNums,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
            } else {
                StepperValueField(
                    value = value,
                    onValueChange = onValueChange,
                    contentDescription = label,
                    modifier = Modifier.weight(1f),
                )
            }
            Text(
                text = unitSuffix,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            StepperButton(symbol = "−", label = stringResource(R.string.ds_decrease, label), onClick = onDecrement)
            StepperButton(symbol = "+", label = stringResource(R.string.ds_increase, label), onClick = onIncrement)
        }
        if (error != null) {
            Text(
                text = error,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

/**
 * The typable value inside a stepper row. Holds its own text so a backspace to empty stays empty
 * while the model already reads zero — binding a `String` straight to an `Int` would refill the
 * field with "0" the moment it was cleared and make the field impossible to retype.
 *
 * [decimal] admits one decimal point, for the portion amounts that are 0.5 of a cup rather than a
 * whole number of grams.
 */
@Composable
internal fun StepperValueField(
    value: String,
    onValueChange: (String) -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    decimal: Boolean = false,
    textAlign: TextAlign = TextAlign.Start,
) {
    var text by remember { mutableStateOf(value) }
    // Re-seed only when the incoming value is a different *number*: a stepper tap, a portion
    // rescale, or a freshly seeded form. Comparing numerically rather than by string leaves a
    // half-typed "1." and a deliberate "0.50" alone.
    if (text.asNumber() != value.asNumber()) text = value

    BasicTextField(
        value = text,
        onValueChange = { raw ->
            val cleaned = raw.keepDigits(decimal)
            text = cleaned
            onValueChange(cleaned)
        },
        singleLine = true,
        textStyle = MaterialTheme.typography.titleMedium.tabularNums
            .copy(color = MaterialTheme.colorScheme.onSurface, textAlign = textAlign),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        keyboardOptions = KeyboardOptions(
            keyboardType = if (decimal) KeyboardType.Decimal else KeyboardType.Number,
            imeAction = ImeAction.Done,
        ),
        modifier = modifier.semantics { this.contentDescription = contentDescription },
    )
}

/** An empty field is a zero, not a parse failure — that is what lets the field be cleared. */
private fun String.asNumber(): Double = if (isBlank()) 0.0 else toDoubleOrNull() ?: 0.0

/** Digits only (plus one decimal point when [decimal]), capped, with the placeholder zero dropped
 * as soon as a real digit lands — a field showing "0" that a user types 320 into should read 320,
 * not 0320. */
internal fun String.keepDigits(decimal: Boolean): String {
    val allowed = filter { it.isDigit() || (decimal && it == '.') }
    if (!decimal || !allowed.contains('.')) return allowed.take(MAX_VALUE_DIGITS).trimLeadingZeros()
    val whole = allowed.substringBefore('.').take(MAX_VALUE_DIGITS).trimLeadingZeros()
    val fraction = allowed.substringAfter('.').filter { it.isDigit() }.take(2)
    return "$whole.$fraction"
}

/** "0320" is 320; "0" and "" are themselves — a cleared field has to stay cleared. */
private fun String.trimLeadingZeros(): String = trimStart('0').ifEmpty { if (isEmpty()) "" else "0" }

/**
 * Shared circular +/- button reused by [NumericStepperField], [MacroInputGroup] and [FoodItemRow].
 *
 * [label] is what a screen reader announces: three identical "minus, plus" pairs down a macro group
 * tell a TalkBack user nothing about which row they are on. 48dp because that is the platform
 * minimum for anything tappable; the visible circle stays smaller than the target it carries.
 */
@Composable
internal fun StepperButton(symbol: String, label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier.size(48.dp),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            onClick = onClick,
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .size(40.dp)
                .semantics { contentDescription = label },
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
                Text(text = symbol, style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun NumericStepperFieldPreview() {
    AppTheme {
        Surface {
            NumericStepperField(
                label = "Age",
                value = "28",
                unitSuffix = "years",
                onIncrement = {},
                onDecrement = {},
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

/** The typable variant — the one every calorie and macro figure in the food feature uses. */
@PreviewLightDark
@Composable
private fun NumericStepperFieldEditablePreview() {
    AppTheme {
        Surface {
            NumericStepperField(
                label = "Calories",
                value = "210",
                unitSuffix = "kcal",
                onIncrement = {},
                onDecrement = {},
                onValueChange = {},
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}
