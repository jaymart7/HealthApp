package ph.mart.healthapp.feature.food.ui.shared.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.data.food.MealType
import ph.mart.healthapp.core.data.food.RecognitionConfidence
import ph.mart.healthapp.core.designsystem.component.AppCard
import ph.mart.healthapp.core.designsystem.component.FoodItemRow
import ph.mart.healthapp.core.designsystem.component.FoodItemRowVariant
import ph.mart.healthapp.core.designsystem.component.MacroFieldGroup
import ph.mart.healthapp.core.designsystem.component.MicronutrientInputGroup
import ph.mart.healthapp.core.designsystem.component.macroLine
import ph.mart.healthapp.core.designsystem.component.TextButton
import ph.mart.healthapp.core.designsystem.icon.AppIcons
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.core.designsystem.theme.tabularNums
import ph.mart.healthapp.feature.food.R
import ph.mart.healthapp.feature.food.ui.shared.AddEntryForm
import ph.mart.healthapp.feature.food.ui.shared.withPortionAmount

/**
 * One estimated food, before any of it is written: the row as it stands, and the whole form behind
 * a tap.
 *
 * In `shared/` because two flows review a list of estimates now — talk-to-log, which reads a
 * sentence, and the photo flow, which reads a plate. It was the voice screen's private card until
 * a photographed plate stopped being a single food; the rule is the one `CLAUDE.md` states, that a
 * component crossing flows inside one feature does not stay in whichever flow declared it first.
 * Which is also why the redesign lands here once rather than as a voice-only row beside it.
 *
 * Collapsed by default and opened one at a time by the caller — the list is the thing being
 * checked, and a screen of five expanded forms is not a list. A portion change reprices through
 * the existing [withPortionAmount], so the numbers below the name stay consistent with it.
 *
 * **The collapsed row carries its own doubt.** A batch notice says *some* of these are guesses and
 * leaves the user to find which; a [RecognitionConfidence.Low] row says so on itself, and where the
 * model named the words it could not pin down, the expanded footer quotes them back. The chip is
 * `tertiaryContainer`, the app's AI accent, because that is exactly what it is about.
 *
 * **Remove moved into the expansion.** The collapsed row's end is the number being checked, and a
 * delete button beside it was a 40dp target for the most destructive thing on the screen sitting
 * where the eye goes for the figure. Opening a row first is one tap, and it is the row you were
 * going to read anyway.
 */
@Composable
internal fun ReviewItemCard(
    item: AddEntryForm,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    onChange: (AddEntryForm) -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AppCard(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        // The one 2dp primary border in either review flow, so "open" reads without counting rows.
        border = if (expanded) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
        contentPadding = PaddingValues(0.dp),
    ) {
        CollapsedRow(item = item, expanded = expanded, onToggleExpanded = onToggleExpanded)

        if (expanded) {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
            ) {
                FoodItemRow(
                    variant = FoodItemRowVariant.Editable,
                    name = item.name,
                    portionAmount = item.portionAmount,
                    portionUnit = item.portionUnit,
                    calories = item.calories ?: 0,
                    proteinG = item.proteinG ?: 0,
                    carbsG = item.carbsG ?: 0,
                    fatG = item.fatG ?: 0,
                    onNameChange = { onChange(item.copy(name = it)) },
                    onPortionAmountChange = { onChange(item.withPortionAmount(it)) },
                    onPortionUnitChange = { onChange(item.copy(portionUnit = it)) },
                    onCaloriesChange = { onChange(item.copy(calories = it)) },
                )
                MacroFieldGroup(
                    proteinG = item.proteinG,
                    carbsG = item.carbsG,
                    fatG = item.fatG,
                    onProteinChange = { onChange(item.copy(proteinG = it)) },
                    onCarbsChange = { onChange(item.copy(carbsG = it)) },
                    onFatChange = { onChange(item.copy(fatG = it)) },
                )
                MicronutrientInputGroup(
                    fiberG = item.nutrients.fiberG.takeIf { it > 0 },
                    sugarG = item.nutrients.sugarG.takeIf { it > 0 },
                    sodiumMg = item.nutrients.sodiumMg.takeIf { it > 0 },
                    onFiberChange = { onChange(item.copy(nutrients = item.nutrients.copy(fiberG = it ?: 0))) },
                    onSugarChange = { onChange(item.copy(nutrients = item.nutrients.copy(sugarG = it ?: 0))) },
                    onSodiumChange = { onChange(item.copy(nutrients = item.nutrients.copy(sodiumMg = it ?: 0))) },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    // The phrase where the model gave one, the plain flag where it didn't — the
                    // chip is about this row either way, so the footer never goes empty on a
                    // low-confidence item. The weight is what holds Remove at the end when there
                    // is no chip at all.
                    Box(modifier = Modifier.weight(1f)) {
                        if (item.confidence == RecognitionConfidence.Low) {
                            ConfidenceChip(
                                label = item.uncertainAbout
                                    ?.let { stringResource(R.string.food_review_rough_guess, it) }
                                    ?: stringResource(R.string.food_review_check_this),
                            )
                        }
                    }
                    TextButton(
                        label = stringResource(R.string.food_review_remove),
                        onClick = onRemove,
                        icon = AppIcons.Delete,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}

/** What the row says with nothing open: the name, what it costs, and whether to look twice. */
@Composable
private fun CollapsedRow(item: AddEntryForm, expanded: Boolean, onToggleExpanded: () -> Unit) {
    Surface(
        onClick = onToggleExpanded,
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 72.dp)
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = item.name.ifBlank { stringResource(R.string.food_review_unnamed) },
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (item.confidence == RecognitionConfidence.Low) {
                        ConfidenceChip(label = stringResource(R.string.food_review_check_this))
                    }
                    Text(
                        text = macroLine(
                            portionAmount = item.portionAmount,
                            portionUnit = item.portionUnit,
                            proteinG = item.proteinG ?: 0,
                            carbsG = item.carbsG ?: 0,
                            fatG = item.fatG ?: 0,
                        ),
                        style = MaterialTheme.typography.bodySmall.tabularNums,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Text(
                text = stringResource(R.string.food_section_kcal, item.calories ?: 0),
                style = MaterialTheme.typography.titleLarge.tabularNums.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Icon(
                imageVector = if (expanded) AppIcons.ChevronUp else AppIcons.ChevronDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

/** The AI accent, at row scale. Nothing else in either review flow draws on `tertiaryContainer`
 * except the header chip and the batch notice, which are the same claim at other sizes. The FAB's
 * quick log draws it under its rows too, for the same doubt. */
@Composable
internal fun ConfidenceChip(label: String) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
        color = MaterialTheme.colorScheme.onTertiaryContainer,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .background(MaterialTheme.colorScheme.tertiaryContainer, RoundedCornerShape(10.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}

private val PREVIEW_ITEM = AddEntryForm(
    MealType.Lunch, "Grilled chicken breast", 150.0, "g", 210, 32, 2, 8,
)

private val PREVIEW_UNSURE = PREVIEW_ITEM.copy(
    name = "Toast",
    portionAmount = 1.0,
    portionUnit = "serving",
    calories = 90,
    proteinG = 3,
    carbsG = 17,
    fatG = 1,
    confidence = RecognitionConfidence.Low,
    uncertainAbout = "a slice",
)

@PreviewLightDark
@Composable
private fun ReviewItemCardPreview() {
    AppTheme {
        ReviewItemCard(
            item = PREVIEW_ITEM,
            expanded = false,
            onToggleExpanded = {},
            onChange = {},
            onRemove = {},
        )
    }
}

/** The row that is the point of the chip: closed, flagged, and readable as "look at this one"
 * without opening anything. */
@PreviewLightDark
@Composable
private fun ReviewItemCardUnsurePreview() {
    AppTheme {
        ReviewItemCard(
            item = PREVIEW_UNSURE,
            expanded = false,
            onToggleExpanded = {},
            onChange = {},
            onRemove = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun ReviewItemCardExpandedPreview() {
    AppTheme {
        ReviewItemCard(
            item = PREVIEW_ITEM,
            expanded = true,
            onToggleExpanded = {},
            onChange = {},
            onRemove = {},
        )
    }
}

/** Open, flagged, and quoting the words the model could not pin down — the footer's two halves
 * both drawn. */
@PreviewLightDark
@Composable
private fun ReviewItemCardExpandedUnsurePreview() {
    AppTheme {
        ReviewItemCard(
            item = PREVIEW_UNSURE,
            expanded = true,
            onToggleExpanded = {},
            onChange = {},
            onRemove = {},
        )
    }
}
