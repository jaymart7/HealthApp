package ph.mart.healthapp.core.designsystem.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.designsystem.R
import ph.mart.healthapp.core.designsystem.icon.AppIcons
import ph.mart.healthapp.core.designsystem.theme.AppTheme

/**
 * Fiber, sugar and sodium, behind a "More nutrients" disclosure — the sibling of [MacroInputGroup],
 * deliberately *not* three more rows inside it. One of that component's callers is onboarding's
 * target screen, where a nutrient has nothing to mean.
 *
 * **These three, and only these three.** An entry also carries vitamin D, calcium, iron and
 * potassium now, but they are seeded by a scan or a picked food, repriced with the portion and
 * never typed — nobody hand-corrects a calcium figure — so there is no stepper for them here. All
 * seven are shown where they are graded, in `NutrientPanel`.
 *
 * These three carry **no colour**. Protein = `primary`, carbs = `tertiary`, fat = `secondary` are
 * fixed everywhere in the app because those three share a bar; nothing in this group appears in
 * that bar, so borrowing a dot would claim a relationship that isn't there.
 *
 * The section opens itself when a value arrives non-zero, so a scanned or AI-estimated food shows
 * its sodium without a tap, while a hand-typed quick add keeps the sheet the height it is today.
 *
 * **Null is unset, and the callers are the ones mapping `0` onto it.** `Nutrients` stores these as
 * non-null `Int` and records at its own definition that `0` means unknown-or-none, so a caller
 * holding one passes `takeIf { it > 0 }` and writes back `?: 0` — the conflation stays exactly
 * where it already was, and the cells get to print a dash rather than three zeroes nobody typed.
 *
 * The closed row carries a **summary of what is actually set**, so the section says whether there
 * is anything behind it without being opened. Listing all three names when only sodium arrived
 * would be the same claim the dash exists to avoid.
 */
@Composable
fun MicronutrientInputGroup(
    fiberG: Int?,
    sugarG: Int?,
    sodiumMg: Int?,
    onFiberChange: (Int?) -> Unit,
    onSugarChange: (Int?) -> Unit,
    onSodiumChange: (Int?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val seeded = fiberG != null || sugarG != null || sodiumMg != null
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
                .clickable(role = Role.Button) { expanded = !expanded }
                .heightIn(min = 48.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.ds_nutrients_more),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = summaryOf(fiberG, sugarG, sodiumMg),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = 8.dp),
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
            // Unit symbols are not copy. Sodium's is the reason the cell takes one at all.
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MacroFieldCell(
                    label = stringResource(R.string.ds_nutrient_fiber),
                    value = fiberG,
                    unit = "g",
                    onValueChange = onFiberChange,
                    modifier = Modifier.weight(1f),
                )
                MacroFieldCell(
                    label = stringResource(R.string.ds_nutrient_sugar),
                    value = sugarG,
                    unit = "g",
                    onValueChange = onSugarChange,
                    modifier = Modifier.weight(1f),
                )
                MacroFieldCell(
                    label = stringResource(R.string.ds_nutrient_sodium),
                    value = sodiumMg,
                    unit = "mg",
                    onValueChange = onSodiumChange,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/** The names of the nutrients that actually carry a figure, or "Optional" when none does. The
 * separator is punctuation, not copy. */
@Composable
private fun summaryOf(fiberG: Int?, sugarG: Int?, sodiumMg: Int?): String {
    val set = listOfNotNull(
        fiberG?.let { stringResource(R.string.ds_nutrient_fiber) },
        sugarG?.let { stringResource(R.string.ds_nutrient_sugar) },
        sodiumMg?.let { stringResource(R.string.ds_nutrient_sodium) },
    )
    return if (set.isEmpty()) stringResource(R.string.ds_nutrients_optional) else set.joinToString(" · ")
}

/** Nothing seeded — the state a quick add opens in, and the one that must not grow the sheet. */
@PreviewLightDark
@Composable
private fun MicronutrientInputGroupCollapsedPreview() {
    AppTheme {
        Surface {
            MicronutrientInputGroup(
                fiberG = null,
                sugarG = null,
                sodiumMg = null,
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
