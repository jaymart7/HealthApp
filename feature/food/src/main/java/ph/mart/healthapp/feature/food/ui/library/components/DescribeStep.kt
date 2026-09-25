package ph.mart.healthapp.feature.food.ui.library.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.data.food.MAX_RECIPE_CHARS
import ph.mart.healthapp.core.designsystem.component.AppTextField
import ph.mart.healthapp.core.designsystem.component.SendStopButton
import ph.mart.healthapp.core.designsystem.component.TextButton
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.food.R

/** Room for a sentence without scrolling, and for the first few lines of a pasted list. */
private const val DESCRIBE_MIN_LINES = 2
private const val DESCRIBE_MAX_LINES = 5

/**
 * The library's way in: one question, one field, and the quick log's [SendStopButton] — the app's
 * one control for "spend a model call", and its stop. The model decides whether what was written is
 * a food or a recipe; the user never has to pick first.
 *
 * Multi-line with no send key on the keyboard: a pasted list needs its return key, so the circle is
 * the only send. A paste past [MAX_RECIPE_CHARS] is cut rather than refused, which is what the
 * repository would do with it anyway, and here the user can see where.
 *
 * The two text buttons under it are the whole manual path, and so the whole offline one. Once a
 * review exists — the user came back here from it — they give way to [onResume], one way back to
 * it: a manual door there would wipe the review without asking.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun DescribeStep(
    text: String,
    filling: Boolean,
    error: String?,
    onTextChange: (String) -> Unit,
    onFill: () -> Unit,
    onStop: () -> Unit,
    onTypeFood: () -> Unit,
    onBuildRecipe: () -> Unit,
    modifier: Modifier = Modifier,
    onResume: (() -> Unit)? = null,
) {
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    val inspection = LocalInspectionMode.current
    LaunchedEffect(Unit) { if (!inspection) focusRequester.requestFocus() }

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Text(
            text = stringResource(R.string.food_library_describe_title),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = stringResource(R.string.food_library_describe_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Bottom,
            modifier = Modifier.padding(top = 12.dp),
        ) {
            AppTextField(
                value = text,
                onValueChange = { onTextChange(it.take(MAX_RECIPE_CHARS)) },
                placeholder = stringResource(R.string.food_library_describe_placeholder),
                error = error,
                minLines = DESCRIBE_MIN_LINES,
                maxLines = DESCRIBE_MAX_LINES,
                // The quick log composer's look: a filled pill, no outline.
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                border = null,
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester),
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
        when {
            filling -> Unit
            onResume != null -> TextButton(label = stringResource(R.string.food_library_resume), onClick = onResume)
            else -> FlowRow(modifier = Modifier.fillMaxWidth()) {
                TextButton(label = stringResource(R.string.food_library_type_food), onClick = onTypeFood)
                TextButton(label = stringResource(R.string.food_library_build_recipe), onClick = onBuildRecipe)
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun DescribeStepPreview() {
    AppTheme {
        Surface {
            DescribeStep(
                text = "",
                filling = false,
                error = null,
                onTextChange = {},
                onFill = {},
                onStop = {},
                onTypeFood = {},
                onBuildRecipe = {},
            )
        }
    }
}

/** Mid-call: the circle is the stop, and the manual doors step aside until it answers. */
@PreviewLightDark
@Composable
private fun DescribeStepFillingPreview() {
    AppTheme {
        Surface {
            DescribeStep(
                text = "Chicken adobo for 4 — 1 kg thighs, soy sauce, vinegar, garlic, bay leaves",
                filling = true,
                error = null,
                onTextChange = {},
                onFill = {},
                onStop = {},
                onTypeFood = {},
                onBuildRecipe = {},
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun DescribeStepErrorPreview() {
    AppTheme {
        Surface {
            DescribeStep(
                text = "my shopping list",
                filling = false,
                error = stringResource(R.string.food_library_fill_nothing),
                onTextChange = {},
                onFill = {},
                onStop = {},
                onTypeFood = {},
                onBuildRecipe = {},
            )
        }
    }
}

/** Back from a review: one way forward to it, in place of the manual doors that would wipe it. */
@PreviewLightDark
@Composable
private fun DescribeStepResumePreview() {
    AppTheme {
        Surface {
            DescribeStep(
                text = "Chili for 4, beef, beans",
                filling = false,
                error = null,
                onTextChange = {},
                onFill = {},
                onStop = {},
                onTypeFood = {},
                onBuildRecipe = {},
                onResume = {},
            )
        }
    }
}
