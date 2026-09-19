package ph.mart.healthapp.feature.profile.ui.supplement.components

import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.data.food.Nutrient
import ph.mart.healthapp.core.data.food.Nutrients
import ph.mart.healthapp.core.data.food.ironMgFrom
import ph.mart.healthapp.core.data.food.ironUgFrom
import ph.mart.healthapp.core.data.food.isEmpty
import ph.mart.healthapp.core.data.food.readings
import ph.mart.healthapp.core.data.food.vitaminDIuFrom
import ph.mart.healthapp.core.data.food.vitaminDUgFrom
import ph.mart.healthapp.core.designsystem.component.MacroFieldCell
import ph.mart.healthapp.core.designsystem.component.SegmentedToggle
import ph.mart.healthapp.core.designsystem.icon.AppIcons
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.profile.R

/**
 * What one dose carries, typed — the seven nutrients this app grades, behind a "What's in a dose"
 * disclosure.
 *
 * **This is the app's one exception to "the four are never typed".** `MicronutrientInputGroup`
 * offers fiber, sugar and sodium and refuses vitamin D, calcium, iron and potassium, on the
 * argument that nobody hand-corrects a calcium figure. That is true of a *plate*, where a
 * micronutrient is an estimate nobody can check against anything. A supplement bottle is the
 * opposite case in every respect: the figure is printed, there are one or two of them rather than
 * seven, and the user is holding the thing it is printed on. Without this the scan is the only way
 * to get a figure at all, so a bottle that photographs badly — or a model that misreads a digit —
 * has no way back.
 *
 * Feature-local rather than shared, and not a widening of `MicronutrientInputGroup`: one screen
 * draws this, and the four food callers of that component must keep offering exactly three.
 *
 * Its chrome is that component's, deliberately, down to the rule that **the section opens itself
 * when a figure is already there** — a scanned supplement shows what was read without a tap, while
 * a hand-typed one keeps the sheet the height it is today.
 */
@Composable
internal fun DoseNutrientFields(
    nutrients: Nutrients,
    onChange: (Nutrients) -> Unit,
    modifier: Modifier = Modifier,
) {
    val seeded = !nutrients.isEmpty
    // Seeded once, at first composition: the section must not slam shut again the moment the user
    // clears the field they opened it to correct — `MicronutrientInputGroup`'s reasoning.
    var expanded by rememberSaveable { mutableStateOf(seeded) }
    val everSeeded = remember { seeded }
    if (seeded && !everSeeded) expanded = true

    // Which unit the vitamin D box is in. Not stored and not saved: it is how the user is typing,
    // not what the supplement carries, which is always micrograms.
    var vitaminDInIu by rememberSaveable { mutableStateOf(false) }

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
                text = stringResource(R.string.profile_supplements_dose_nutrients),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = summaryOf(nutrients),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = 8.dp),
            )
            Icon(
                imageVector = AppIcons.ChevronDown,
                contentDescription = stringResource(
                    if (expanded) {
                        R.string.profile_supplements_dose_nutrients_hide
                    } else {
                        R.string.profile_supplements_dose_nutrients_show
                    },
                ),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.rotate(rotation),
            )
        }
        if (expanded) {
            Text(
                text = stringResource(R.string.profile_supplements_dose_nutrients_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            // Unit symbols are not copy. Panel order, the order every surface that grades these
            // draws them in.
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Cell(Nutrient.Fiber, nutrients.fiberG, "g", Modifier.weight(1f)) {
                    onChange(nutrients.copy(fiberG = it ?: 0))
                }
                Cell(Nutrient.Sugar, nutrients.sugarG, "g", Modifier.weight(1f)) {
                    onChange(nutrients.copy(sugarG = it ?: 0))
                }
                Cell(Nutrient.Sodium, nutrients.sodiumMg, "mg", Modifier.weight(1f)) {
                    onChange(nutrients.copy(sodiumMg = it ?: 0))
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Cell(Nutrient.Calcium, nutrients.calciumMg, "mg", Modifier.weight(1f)) {
                    onChange(nutrients.copy(calciumMg = it ?: 0))
                }
                // Milligrams in the box, micrograms in the row — `Nutrients` stores iron finer
                // than a panel prints it, and `ironUgFrom`/`ironMgFrom` are the one conversion.
                Cell(Nutrient.Iron, ironMgFrom(nutrients.ironUg), "mg", Modifier.weight(1f)) {
                    onChange(nutrients.copy(ironUg = ironUgFrom(it?.toDouble())))
                }
                Cell(Nutrient.Potassium, nutrients.potassiumMg, "mg", Modifier.weight(1f)) {
                    onChange(nutrients.copy(potassiumMg = it ?: 0))
                }
            }
            // Vitamin D gets a row to itself for the toggle beside it. A US bottle prints "2000
            // IU" and a European one "50 µg" for the same tablet, so a µg-only box turns the
            // commoner label into a silent 40× overstatement — and unlike a mistyped calcium, it
            // is one the day's panel then grades. The conversion is `:core:data`'s, tested there.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Cell(
                    nutrient = Nutrient.VitaminD,
                    value = if (vitaminDInIu) vitaminDIuFrom(nutrients.vitaminDUg) else nutrients.vitaminDUg,
                    unit = if (vitaminDInIu) "IU" else "µg",
                    modifier = Modifier.weight(1f),
                ) { typed ->
                    val stored = if (vitaminDInIu) {
                        vitaminDUgFrom(vitaminDUg = null, vitaminDIu = typed)
                    } else {
                        vitaminDUgFrom(vitaminDUg = typed?.toDouble(), vitaminDIu = null)
                    }
                    onChange(nutrients.copy(vitaminDUg = stored))
                }
                SegmentedToggle(
                    options = listOf("µg", "IU"),
                    selectedIndex = if (vitaminDInIu) 1 else 0,
                    onSelect = { vitaminDInIu = it == 1 },
                    minPillWidth = 44.dp,
                    modifier = Modifier.width(112.dp),
                )
            }
        }
    }
}

/**
 * One cell, named by the graded enum rather than by a string of this feature's own: these are the
 * same seven `NutrientPanel` grades, so they read the same in the sheet that fills them and the
 * panel that scores them.
 *
 * `0` is unset here and prints an em dash, the mapping [MacroFieldCell] documents and leaves to
 * its callers — `Nutrients` has no null to offer, and a dose declaring nothing must not read as a
 * dose declaring zero.
 */
@Composable
private fun Cell(
    nutrient: Nutrient,
    value: Int,
    unit: String,
    modifier: Modifier = Modifier,
    onValueChange: (Int?) -> Unit,
) {
    MacroFieldCell(
        label = stringResource(nutrient.labelRes),
        value = value.takeIf { it > 0 },
        unit = unit,
        onValueChange = onValueChange,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = modifier,
    )
}

/** The nutrients that actually carry a figure, or "Optional" when none does — so the closed row
 * says whether there is anything behind it. [readings] with no targets is already exactly that
 * list, in panel order, which is why nothing here re-derives it. */
@Composable
private fun summaryOf(nutrients: Nutrients): String {
    val set = nutrients.readings(targets = null)
    return if (set.isEmpty()) {
        stringResource(R.string.profile_supplements_dose_nutrients_optional)
    } else {
        // `map` is inline so a resource can be read inside it; `joinToString`'s transform is
        // not, which is the one thing that decides the shape of this line.
        set.map { stringResource(it.nutrient.labelRes) }.joinToString(" · ")
    }
}

/** Nothing typed — the state a hand-added supplement opens in, and the one that must not grow the
 * sheet. */
@PreviewLightDark
@Composable
private fun DoseNutrientFieldsEmptyPreview() {
    AppTheme {
        Surface {
            DoseNutrientFields(
                nutrients = Nutrients(),
                onChange = {},
                modifier = Modifier.padding(vertical = 16.dp),
            )
        }
    }
}

/** A scanned bottle: the section opens itself because the figures arrived with the supplement. */
@PreviewLightDark
@Composable
private fun DoseNutrientFieldsSeededPreview() {
    AppTheme {
        Surface {
            DoseNutrientFields(
                nutrients = Nutrients(
                    vitaminDUg = 25,
                    calciumMg = 210,
                    ironUg = 18_000,
                    potassiumMg = 80,
                ),
                onChange = {},
                modifier = Modifier.padding(vertical = 16.dp),
            )
        }
    }
}
