package ph.mart.healthapp.feature.food.ui.recipe.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.data.food.MAX_RECIPE_CHARS
import ph.mart.healthapp.core.designsystem.component.AppTextField
import ph.mart.healthapp.core.designsystem.component.SendStopButton
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.food.R

/** Tall enough to show a pasted list's first few lines, short enough to leave the form in view. */
private const val DESCRIPTION_LINES = 5

/**
 * The recipe builder's way in: a dish, a sentence or a pasted ingredient list, sent with the quick
 * log's [SendStopButton] — the app's one control for "spend a model call", and its stop.
 *
 * Multi-line with no send key on the keyboard: a pasted list needs its return key, so the circle
 * is the only send. A paste past [MAX_RECIPE_CHARS] is cut rather than refused, which is what the
 * repository would do with it anyway, and here the user can see where.
 */
@Composable
internal fun RecipeDescribeField(
    text: String,
    filling: Boolean,
    error: String?,
    onTextChange: (String) -> Unit,
    onFill: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        // The label sits outside the row, the field's own style, so the circle lines up with the
        // field's top edge and stays there when the error line grows the column beneath it.
        Text(
            text = stringResource(R.string.food_recipe_describe_label),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
            AppTextField(
                value = text,
                onValueChange = { onTextChange(it.take(MAX_RECIPE_CHARS)) },
                placeholder = stringResource(R.string.food_recipe_describe_placeholder),
                error = error,
                maxLines = DESCRIPTION_LINES,
                modifier = Modifier.weight(1f),
            )
            SendStopButton(
                sending = filling,
                canSend = text.isNotBlank(),
                onSend = {
                    focusManager.clearFocus()
                    onFill()
                },
                onStop = onStop,
                sendLabel = stringResource(R.string.food_recipe_fill),
                stopLabel = stringResource(R.string.food_recipe_fill_stop),
            )
        }
        // Every figure below the field may be the model's, and the app says so wherever one is.
        Text(
            text = stringResource(R.string.food_recipe_describe_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@PreviewLightDark
@Composable
private fun RecipeDescribeFieldPreview() {
    AppTheme {
        Surface {
            RecipeDescribeField(
                text = "",
                filling = false,
                error = null,
                onTextChange = {},
                onFill = {},
                onStop = {},
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

/** Mid-call: the circle is the stop. */
@PreviewLightDark
@Composable
private fun RecipeDescribeFieldFillingPreview() {
    AppTheme {
        Surface {
            RecipeDescribeField(
                text = "Chicken adobo for 4 — 1 kg thighs, soy sauce, vinegar, garlic, bay leaves",
                filling = true,
                error = null,
                onTextChange = {},
                onFill = {},
                onStop = {},
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun RecipeDescribeFieldErrorPreview() {
    AppTheme {
        Surface {
            RecipeDescribeField(
                text = "my shopping list",
                filling = false,
                error = stringResource(R.string.food_recipe_fill_nothing),
                onTextChange = {},
                onFill = {},
                onStop = {},
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}
