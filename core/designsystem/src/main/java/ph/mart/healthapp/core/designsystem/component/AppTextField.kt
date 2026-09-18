package ph.mart.healthapp.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.designsystem.theme.AppTheme

/**
 * 12dp-corner text field, [MaterialTheme.colorScheme.outline] border, transparent fill.
 *
 * [imeAction] and [onImeAction] are the keyboard's own action key — the shape `RulerPickerField`
 * already uses. A field that leaves them alone keeps the return key that does nothing, which is
 * right for a field in a form; a field that *is* the whole gesture (the coach's chat bar) says
 * `ImeAction.Send` and hands it the same lambda its button calls. [onImeAction] null is what makes
 * the key inert while the action is unavailable, so the keyboard can never do what the button
 * refuses to.
 *
 * [maxLines] above 1 lets the text wrap and the box grow with it — talk-to-log's sentence field,
 * which is the one field in the app whose content is a sentence rather than a value. It is a
 * *parameter*, not a second component, and it is the only thing this field has ever grown: what
 * `HistorySearchField` was kept out of here (a pill radius, a magnifier, a clear button, a progress
 * line) were all features that would have landed in every form in the product, and letting text
 * wrap lands nothing in a caller that doesn't ask. The height is a floor rather than a fixture as
 * of that change, so a one-line field is still exactly 48dp and no longer clips its own text at the
 * largest font scales.
 *
 * [shape], [color] and [border] are the third, and they default to exactly the chrome above — the
 * move [AppCard] already made for the diary's section cards, for the same reason: one caller wants
 * a different container and every other one is unchanged by a defaulted parameter. The coach's
 * composer is that caller, and it is a *field that is a control* rather than a field in a form — a
 * 24dp pill filled with `surfaceContainerHighest` and no border at all, sitting beside its own send
 * circle. A second text-field component would be a second set of focus, IME and trailing-slot rules
 * to keep in step.
 *
 * [trailing] is the second, and it earns its place the same way: a 48dp slot at the end of the box,
 * laid out only when a caller passes one, top-aligned so it stays put as a wrapping field grows.
 * Talk-to-log's Clear is the one user — it lived in a row beneath the field, where it and the mic
 * were two identical grey glyphs doing different jobs, and inside the box it is unmistakably about
 * the text. The slot reserves its width, so a glyph that appears with the first keystroke never
 * reflows the sentence being typed.
 */
@Composable
fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    error: String? = null,
    imeAction: ImeAction = ImeAction.Default,
    onImeAction: (() -> Unit)? = null,
    maxLines: Int = 1,
    trailing: @Composable (() -> Unit)? = null,
    shape: Shape = RoundedCornerShape(12.dp),
    color: Color = Color.Transparent,
    border: BorderStroke? = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
) {
    Column(modifier = modifier) {
        if (label != null) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                // A floor, not a fixture: one line of bodyLarge plus the padding below is exactly
                // 48dp, so a form field is the height it always was and a wrapping one grows.
                .heightIn(min = 48.dp)
                .clip(shape)
                .background(color)
                // An error outlines the field whatever its usual chrome is — including a field that
                // normally has no border at all, which is the one state a fill cannot say by itself.
                .let { base ->
                    val stroke = when {
                        error != null -> BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                        else -> border
                    }
                    if (stroke == null) base else base.border(stroke, shape)
                }
                .padding(start = 16.dp, end = if (trailing != null) 0.dp else 16.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            Row(verticalAlignment = Alignment.Top, modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    FieldContent(
                        value = value,
                        onValueChange = onValueChange,
                        label = label,
                        placeholder = placeholder,
                        imeAction = imeAction,
                        onImeAction = onImeAction,
                        maxLines = maxLines,
                    )
                }
                if (trailing != null) {
                    Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) { trailing() }
                }
            }
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

/** The placeholder and the field itself, split out only so the box above stays readable once the
 * trailing slot sits beside them. */
@Composable
private fun FieldContent(
    value: String,
    onValueChange: (String) -> Unit,
    label: String?,
    placeholder: String?,
    imeAction: ImeAction,
    onImeAction: (() -> Unit)?,
    maxLines: Int,
) {
    Box(contentAlignment = Alignment.CenterStart) {
        if (value.isEmpty() && placeholder != null) {
            Text(
                text = placeholder,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            // Not `singleLine = true` beside a maxLines above 1 — BasicTextField rejects the pair.
            singleLine = maxLines == 1,
            maxLines = maxLines,
            keyboardOptions = KeyboardOptions(imeAction = imeAction),
            // One handler for all of them: the key the IME shows is `imeAction`'s, so whichever
            // callback fires is the one the caller asked for.
            keyboardActions = KeyboardActions(
                onSend = { onImeAction?.invoke() },
                onDone = { onImeAction?.invoke() },
                onSearch = { onImeAction?.invoke() },
                onGo = { onImeAction?.invoke() },
            ),
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            // The label and placeholder are siblings, not part of the field, so without this a
            // screen reader announces every field in the app as a bare edit box. The
            // placeholder stands in where a field has no visible label — the diary's filter
            // and the food search both rely on it.
            modifier = Modifier
                .fillMaxWidth()
                .semantics {
                    val name = label ?: placeholder
                    if (name != null) contentDescription = name
                },
        )
    }
}

@PreviewLightDark
@Composable
private fun AppTextFieldPreview() {
    AppTheme {
        Surface {
            Column(modifier = Modifier.padding(16.dp)) {
                AppTextField(value = "", onValueChange = {}, placeholder = "Search foods…")
                AppTextField(
                    value = "two scrambled eggs, a slice of toast and a black coffee",
                    onValueChange = {},
                    maxLines = 4,
                )
            }
        }
    }
}
