package ph.mart.healthapp.feature.food.ui.history.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.designsystem.icon.AppIcons
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.food.R

/**
 * The history search's own field: a 56dp pill with the magnifier inside it, a clear button once
 * there is something to clear, and the in-flight line under it.
 *
 * Not [AppTextField][ph.mart.healthapp.core.designsystem.component.AppTextField], and not a set of
 * new parameters on it. That component is the app's *form* field — bordered, 48dp, square-ish, and
 * drawn in every sheet in the product; growing icons, a pill radius and a progress line onto it to
 * serve one screen would push all of that into every form. This one is a search box, which is a
 * different thing that happens to also take text.
 *
 * It lives in this flow's `components/` because one screen draws it. The day `FoodSearchPanel`
 * wants the same box, it moves to `:core:designsystem` — not before, which is the rule for every
 * component in this app.
 */
@Composable
internal fun HistorySearchField(
    value: String,
    onValueChange: (String) -> Unit,
    /** Draws the indeterminate line beneath the field. The list above it stays where it is. */
    searching: Boolean,
    modifier: Modifier = Modifier,
) {
    // The focus ring is drawn rather than inferred: BasicTextField has no container of its own, so
    // nothing else on screen would say the keyboard is pointed here.
    var focused by remember { mutableStateOf(false) }
    val placeholder = stringResource(R.string.food_history_placeholder)
    val clearLabel = stringResource(R.string.food_history_clear)
    Column(modifier = modifier.fillMaxWidth()) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier.fillMaxWidth().height(56.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .then(
                        if (focused) {
                            Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(28.dp))
                        } else {
                            Modifier
                        },
                    )
                    .padding(horizontal = 4.dp),
            ) {
                Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = AppIcons.Search,
                        // Decorative: the field below announces itself by its placeholder.
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp),
                    )
                }
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.outline,
                        )
                    }
                    BasicTextField(
                        value = value,
                        onValueChange = onValueChange,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        // The results are already on screen by the time this key can be pressed —
                        // every keystroke ran a read. It exists so the key isn't a dead one.
                        keyboardActions = KeyboardActions(onSearch = {}),
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged { focused = it.isFocused }
                            .semantics { contentDescription = placeholder },
                    )
                }
                if (value.isNotEmpty()) {
                    IconButton(onClick = { onValueChange("") }) {
                        Icon(
                            imageVector = AppIcons.Close,
                            contentDescription = clearLabel,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
            }
        }
        if (searching) {
            LinearProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 24.dp, top = 8.dp)
                    .height(2.dp),
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun HistorySearchFieldPreview() {
    AppTheme {
        Surface {
            Column(modifier = Modifier.padding(16.dp)) {
                HistorySearchField(value = "", onValueChange = {}, searching = false)
                HistorySearchField(value = "chick", onValueChange = {}, searching = true)
            }
        }
    }
}
