package ph.mart.healthapp.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
                .height(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .border(
                    BorderStroke(
                        1.dp,
                        if (error != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline,
                    ),
                    RoundedCornerShape(12.dp),
                )
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
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
                singleLine = true,
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
        if (error != null) {
            Text(
                text = error,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun AppTextFieldPreview() {
    AppTheme {
        Surface {
            Column(modifier = Modifier.padding(16.dp)) {
                AppTextField(value = "", onValueChange = {}, placeholder = "Search foods…")
            }
        }
    }
}
