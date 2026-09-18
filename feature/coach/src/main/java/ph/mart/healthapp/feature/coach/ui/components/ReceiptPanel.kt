package ph.mart.healthapp.feature.coach.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.designsystem.icon.AppIcons
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.core.designsystem.theme.tabularNums
import ph.mart.healthapp.feature.coach.R

/**
 * Every figure that is about to be written, on a white panel inside the accent card.
 *
 * **The only `surfaceContainerLowest` in the conversation**, and that is the whole idea: the card
 * around it is `tertiaryContainer`, which says *a model made this*, and the panel inside it says
 * *these are the numbers*. A user who does not want 320 kcal of it needs to see the 320 before the
 * tap, not after, so nothing here is ever summarised away.
 *
 * It is a container rather than a component per kind — the kinds fill it, and the panel only owns
 * the surface, the radius and the padding.
 */
@Composable
internal fun ReceiptPanel(
    modifier: Modifier = Modifier,
    contentPadding: Int = 12,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(contentPadding.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content,
        )
    }
}

/** A label against its figure, on one baseline: the calories against the number, the weight
 * against the reading. The figure is the big one, because it is what the tap writes. */
@Composable
internal fun ReceiptHeadline(label: String, value: String, unit: String? = null) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall.tabularNums,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (unit != null) {
                Text(
                    text = unit,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 3.dp),
                )
            }
        }
    }
}

/**
 * The three macros as equal columns, each under its own fixed dot.
 *
 * **The dots are not a choice made here.** Protein is `primary`, carbs `tertiary`, fat `secondary`
 * in every macro bar, chart and legend in this app, and a card that swapped two of them would
 * teach the user the wrong mapping on the one surface where they are agreeing to the figures.
 */
@Composable
internal fun MacroColumns(proteinG: Int, carbsG: Int, fatG: Int, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        MacroColumn(
            label = stringResource(R.string.coach_macro_protein),
            grams = proteinG,
            dot = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f),
        )
        MacroColumn(
            label = stringResource(R.string.coach_macro_carbs),
            grams = carbsG,
            dot = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.weight(1f),
        )
        MacroColumn(
            label = stringResource(R.string.coach_macro_fat),
            grams = fatG,
            dot = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun MacroColumn(label: String, grams: Int, dot: Color, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            // A square rather than a circle, matching `MacroBar`'s hard-edged segments.
            Box(modifier = Modifier.size(8.dp).background(dot, RoundedCornerShape(2.dp)))
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = stringResource(R.string.coach_macro_grams, grams),
            style = MaterialTheme.typography.titleMedium.tabularNums,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

/**
 * One row of a several-row draft, on its own white cell.
 *
 * A cell per row rather than rows on one panel, so the 48dp `✕` has a boundary instead of crowding
 * its neighbour — a removal is irreversible enough to want a target you can aim at, and the undo
 * line exists because "enough" is not "completely".
 *
 * The macros shrink to coloured letters here, which is the one place in the app they do: three
 * full words per row over four rows is the legend four times, and the letters still carry the
 * mapping in the colour the legend under the total repeats in full.
 */
@Composable
internal fun ReceiptRow(
    name: String,
    detail: String?,
    macros: Triple<Int, Int, Int>?,
    trailing: String?,
    onRemove: (() -> Unit)?,
    removeLabel: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .heightIn(min = 56.dp)
                .padding(start = 12.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (macros != null) {
                    MacroLetters(macros)
                } else if (detail != null) {
                    Text(
                        text = detail,
                        style = MaterialTheme.typography.labelMedium.tabularNums,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (trailing != null) {
                Text(
                    text = trailing,
                    style = MaterialTheme.typography.titleMedium.tabularNums,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.End,
                )
            }
            if (onRemove != null) {
                IconButton(onClick = onRemove) {
                    Icon(
                        imageVector = AppIcons.Close,
                        contentDescription = removeLabel,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/** `P 14g · C 2g · F 17g`, each letter in its macro's own colour. */
@Composable
private fun MacroLetters(macros: Triple<Int, Int, Int>) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        MacroLetter(R.string.coach_macro_letter_protein, macros.first, MaterialTheme.colorScheme.primary)
        MacroLetter(R.string.coach_macro_letter_carbs, macros.second, MaterialTheme.colorScheme.tertiary)
        MacroLetter(R.string.coach_macro_letter_fat, macros.third, MaterialTheme.colorScheme.secondary)
    }
}

@Composable
private fun MacroLetter(letter: Int, grams: Int, color: Color) {
    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = stringResource(letter),
            style = MaterialTheme.typography.labelMedium,
            color = color,
        )
        Text(
            text = stringResource(R.string.coach_macro_grams, grams),
            style = MaterialTheme.typography.labelMedium.tabularNums,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** What the surviving rows come to, under a rule, with the macro mapping repeated in full — the
 * letters above are shorthand and a legend is what makes shorthand readable. */
@Composable
internal fun ReceiptTotal(count: Int, calories: Int, proteinG: Int, carbsG: Int, fatG: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.tertiary)
        ReceiptHeadline(
            label = stringResource(R.string.coach_proposal_total, count),
            value = calories.toString(),
            unit = stringResource(R.string.coach_unit_kcal),
        )
        MacroColumns(proteinG = proteinG, carbsG = carbsG, fatG = fatG)
    }
}

@PreviewLightDark
@Composable
private fun ReceiptPanelPreview() {
    AppTheme {
        Surface {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ReceiptPanel {
                    ReceiptHeadline(
                        label = stringResource(R.string.coach_receipt_calories),
                        value = "420",
                        unit = stringResource(R.string.coach_unit_kcal),
                    )
                    MacroColumns(proteinG = 22, carbsG = 31, fatG = 23)
                }
                ReceiptRow(
                    name = "Scrambled eggs",
                    detail = null,
                    macros = Triple(14, 2, 17),
                    trailing = "220",
                    onRemove = {},
                    removeLabel = "Remove Scrambled eggs",
                )
            }
        }
    }
}

