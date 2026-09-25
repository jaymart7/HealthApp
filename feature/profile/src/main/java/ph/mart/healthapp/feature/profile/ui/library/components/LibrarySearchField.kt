package ph.mart.healthapp.feature.profile.ui.library.components

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.designsystem.icon.AppIcons
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.profile.R

private val FieldShape = RoundedCornerShape(24.dp)

/**
 * The library's search, opened from the icon at the end of the chip row and drawn in the chips'
 * place — the list is short enough to scan now that it is one list with filters, so search is
 * the occasional intent and does not earn a permanent row. It opens focused, and its ✕ closes it,
 * query and all; back does the same.
 *
 * Not [ph.mart.healthapp.core.designsystem.component.AppTextField], for the reason
 * `HistorySearchField` gives one flow over: that component is the app's *form* field — bordered,
 * square-ish, drawn in every sheet in the product — and growing a pill radius and a pair of icons
 * onto it to serve one screen would push both into every form.
 *
 * It lives in this flow's `components/` because one screen draws it. The day a second one wants
 * the same box, the two merge in `:core:designsystem` — not before.
 */
@Composable
internal fun LibrarySearchField(
    value: String,
    onValueChange: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Drawn rather than inferred: BasicTextField has no container of its own, so without this
    // nothing on screen would say the keyboard is pointed here.
    var focused by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val inspection = LocalInspectionMode.current
    LaunchedEffect(Unit) { if (!inspection) focusRequester.requestFocus() }
    val placeholder = stringResource(R.string.profile_library_search)
    val closeLabel = stringResource(R.string.profile_library_search_close)
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = FieldShape,
        modifier = modifier.fillMaxWidth().height(48.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .then(
                    if (focused) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, FieldShape) else Modifier,
                )
                .padding(start = 12.dp),
        ) {
            Icon(
                imageVector = AppIcons.Search,
                // Decorative: the field beside it announces itself by its placeholder.
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
            Box(
                contentAlignment = Alignment.CenterStart,
                modifier = Modifier.weight(1f).padding(start = 8.dp),
            ) {
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    // Every result is already on screen by the time this key can be pressed —
                    // the filter runs on the keystroke. It exists so the key isn't a dead one.
                    keyboardActions = KeyboardActions(onSearch = {}),
                    textStyle = MaterialTheme.typography.bodyMedium
                        .copy(color = MaterialTheme.colorScheme.onSurface),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                        .onFocusChanged { focused = it.isFocused }
                        .semantics { contentDescription = placeholder },
                )
            }
            IconButton(onClick = onClose, modifier = Modifier.size(48.dp)) {
                Icon(
                    imageVector = AppIcons.Close,
                    contentDescription = closeLabel,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun LibrarySearchFieldPreview() {
    AppTheme {
        Surface {
            Column(modifier = Modifier.padding(16.dp)) {
                LibrarySearchField(value = "", onValueChange = {}, onClose = {})
                LibrarySearchField(
                    value = "oat",
                    onValueChange = {},
                    onClose = {},
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
        }
    }
}
