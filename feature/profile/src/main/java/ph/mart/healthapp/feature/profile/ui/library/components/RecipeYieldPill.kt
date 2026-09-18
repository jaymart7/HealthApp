package ph.mart.healthapp.feature.profile.ui.library.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.core.designsystem.theme.tabularNums
import ph.mart.healthapp.feature.profile.R

/**
 * "makes 4", beside a recipe's per-serving figure.
 *
 * A food is a unit price and a saved meal is a bundle; a recipe is the only one of the three that
 * is a *yield*, and the yield is what makes its calorie figure mean per-serving rather than total.
 * Bordered rather than filled, because it is a qualifier on the figure next to it and not a second
 * figure — a filled chip here would read as a third thing to compare.
 */
@Composable
internal fun RecipeYieldPill(servings: Int, modifier: Modifier = Modifier) {
    Surface(
        color = Color.Transparent,
        shape = RoundedCornerShape(6.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier,
    ) {
        Text(
            text = stringResource(R.string.profile_library_makes, servings),
            style = MaterialTheme.typography.labelMedium.tabularNums,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}

@PreviewLightDark
@Composable
private fun RecipeYieldPillPreview() {
    AppTheme {
        Surface(color = MaterialTheme.colorScheme.surfaceContainerLow) {
            RecipeYieldPill(servings = 4, modifier = Modifier.padding(16.dp))
        }
    }
}
