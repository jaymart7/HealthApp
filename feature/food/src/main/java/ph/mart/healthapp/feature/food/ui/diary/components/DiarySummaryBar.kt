package ph.mart.healthapp.feature.food.ui.diary.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.data.food.DiaryTotals
import ph.mart.healthapp.core.designsystem.component.MacroBar
import ph.mart.healthapp.core.designsystem.component.Macros
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.core.designsystem.theme.tabularNums

/**
 * The day, at the top of the diary and pinned above the scroll.
 *
 * This is the only always-visible summary on the screen the user spends the most time in, and it
 * used to state the day in `bodySmall` — smaller than the "610 kcal" on any single row beneath it,
 * so the loudest number on the diary was one meal rather than the whole day. Home already gives
 * this exact figure display weight inside its calorie ring; this brings the diary up to the
 * expressive level its neighbour already reaches, in the same type scale rather than a new one.
 *
 * The bar beneath it fills as the day fills. The legend is not decoration: it is what keeps the
 * three macro colours from carrying their meaning alone, and it is where a macro past its goal is
 * reported, since the bar itself stops at full.
 */
@Composable
fun DiarySummaryBar(
    consumed: DiaryTotals,
    goalKcal: Int,
    proteinGoalG: Int,
    carbsGoalG: Int,
    fatGoalG: Int,
    modifier: Modifier = Modifier,
) {
    val remaining = goalKcal - consumed.calories
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            // Read as one phrase — "1584 left" — instead of a number and a stray word after it.
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.clearAndSetSemantics {
                    contentDescription = remainingAnnouncement(remaining)
                },
            ) {
                Text(
                    // headlineSmall is Poppins, the face this system gives to a screen's own
                    // heading — and on the diary this figure *is* the heading.
                    text = "${if (remaining < 0) -remaining else remaining}",
                    style = MaterialTheme.typography.headlineSmall.tabularNums,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    // Never `error`, and never `primary` for being under: a day is not a grade, and
                    // the Earned Red Rule keeps red for genuine failure rather than a direction.
                    text = if (remaining < 0) "over" else "left",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
            }
            Text(
                text = "${consumed.calories} / $goalKcal kcal",
                style = MaterialTheme.typography.bodySmall.tabularNums,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp),
            )
        }

        MacroBar(
            proteinG = proteinGoalG,
            carbsG = carbsGoalG,
            fatG = fatGoalG,
            consumed = Macros(consumed.proteinG, consumed.carbsG, consumed.fatG),
        )

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            MacroLegend("Protein", consumed.proteinG, proteinGoalG, MaterialTheme.colorScheme.primary)
            MacroLegend("Carbs", consumed.carbsG, carbsGoalG, MaterialTheme.colorScheme.tertiary)
            MacroLegend("Fat", consumed.fatG, fatGoalG, MaterialTheme.colorScheme.secondary)
        }
    }
}

/** Same shape as Home's macro legend, so the two screens report a macro the same way. */
@Composable
private fun MacroLegend(label: String, consumedG: Int, goalG: Int, color: Color) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.clearAndSetSemantics {
            contentDescription = "$label $consumedG of $goalG grams"
        },
    ) {
        Box(modifier = Modifier.size(8.dp).background(color, RoundedCornerShape(2.dp)))
        Text(
            text = "$label $consumedG/${goalG}g",
            style = MaterialTheme.typography.labelSmall.tabularNums,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium,
        )
    }
}

internal fun remainingAnnouncement(remaining: Int): String =
    if (remaining < 0) "${-remaining} kilocalories over" else "$remaining kilocalories left"

@PreviewLightDark
@Composable
private fun DiarySummaryBarPreview() {
    AppTheme {
        Surface {
            DiarySummaryBar(
                consumed = DiaryTotals(calories = 940, proteinG = 62, carbsG = 88, fatG = 31),
                goalKcal = 1941,
                proteinGoalG = 146,
                carbsGoalG = 194,
                fatGoalG = 65,
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

/** A day barely started: the bar has to read as an empty frame, not as a missing component. */
@PreviewLightDark
@Composable
private fun DiarySummaryBarEmptyPreview() {
    AppTheme {
        Surface {
            DiarySummaryBar(
                consumed = DiaryTotals(calories = 0, proteinG = 0, carbsG = 0, fatG = 0),
                goalKcal = 1941,
                proteinGoalG = 146,
                carbsGoalG = 194,
                fatGoalG = 65,
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

/** Past the goal — the case that must not read as a scolding. */
@PreviewLightDark
@Composable
private fun DiarySummaryBarOverPreview() {
    AppTheme {
        Surface {
            DiarySummaryBar(
                consumed = DiaryTotals(calories = 2153, proteinG = 151, carbsG = 190, fatG = 78),
                goalKcal = 1941,
                proteinGoalG = 146,
                carbsGoalG = 194,
                fatGoalG = 65,
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}
