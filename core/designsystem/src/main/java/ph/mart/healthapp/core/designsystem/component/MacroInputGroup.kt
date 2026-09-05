package ph.mart.healthapp.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.designsystem.R
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.core.designsystem.theme.tabularNums

/**
 * Three labeled numeric rows (Protein/Carbs/Fat) using the fixed semantic colors — protein =
 * `primary`, carbs = `tertiary`, fat = `secondary` — identical across every macro display in the
 * app.
 *
 * Every value is typable. A macro figure is *entered*, not nudged: 48g of carbs off a photo
 * estimate is 48 taps at the default step, which is not a correction path anyone would use.
 */
@Composable
fun MacroInputGroup(
    proteinG: Int,
    carbsG: Int,
    fatG: Int,
    onProteinChange: (Int) -> Unit,
    onCarbsChange: (Int) -> Unit,
    onFatChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    step: Int = 1,
    showPercentages: Boolean = false,
) {
    val proteinKcal = proteinG * 4
    val carbsKcal = carbsG * 4
    val fatKcal = fatG * 9
    val totalKcal = (proteinKcal + carbsKcal + fatKcal).coerceAtLeast(1)
    val percentFormat = stringResource(R.string.ds_macro_percent)
    fun label(base: String, kcal: Int) =
        if (showPercentages) percentFormat.format(base, kcal * 100 / totalKcal) else base

    val protein = stringResource(R.string.ds_macro_protein)
    val carbs = stringResource(R.string.ds_macro_carbs)
    val fat = stringResource(R.string.ds_macro_fat)
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        MacroRow(protein, label(protein, proteinKcal), proteinG, MaterialTheme.colorScheme.primary, step, onProteinChange)
        MacroRow(carbs, label(carbs, carbsKcal), carbsG, MaterialTheme.colorScheme.tertiary, step, onCarbsChange)
        MacroRow(fat, label(fat, fatKcal), fatG, MaterialTheme.colorScheme.secondary, step, onFatChange)
    }
}

/** [macro] is the bare name for screen readers; [label] is what is drawn, which may carry a
 * percentage the announcement doesn't need to repeat three times. */
@Composable
private fun MacroRow(
    macro: String,
    label: String,
    grams: Int,
    dotColor: Color,
    step: Int,
    onChange: (Int) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(start = 16.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(dotColor),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp),
        )
        StepperValueField(
            value = grams.toString(),
            onValueChange = { onChange(it.toIntOrNull() ?: 0) },
            contentDescription = stringResource(R.string.ds_macro_grams, macro),
            // Wide enough for three digits without giving the row's whole width to a number that
            // is almost always two; right-aligned so it sits against its "g" the way it always did.
            textAlign = TextAlign.End,
            modifier = Modifier.width(56.dp),
        )
        Text(
            text = "g",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        StepperButton(
            symbol = "−",
            label = stringResource(R.string.ds_decrease, macro),
            onClick = { onChange((grams - step).coerceAtLeast(0)) },
        )
        StepperButton(
            symbol = "+",
            label = stringResource(R.string.ds_increase, macro),
            onClick = { onChange(grams + step) },
        )
    }
}

@PreviewLightDark
@Composable
private fun MacroInputGroupPreview() {
    AppTheme {
        Surface {
            MacroInputGroup(
                proteinG = 32,
                carbsG = 48,
                fatG = 14,
                onProteinChange = {},
                onCarbsChange = {},
                onFatChange = {},
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun MacroInputGroupWithPercentagesPreview() {
    AppTheme {
        Surface {
            MacroInputGroup(
                proteinG = 135,
                carbsG = 179,
                fatG = 60,
                onProteinChange = {},
                onCarbsChange = {},
                onFatChange = {},
                step = 5,
                showPercentages = true,
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}
