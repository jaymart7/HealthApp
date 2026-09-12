package ph.mart.healthapp.feature.food.ui.shared.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fitInside
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.WindowInsetsRulers
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.data.food.MealType
import ph.mart.healthapp.core.data.food.Nutrients
import ph.mart.healthapp.core.designsystem.component.AppTopBar
import ph.mart.healthapp.core.designsystem.component.MacroBar
import ph.mart.healthapp.core.designsystem.component.MacroFieldGroup
import ph.mart.healthapp.core.designsystem.component.MicronutrientInputGroup
import ph.mart.healthapp.core.designsystem.component.PrimaryButton
import ph.mart.healthapp.core.designsystem.component.TextButton
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.food.R
import ph.mart.healthapp.feature.food.ui.shared.AddEntryForm
import ph.mart.healthapp.feature.food.ui.shared.isValid

/** How far the content has to scroll before the bar takes over the food's name. One card's worth,
 * roughly: past this the name is off screen and the bar is the only thing that still knows it. */
private const val TITLE_HANDOVER_PX = 240

/**
 * The barcode flow's review step, and the photo flow's after a search hit or a hand entry. Same
 * parts as [ConfirmationScreen][ph.mart.healthapp.feature.food.ui.photo.components.ConfirmationScreen]
 * minus the photo and the AI chrome: a barcode match is a database row, not an estimate, so there is
 * no `AIChip` and no confidence notice here — `tertiaryContainer` stays the AI accent alone, and the
 * only `tertiary` on this screen is the carbs cell's dot.
 *
 * **One subject, then its corrections.** The screen used to be a flat stack of nine input boxes, all
 * weighted the same, which made confirming a scan into a reading exercise. Now the name, the portion
 * and the calories sit in [SubjectCard] — the screen's only card — the macros are a row of tiles
 * under it, and the micronutrients are behind a disclosure. Nothing was removed; the order now says
 * what is being checked and what is merely available.
 *
 * [manualEntry] is what the form knows that the flow does not: a scan that found nothing and a
 * search that was skipped both arrive here with a blank form, and they need the opposite caveat
 * from a seeded one ("enter the values for this portion" rather than "these are per 100 g"). It
 * also picks the title, because "Review this item" is the wrong verb for an item nobody supplied.
 *
 * [subtitle] is null for a found product — the per-100 g caveat it used to carry now sits under the
 * portion it is about, which is the number it was always talking about.
 */
@Composable
internal fun ScanConfirmationScreen(
    form: AddEntryForm,
    manualEntry: Boolean,
    onFormChange: (AddEntryForm) -> Unit,
    onMealTypeSelect: (MealType) -> Unit,
    onLogEntry: () -> Unit,
    onDiscard: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
) {
    val scroll = rememberScrollState()
    // Both are derived so the screen re-composes when an *answer* changes rather than on every
    // scrolled pixel. `form.name` is deliberately outside them: a derivedStateOf captures a
    // non-state value at the composition it was remembered in, so folding the name into one would
    // freeze the bar on whatever the name was when the screen opened.
    val scrolled by remember { derivedStateOf { scroll.value > 0 } }
    val scrolledPastTitle by remember { derivedStateOf { scroll.value > TITLE_HANDOVER_PX } }
    val handedOver = scrolledPastTitle && form.name.isNotBlank()

    Surface(color = MaterialTheme.colorScheme.surface, modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                // The whole screen sits above the keyboard, which is what docks the action bar:
                // it is at the bottom of what is left, not under the IME.
                .fitInside(WindowInsetsRulers.Ime.current),
        ) {
            AppTopBar(
                title = if (handedOver) {
                    form.name
                } else {
                    stringResource(if (manualEntry) R.string.food_scan_add_title else R.string.food_scan_review)
                },
                // The same question back asks: a dirty form is confirmed before it is thrown away.
                onBack = onDiscard,
                titleStyle = if (handedOver) {
                    MaterialTheme.typography.titleMedium
                } else {
                    MaterialTheme.typography.titleLarge
                },
            )
            // Only once something has scrolled under it — a rule over an unscrolled page is a line
            // separating a heading from the thing it heads.
            if (scrolled) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scroll)
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                MealTypeChipRow(selected = form.mealType, onSelect = onMealTypeSelect)

                SubjectCard(form = form, manualEntry = manualEntry, onFormChange = onFormChange)

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    CardLabel(stringResource(R.string.food_macros))
                    // The split of what is actually in the cells: an empty track while there is
                    // nothing to split, which is the honest picture of a form nobody has filled in.
                    MacroBar(
                        proteinG = form.proteinG ?: 0,
                        carbsG = form.carbsG ?: 0,
                        fatG = form.fatG ?: 0,
                    )
                    MacroFieldGroup(
                        proteinG = form.proteinG,
                        carbsG = form.carbsG,
                        fatG = form.fatG,
                        onProteinChange = { onFormChange(form.copy(proteinG = it)) },
                        onCarbsChange = { onFormChange(form.copy(carbsG = it)) },
                        onFatChange = { onFormChange(form.copy(fatG = it)) },
                    )
                }

                MicronutrientInputGroup(
                    fiberG = form.nutrients.fiberG.takeIf { it > 0 },
                    sugarG = form.nutrients.sugarG.takeIf { it > 0 },
                    sodiumMg = form.nutrients.sodiumMg.takeIf { it > 0 },
                    onFiberChange = { onFormChange(form.copy(nutrients = form.nutrients.copy(fiberG = it ?: 0))) },
                    onSugarChange = { onFormChange(form.copy(nutrients = form.nutrients.copy(sugarG = it ?: 0))) },
                    onSodiumChange = { onFormChange(form.copy(nutrients = form.nutrients.copy(sodiumMg = it ?: 0))) },
                )
            }

            DockedActionBar(
                logEnabled = form.isValid(),
                onLogEntry = onLogEntry,
                onDiscard = onDiscard,
            )
        }
    }
}

/**
 * Log and discard, docked rather than scrolled to.
 *
 * The buttons used to be the bottom of the form, which meant logging a scan you had not edited
 * still cost a scroll past six inputs you did not need to look at. Ruled off rather than floated:
 * the content behind it is a scroll with an edge, and a shadow would only blur that edge.
 *
 * **Discard leaves while the keyboard is up.** Standing a destructive full-width button directly
 * under the IME is standing it where a mis-swipe at a suggestion bar lands, and nothing on this
 * screen needs discarding mid-word — back still does it, and asks first.
 */
@Composable
private fun DockedActionBar(logEnabled: Boolean, onLogEntry: () -> Unit, onDiscard: () -> Unit) {
    val imeOpen = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    Column(modifier = Modifier.fillMaxWidth()) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            PrimaryButton(
                label = stringResource(R.string.food_scan_log_item),
                onClick = onLogEntry,
                enabled = logEnabled,
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
            )
            if (!imeOpen) {
                TextButton(
                    label = stringResource(R.string.food_discard),
                    onClick = onDiscard,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun ScanConfirmationScreenFoundPreview() {
    AppTheme {
        ScanConfirmationScreen(
            form = AddEntryForm(
                mealType = MealType.Snacks,
                name = "Nutella",
                portionAmount = 100.0,
                portionUnit = "g",
                calories = 539,
                proteinG = 6,
                carbsG = 58,
                fatG = 31,
                nutrients = Nutrients(sugarG = 56, sodiumMg = 41),
            ),
            manualEntry = false,
            onFormChange = {},
            onMealTypeSelect = {},
            onLogEntry = {},
            onDiscard = {},
        )
    }
}

/** The not-found path: nothing seeded, so nothing claimed. */
@PreviewLightDark
@Composable
private fun ScanConfirmationScreenManualPreview() {
    AppTheme {
        ScanConfirmationScreen(
            form = AddEntryForm(mealType = MealType.Lunch),
            manualEntry = true,
            subtitle = "That barcode isn't in the database, so these numbers are yours to fill in — check the label.",
            onFormChange = {},
            onMealTypeSelect = {},
            onLogEntry = {},
            onDiscard = {},
        )
    }
}
