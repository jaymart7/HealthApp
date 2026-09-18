package ph.mart.healthapp.feature.profile.ui.library.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.core.designsystem.theme.tabularNums
import ph.mart.healthapp.feature.profile.R

/**
 * A food's macros, in the colours the rest of the app already gives them — protein `primary`,
 * carbs `tertiary`, fat `secondary`, the fixed mapping every macro bar, chart and legend uses.
 *
 * This row was the last place in the app where P/C/F were grey text. It sits in the detail slot
 * of a My foods row, where a saved meal or a recipe puts its contents line: a food has no parts
 * to name, and its macros are the thing that says what kind of food it is.
 *
 * Full opacity, no alpha — the figures are 12sp and a faded 12sp figure on
 * `surfaceContainerLow` is the one pairing in this flow that would miss 4.5:1. The letter is
 * printed as well as the colour, so nothing here is carried by colour alone.
 */
@Composable
internal fun MacroTriplet(proteinG: Int, carbsG: Int, fatG: Int, modifier: Modifier = Modifier) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = modifier) {
        MacroValue(
            text = stringResource(R.string.profile_library_macro_protein, proteinG),
            color = MaterialTheme.colorScheme.primary,
        )
        MacroValue(
            text = stringResource(R.string.profile_library_macro_carbs, carbsG),
            color = MaterialTheme.colorScheme.tertiary,
        )
        MacroValue(
            text = stringResource(R.string.profile_library_macro_fat, fatG),
            color = MaterialTheme.colorScheme.secondary,
        )
    }
}

@Composable
private fun MacroValue(text: String, color: Color) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(color))
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium.tabularNums.copy(fontWeight = FontWeight.Medium),
            color = color,
        )
    }
}

@PreviewLightDark
@Composable
private fun MacroTripletPreview() {
    AppTheme {
        Surface(color = MaterialTheme.colorScheme.surfaceContainerLow) {
            MacroTriplet(proteinG = 28, carbsG = 12, fatG = 28, modifier = Modifier.padding(16.dp))
        }
    }
}
