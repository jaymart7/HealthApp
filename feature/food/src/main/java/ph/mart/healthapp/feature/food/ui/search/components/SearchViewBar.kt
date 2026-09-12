package ph.mart.healthapp.feature.food.ui.search.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.designsystem.icon.AppIcons
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.food.R

private val BarShape = RoundedCornerShape(28.dp)

/**
 * The whole top of the search screen: back out, type, clear.
 *
 * A bar rather than [AppTopBar][ph.mart.healthapp.core.designsystem.component.AppTopBar] plus a
 * field beneath it, because this screen has no title to print. The thing a title would name is the
 * thing the field is for, and the back arrow living *inside* the bar is what says so — one row that
 * both leaves and searches, the shape the platform's own search surfaces use.
 *
 * The arrow is the screen's only exit and it replaces the Cancel button that used to sit at the
 * bottom: a text button under a full-height list is a long way from the finger that is already at
 * the top of the screen typing.
 *
 * [searching] draws the 2dp rule under the bar — the one piece of the online tier's status that is
 * *not* in the list's tail, because progress belongs to the query and the query lives up here.
 */
@Composable
internal fun SearchViewBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onBack: () -> Unit,
    searching: Boolean,
    modifier: Modifier = Modifier,
) {
    val focusRequester = remember { FocusRequester() }
    val inspection = LocalInspectionMode.current
    // The screen exists to be typed into, and every path that reaches it is one the user asked
    // for. Skipped under @Preview, where there is no window to take focus in.
    LaunchedEffect(Unit) { if (!inspection) focusRequester.requestFocus() }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(56.dp)
                .clip(BarShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = AppIcons.Back,
                    contentDescription = stringResource(R.string.food_search_close),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Box(modifier = Modifier.weight(1f).padding(horizontal = 8.dp)) {
                if (query.isEmpty()) {
                    Text(
                        text = stringResource(R.string.food_search_placeholder),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge
                        .copy(color = MaterialTheme.colorScheme.onSurface),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                )
            }
            if (query.isEmpty()) {
                // Decorative once the field is already focused — there is nothing to press it for,
                // which is why it is a glyph and not a button.
                Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = AppIcons.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        imageVector = AppIcons.Close,
                        contentDescription = stringResource(R.string.food_search_clear),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        if (searching) {
            LinearProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp)
                    .height(2.dp),
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun SearchViewBarEmptyPreview() {
    AppTheme {
        Surface {
            SearchViewBar(query = "", onQueryChange = {}, onBack = {}, searching = false)
        }
    }
}

@PreviewLightDark
@Composable
private fun SearchViewBarSearchingPreview() {
    AppTheme {
        Surface {
            SearchViewBar(query = "sky flakes", onQueryChange = {}, onBack = {}, searching = true)
        }
    }
}
