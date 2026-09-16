package ph.mart.healthapp.feature.food.ui.shared.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.data.food.MealType
import ph.mart.healthapp.core.designsystem.component.AppCard
import ph.mart.healthapp.core.designsystem.component.MacroBar
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.core.designsystem.theme.tabularNums
import ph.mart.healthapp.feature.food.R
import ph.mart.healthapp.feature.food.ui.shared.AddEntryForm

/**
 * What the whole batch comes to, above the rows it is the sum of.
 *
 * The review screen used to show four rows of calories and no answer to the question the user
 * actually has, which is what this meal costs them. [MealTotal.of] is the one derivation and
 * [MealTotal.calories] is what the Log button's label quotes, so the headline figure and the
 * button can never disagree — the failure mode that makes a total worse than none.
 *
 * The bar is the app's existing [MacroBar] at its usual 8dp, which means protein `primary`, carbs
 * `tertiary`, fat `secondary`, exactly as every other macro bar in the product. The legend repeats
 * those three colours rather than inventing a key, and the row summaries below tint their letters
 * to match.
 */
@Composable
internal fun MealTotalCard(total: MealTotal, modifier: Modifier = Modifier) {
    AppCard(
        modifier = modifier,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = stringResource(R.string.food_review_this_meal),
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = "${total.calories}",
                    style = MaterialTheme.typography.headlineMedium.tabularNums.copy(
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(R.string.food_review_kcal_unit),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp, bottom = 4.dp),
                )
            }

            MacroBar(proteinG = total.proteinG, carbsG = total.carbsG, fatG = total.fatG)

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                MacroLegend(
                    label = stringResource(R.string.food_macro_protein),
                    grams = total.proteinG,
                    color = MaterialTheme.colorScheme.primary,
                )
                MacroLegend(
                    label = stringResource(R.string.food_macro_carbs),
                    grams = total.carbsG,
                    color = MaterialTheme.colorScheme.tertiary,
                )
                MacroLegend(
                    label = stringResource(R.string.food_macro_fat),
                    grams = total.fatG,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
        }
    }
}

@Composable
private fun MacroLegend(label: String, grams: Int, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Surface(color = color, shape = CircleShape, modifier = Modifier.size(8.dp)) {}
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = stringResource(R.string.food_review_grams, grams),
            style = MaterialTheme.typography.bodySmall.tabularNums.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

/**
 * The batch's four numbers, derived from the rows and nothing else.
 *
 * A value type rather than four parameters because two places read it and they must read the same
 * thing: the card above, and the Log button's label. Pure, so [MealTotalTest] can hold it —
 * [AddEntryForm]'s nullable figures mean "the user cleared it", which is zero toward a total, not
 * a reason to skip the row.
 */
internal data class MealTotal(
    val calories: Int = 0,
    val proteinG: Int = 0,
    val carbsG: Int = 0,
    val fatG: Int = 0,
) {
    companion object {
        fun of(items: List<AddEntryForm>): MealTotal = MealTotal(
            calories = items.sumOf { it.calories ?: 0 },
            proteinG = items.sumOf { it.proteinG ?: 0 },
            carbsG = items.sumOf { it.carbsG ?: 0 },
            fatG = items.sumOf { it.fatG ?: 0 },
        )
    }
}

private val PREVIEW_ITEMS = listOf(
    AddEntryForm(MealType.Breakfast, "Scrambled eggs", 2.0, "serving", 182, 13, 2, 14),
    AddEntryForm(MealType.Breakfast, "Toast", 1.0, "serving", 90, 3, 17, 1),
    AddEntryForm(MealType.Breakfast, "Black coffee", 1.0, "cup", 2, 0, 0, 0),
)

@PreviewLightDark
@Composable
private fun MealTotalCardPreview() {
    AppTheme {
        Surface {
            MealTotalCard(total = MealTotal.of(PREVIEW_ITEMS), modifier = Modifier.padding(16.dp))
        }
    }
}

/** One row, and one of its macros at zero — the bar has to stay a bar rather than collapsing to a
 * hairline, which is what [MacroBar]'s own floor is for. */
@PreviewLightDark
@Composable
private fun MealTotalCardSingleItemPreview() {
    AppTheme {
        Surface {
            MealTotalCard(
                total = MealTotal.of(listOf(PREVIEW_ITEMS[1])),
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}
