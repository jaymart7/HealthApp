package ph.mart.healthapp.core.designsystem.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.designsystem.R
import ph.mart.healthapp.core.designsystem.icon.AppIcons
import ph.mart.healthapp.core.designsystem.theme.AppTheme

/**
 * Fiber, sugar and sodium, behind a "More nutrients" disclosure — the sibling of [MacroInputGroup],
 * deliberately *not* three more rows inside it. One of that component's callers is onboarding's
 * target screen, where a micronutrient has nothing to mean.
 *
 * These three carry **no colour**. Protein = `primary`, carbs = `tertiary`, fat = `secondary` are
 * fixed everywhere in the app because those three share a bar; fiber, sugar and sodium appear in no
 * bar and no chart, so borrowing a dot would claim a relationship that isn't there.
 *
 * The section opens itself when a value arrives non-zero, so a scanned or AI-estimated food shows
 * its sodium without a tap, while a hand-typed quick add keeps the sheet the height it is today.
 */
@Composable
fun MicronutrientInputGroup(
    fiberG: Int,
    sugarG: Int,
    sodiumMg: Int,
    onFiberChange: (Int) -> Unit,
    onSugarChange: (Int) -> Unit,
    onSodiumChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val seeded = fiberG > 0 || sugarG > 0 || sodiumMg > 0
    // Seeded once, at first composition: the section must not slam shut again the moment the user
    // clears the field they just opened it to correct.
    var expanded by rememberSaveable { mutableStateOf(seeded) }
    val everSeeded = remember { seeded }
    if (seeded && !everSeeded) expanded = true

    val rotation by animateFloatAsState(if (expanded) 180f else 0f, label = "chevron")

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable { expanded = !expanded }
                .heightIn(min = 48.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.ds_nutrients_more),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            Icon(
                imageVector = AppIcons.ChevronDown,
                contentDescription = stringResource(
                    if (expanded) R.string.ds_nutrients_hide else R.string.ds_nutrients_show,
                ),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.rotate(rotation),
            )
        }
        if (expanded) {
            NutrientRow(stringResource(R.string.ds_nutrient_fiber), fiberG, "g", step = 1, onChange = onFiberChange)
            NutrientRow(stringResource(R.string.ds_nutrient_sugar), sugarG, "g", step = 1, onChange = onSugarChange)
            // A 50mg step: sodium is the one figure here counted in the hundreds, and nudging it
            // by 1 would be the tap-count problem StepperValueField exists to solve.
            NutrientRow(stringResource(R.string.ds_nutrient_sodium), sodiumMg, "mg", step = 50, onChange = onSodiumChange)
        }
    }
}

@Composable
private fun NutrientRow(
    nutrient: String,
    value: Int,
    unit: String,
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
        Text(
            text = nutrient,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        StepperValueField(
            value = value.toString(),
            onValueChange = { onChange(it.toIntOrNull() ?: 0) },
            contentDescription = stringResource(
                if (unit == "mg") R.string.ds_nutrient_in_milligrams else R.string.ds_nutrient_in_grams,
                nutrient,
            ),
            // Four digits wide, not three: sodium routinely runs past 1000mg.
            textAlign = TextAlign.End,
            modifier = Modifier.width(64.dp),
        )
        Text(
            text = unit,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        StepperButton(
            symbol = "−",
            label = stringResource(R.string.ds_decrease, nutrient),
            onClick = { onChange((value - step).coerceAtLeast(0)) },
        )
        StepperButton(
            symbol = "+",
            label = stringResource(R.string.ds_increase, nutrient),
            onClick = { onChange(value + step) },
        )
    }
}

/** Nothing seeded — the state a quick add opens in, and the one that must not grow the sheet. */
@PreviewLightDark
@Composable
private fun MicronutrientInputGroupCollapsedPreview() {
    AppTheme {
        Surface {
            MicronutrientInputGroup(
                fiberG = 0,
                sugarG = 0,
                sodiumMg = 0,
                onFiberChange = {},
                onSugarChange = {},
                onSodiumChange = {},
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

/** A scanned packet: the section opens itself because the numbers arrived with the food. */
@PreviewLightDark
@Composable
private fun MicronutrientInputGroupSeededPreview() {
    AppTheme {
        Surface {
            MicronutrientInputGroup(
                fiberG = 6,
                sugarG = 12,
                sodiumMg = 1240,
                onFiberChange = {},
                onSugarChange = {},
                onSodiumChange = {},
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}
