package ph.mart.healthapp.core.designsystem.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import ph.mart.healthapp.core.designsystem.R
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.core.designsystem.theme.tabularNums

/** One value unit of scale. Every geometry figure here is dp and deliberately does **not** scale
 * with the font: a scale whose ticks grew with the text would stop mapping [UnitSpacing] to one
 * unit, and the caret would stop pointing at the number under it. */
private val UnitSpacing = 11.dp
private val ScaleHeight = 44.dp
private val MinorTick = Size(1.5f, 10f)
private val MajorTick = Size(2f, 18f)
private val Caret = Size(3f, 28f)

/** A numeral every five units — the reading grid. */
private const val MAJOR_EVERY = 5

/** How far past either end the scale can be dragged before it refuses. Resistance inside this band
 * is what tells a thumb it has reached 100 years old, rather than that the drag broke. */
private val RubberBand = 12.dp

private const val EM_DASH = "—"

/**
 * A value on a draggable tick scale: one large tabular number with a fixed
 * [MaterialTheme.colorScheme.primary] caret beneath it, and the scale translating under the caret.
 *
 * [NumericStepperField] is the sibling, not the predecessor — a stepper is one tap per unit, so it
 * is right wherever the expected change is a unit or two (±50 kcal, ±5 g) and wrong wherever a
 * value is *chosen* out of a range. Setting 78 kg from a 65 kg default costs 26 taps there and one
 * gesture here. Tapping the number still opens the numeric IME, so this is also the typed path
 * without a second control in the layout.
 *
 * [range] and [step] are in whatever unit the caller is displaying — the field never converts, so
 * a metric/imperial toggle hands it a different range and a different [unit] and nothing here
 * knows the difference. A null [value] is unset: "—", a hint in place of the unit, and the scale
 * drawn in [MaterialTheme.colorScheme.outlineVariant] so the gesture still advertises itself.
 * First contact sets the midpoint rather than whatever tick the finger landed on, which would make
 * where you happened to touch look like a choice.
 */
@Composable
fun RulerPickerField(
    label: String,
    value: Double?,
    range: ClosedFloatingPointRange<Double>,
    step: Double,
    unit: String,
    onValueChange: (Double) -> Unit,
    modifier: Modifier = Modifier,
    decimals: Int = 0,
    trailing: (@Composable () -> Unit)? = null,
) {
    val density = LocalDensity.current
    val pxPerUnit = with(density) { UnitSpacing.toPx() }
    val rubberBandPx = with(density) { RubberBand.toPx() }
    val maxOffsetPx = (range.endInclusive - range.start).toFloat() * pxPerUnit
    val scope = rememberCoroutineScope()
    var focused by remember { mutableStateOf(false) }

    // Distance from the range's start in px, and the single source of truth while a drag runs:
    // `value` only catches up on each settled step, so driving the scale off it would quantise the
    // translation to the snap increment and the drag would read as a stutter.
    val offset = remember { Animatable(0f) }
    val shown = value ?: midpoint(range, step)
    LaunchedEffect(shown, pxPerUnit) {
        val target = ((shown - range.start) * pxPerUnit).toFloat()
        if (abs(offset.value - target) > pxPerUnit / 4f) offset.snapTo(target)
    }

    fun nudge(steps: Int) =
        onValueChange(snapToStep((value ?: midpoint(range, step)) + steps * step, range, step))

    val hint = stringResource(R.string.ds_ruler_hint)
    val increase = stringResource(R.string.ds_increase, label)
    val decrease = stringResource(R.string.ds_decrease, label)
    val readout = if (value == null) hint else "${format(value, decimals)} $unit"

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .then(
                if (focused) {
                    Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp))
                } else {
                    Modifier
                },
            )
            .padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 12.dp)
            // One node, so TalkBack reads a slider rather than a label, a number and a drawing.
            // `setProgress` is what its swipe-to-adjust drives; the two custom actions are the
            // single-unit moves a keyboard and Switch Access need.
            .semantics(mergeDescendants = true) {
                contentDescription = "$label, $readout"
                progressBarRangeInfo = ProgressBarRangeInfo(
                    current = (value ?: range.start).toFloat(),
                    range = range.start.toFloat()..range.endInclusive.toFloat(),
                    steps = (((range.endInclusive - range.start) / step).roundToInt() - 1).coerceAtLeast(0),
                )
                setProgress { target ->
                    onValueChange(snapToStep(target.toDouble(), range, step))
                    true
                }
                customActions = listOf(
                    CustomAccessibilityAction(increase) { nudge(1); true },
                    CustomAccessibilityAction(decrease) { nudge(-1); true },
                )
            },
    ) {
        ValueRow(
            label = label,
            value = value,
            unit = unit,
            hint = hint,
            decimals = decimals,
            focused = focused,
            onOpenKeyboard = { focused = true },
            onTyped = { typed ->
                typed.toDoubleOrNull()?.let { onValueChange(snapToStep(it, range, step)) }
            },
            onDone = { focused = false },
        )
        if (focused) {
            FocusedFooter(
                rangeText = stringResource(
                    R.string.ds_ruler_range,
                    format(range.start, decimals),
                    format(range.endInclusive, decimals),
                    unit,
                ),
                onDone = { focused = false },
            )
        } else {
            RulerScale(
                offsetPx = offset.value,
                pxPerUnit = pxPerUnit,
                range = range,
                set = value != null,
                trailing = trailing,
                modifier = Modifier
                    .draggable(
                        orientation = Orientation.Horizontal,
                        state = rememberDraggableState { delta ->
                            scope.launch {
                                val next = offset.value - delta
                                offset.snapTo(resist(next, maxOffsetPx, rubberBandPx))
                                onValueChange(
                                    snapToStep(
                                        range.start + next.coerceIn(0f, maxOffsetPx) / pxPerUnit,
                                        range,
                                        step,
                                    ),
                                )
                            }
                        },
                        onDragStopped = { velocity ->
                            if (offset.value in 0f..maxOffsetPx) {
                                offset.animateDecay(-velocity, exponentialDecay())
                            }
                            val settled = snapToStep(range.start + offset.value / pxPerUnit, range, step)
                            offset.animateTo(((settled - range.start) * pxPerUnit).toFloat())
                            onValueChange(settled)
                        },
                    )
                    // An unset field has no value to drag from, so the first touch anywhere on the
                    // scale is what seeds one.
                    .pointerInput(value == null) {
                        if (value == null) {
                            awaitPointerEventScope {
                                awaitPointerEvent()
                                onValueChange(midpoint(range, step))
                            }
                        }
                    },
            )
        }
    }
}

/** Label start, value + unit end. The whole number is a 48dp tap target that raises the IME, which
 * is why the row has a minimum height rather than taking the text's own. */
@Composable
private fun ValueRow(
    label: String,
    value: Double?,
    unit: String,
    hint: String,
    decimals: Int,
    focused: Boolean,
    onOpenKeyboard: () -> Unit,
    onTyped: (String) -> Unit,
    onDone: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(focused) { if (focused) focusRequester.requestFocus() }
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = if (focused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        val valueStyle = MaterialTheme.typography.headlineSmall.tabularNums
        if (focused) {
            var text by remember { mutableStateOf(value?.let { format(it, decimals) }.orEmpty()) }
            BasicTextField(
                value = text,
                onValueChange = { raw ->
                    text = raw.keepDigits(decimals > 0)
                    onTyped(text)
                },
                singleLine = true,
                textStyle = valueStyle.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.End,
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                keyboardOptions = KeyboardOptions(
                    keyboardType = if (decimals > 0) KeyboardType.Decimal else KeyboardType.Number,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { onDone() }),
                modifier = Modifier.weight(1f).focusRequester(focusRequester),
            )
        } else {
            Text(
                text = value?.let { format(it, decimals) } ?: EM_DASH,
                style = valueStyle,
                color = if (value == null) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onOpenKeyboard)
                    .padding(horizontal = 8.dp),
            )
        }
        Text(
            text = if (value == null && !focused) hint else unit,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** What the 44dp scale collapses to while the IME is up: the range it will accept, and the way
 * out. No dialog and no layout jump — the field keeps its place in the column. */
@Composable
private fun FocusedFooter(rangeText: String, onDone: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().height(24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = rangeText,
            style = MaterialTheme.typography.labelSmall.tabularNums,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        TextButton(label = stringResource(R.string.ds_ruler_done), onClick = onDone)
    }
}

/** The ticks, the numerals and the caret. Everything reads from [offsetPx], so the scale is a pure
 * function of how far the drag has travelled — which is what keeps the caret nailed to the centre
 * while the ruler moves underneath it. */
@Composable
private fun RulerScale(
    offsetPx: Float,
    pxPerUnit: Float,
    range: ClosedFloatingPointRange<Double>,
    set: Boolean,
    trailing: (@Composable () -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val measurer = rememberTextMeasurer()
    val minorColor = MaterialTheme.colorScheme.outlineVariant
    val majorColor = if (set) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.outlineVariant
    val caretColor = if (set) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
    val numeralColor = majorColor
    val numeralStyle = MaterialTheme.typography.labelSmall.tabularNums.copy(fontSize = 10.sp)
    val caretNumeralStyle = numeralStyle.copy(color = caretColor, fontWeight = FontWeight.SemiBold)

    Box(modifier = modifier.fillMaxWidth().height(ScaleHeight)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerX = size.width / 2f
            val units = (range.endInclusive - range.start).roundToInt()
            val first = floor((offsetPx - centerX) / pxPerUnit).toInt().coerceAtLeast(0)
            val last = ceil((offsetPx + centerX) / pxPerUnit).toInt().coerceAtMost(units)
            val caretUnit = (offsetPx / pxPerUnit).roundToInt()
            for (i in first..last) {
                val x = centerX + i * pxPerUnit - offsetPx
                val major = i % MAJOR_EVERY == 0
                drawTick(
                    x = x,
                    spec = if (major) MajorTick else MinorTick,
                    color = if (major) majorColor else minorColor,
                )
                if (major) {
                    val numeral = (range.start + i).roundToInt().toString()
                    val style = if (i == caretUnit) caretNumeralStyle else numeralStyle.copy(color = numeralColor)
                    val laid = measurer.measure(numeral, style)
                    drawText(
                        textLayoutResult = laid,
                        topLeft = Offset(x - laid.size.width / 2f, MajorTick.height.dp.toPx() + 4.dp.toPx()),
                    )
                }
            }
            drawRoundRect(
                color = caretColor,
                topLeft = Offset(centerX - Caret.width.dp.toPx() / 2f, 0f),
                size = Size(Caret.width.dp.toPx(), Caret.height.dp.toPx()),
                cornerRadius = CornerRadius(2.dp.toPx()),
            )
        }
        if (trailing != null) Box(modifier = Modifier.align(Alignment.TopEnd)) { trailing() }
    }
}

private fun DrawScope.drawTick(x: Float, spec: Size, color: Color) {
    drawRoundRect(
        color = color,
        topLeft = Offset(x - spec.width.dp.toPx() / 2f, 0f),
        size = Size(spec.width.dp.toPx(), spec.height.dp.toPx()),
        cornerRadius = CornerRadius(spec.width.dp.toPx() / 2f),
    )
}

internal fun format(value: Double, decimals: Int): String =
    if (decimals == 0) value.roundToInt().toString() else "%.${decimals}f".format(value)

/** The default an unset field takes on first contact. Snapped, so the very first value a user sees
 * is one the scale can return to. */
internal fun midpoint(range: ClosedFloatingPointRange<Double>, step: Double): Double =
    snapToStep((range.start + range.endInclusive) / 2, range, step)

/** Nearest multiple of [step] measured from the range's start, clamped to the range. Measuring
 * from the start rather than from zero is what keeps a 0.5 kg scale landing on 65.0 and 65.5
 * whatever the range happens to begin at. */
internal fun snapToStep(value: Double, range: ClosedFloatingPointRange<Double>, step: Double): Double {
    val steps = ((value - range.start) / step).roundToInt()
    return (range.start + steps * step).coerceIn(range.start, range.endInclusive)
}

/** Drag past either end with diminishing returns rather than into a wall: the first pixel still
 * moves, the last barely does, and nothing beyond [limit] is reachable. */
internal fun resist(offsetPx: Float, maxPx: Float, limit: Float): Float = when {
    offsetPx < 0f -> -limit * asymptote(-offsetPx / limit)
    offsetPx > maxPx -> maxPx + limit * asymptote((offsetPx - maxPx) / limit)
    else -> offsetPx
}

/** An asymptote at 1: however hard the drag pushes, the band never exceeds its own width. */
private fun asymptote(ratio: Float): Float = ratio / (1f + ratio)

@PreviewLightDark
@Composable
private fun RulerPickerFieldPreview() {
    AppTheme {
        Surface {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(16.dp),
            ) {
                RulerPickerField(
                    label = "Age",
                    value = 25.0,
                    range = 13.0..100.0,
                    step = 1.0,
                    unit = "years",
                    onValueChange = {},
                )
                RulerPickerField(
                    label = "Current weight",
                    value = 65.0,
                    range = 30.0..250.0,
                    step = 0.5,
                    unit = "kg",
                    decimals = 1,
                    onValueChange = {},
                )
            }
        }
    }
}

/** The state every field opens in before anything is set. The scale is still drawn, because that
 * is what says the gesture exists, and nothing is red: an untouched form is not an error. */
@PreviewLightDark
@Composable
private fun RulerPickerFieldUnsetPreview() {
    AppTheme {
        Surface {
            RulerPickerField(
                label = "Height",
                value = null,
                range = 120.0..220.0,
                step = 1.0,
                unit = "cm",
                onValueChange = {},
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}
