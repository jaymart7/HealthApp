package ph.mart.healthapp.feature.food.ui.diary.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import ph.mart.healthapp.core.data.food.DiaryTotals
import ph.mart.healthapp.core.designsystem.component.MacroBar
import ph.mart.healthapp.core.designsystem.component.Macros
import ph.mart.healthapp.core.designsystem.component.MicronutrientLegend
import ph.mart.healthapp.core.designsystem.icon.AppIcons
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.core.designsystem.theme.Motion
import ph.mart.healthapp.core.designsystem.theme.tabularNums
import ph.mart.healthapp.feature.food.R

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
 *
 * [collapsed] is the scrolled state — the same figures at a size that leaves a food row on screen.
 * It is a *display* state and nothing else: no figure is dropped that the user cannot get back by
 * scrolling to the top, and the remaining figure is present in both, because that is the one number
 * this bar exists to say.
 *
 * **No `error` role anywhere in here.** Over budget flips the word and clamps the macro fills; a
 * day is not a grade, and the Earned Red Rule keeps red for genuine failure rather than a
 * direction.
 */
@Composable
fun DiarySummaryBar(
    consumed: DiaryTotals,
    goalKcal: Int,
    proteinGoalG: Int,
    carbsGoalG: Int,
    fatGoalG: Int,
    modifier: Modifier = Modifier,
    collapsed: Boolean = false,
    burnedKcal: Int = 0,
    exerciseCredited: Boolean = false,
) {
    val remaining = goalKcal - consumed.calories
    Crossfade(
        targetState = collapsed,
        animationSpec = tween(durationMillis = Motion.State, easing = Motion.EmphasizedDecelerate),
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(tween(durationMillis = Motion.State, easing = Motion.EmphasizedDecelerate)),
        label = "diarySummary",
    ) { isCollapsed ->
        if (isCollapsed) {
            CollapsedSummary(consumed = consumed, goalKcal = goalKcal, remaining = remaining, proteinGoalG = proteinGoalG, carbsGoalG = carbsGoalG, fatGoalG = fatGoalG)
        } else {
            FullSummary(
                consumed = consumed,
                goalKcal = goalKcal,
                remaining = remaining,
                proteinGoalG = proteinGoalG,
                carbsGoalG = carbsGoalG,
                fatGoalG = fatGoalG,
                burnedKcal = burnedKcal,
                exerciseCredited = exerciseCredited,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FullSummary(
    consumed: DiaryTotals,
    goalKcal: Int,
    remaining: Int,
    proteinGoalG: Int,
    carbsGoalG: Int,
    fatGoalG: Int,
    burnedKcal: Int,
    exerciseCredited: Boolean,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            RemainingLine(
                remaining = remaining,
                consumedKcal = consumed.calories,
                goalKcal = goalKcal,
                // headlineSmall is Poppins, the face this system gives to a screen's own heading —
                // and on the diary this figure *is* the heading.
                figureStyle = MaterialTheme.typography.headlineSmall,
                wordStyle = MaterialTheme.typography.bodyLarge,
            )

            MacroBar(
                proteinG = proteinGoalG,
                carbsG = carbsGoalG,
                fatG = fatGoalG,
                consumed = Macros(consumed.proteinG, consumed.carbsG, consumed.fatG),
                height = 8.dp,
            )

            // FlowRow, not Row: at a large font scale three "Protein 62/146g" labels are wider
            // than the screen, and a legend that clipped its third item would drop fat entirely —
            // the one macro the bar's own colour cannot be read without.
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                MacroLegend(stringResource(R.string.food_macro_protein), consumed.proteinG, proteinGoalG, MaterialTheme.colorScheme.primary)
                MacroLegend(stringResource(R.string.food_macro_carbs), consumed.carbsG, carbsGoalG, MaterialTheme.colorScheme.tertiary)
                MacroLegend(stringResource(R.string.food_macro_fat), consumed.fatG, fatGoalG, MaterialTheme.colorScheme.secondary)
            }

            // Silent on a day nothing was logged for, and on one logged entirely by quick add —
            // which is why it sits outside the legend Row rather than as a fourth item inside it.
            MicronutrientLegend(
                fiberG = consumed.fiberG,
                sugarG = consumed.sugarG,
                sodiumMg = consumed.sodiumMg,
            )

            // The consumed line above already reads against goal + burn, which silently grew the
            // budget with no word for why. This is that word. It is hidden when the profile's
            // "add exercise to budget" switch is off, not just when nothing was burned: with the
            // switch off the burn is genuinely *not* added, and saying it was would be a lie the
            // arithmetic contradicts.
            if (burnedKcal > 0 && exerciseCredited) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = AppIcons.Run,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = stringResource(R.string.food_summary_exercise_credit, burnedKcal),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        HorizontalDivider(
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

/**
 * The scrolled state. Everything dropped here is either restated below (the macros, by each
 * section's own subtotal) or recoverable by scrolling back to the top — except the remaining
 * figure and the bar, which are what the bar is *for* and so survive at a smaller size.
 */
@Composable
private fun CollapsedSummary(
    consumed: DiaryTotals,
    goalKcal: Int,
    remaining: Int,
    proteinGoalG: Int,
    carbsGoalG: Int,
    fatGoalG: Int,
) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        RemainingLine(
            remaining = remaining,
            consumedKcal = consumed.calories,
            goalKcal = goalKcal,
            figureStyle = MaterialTheme.typography.titleLarge,
            wordStyle = MaterialTheme.typography.bodySmall,
        )
        MacroBar(
            proteinG = proteinGoalG,
            carbsG = carbsGoalG,
            fatG = fatGoalG,
            consumed = Macros(consumed.proteinG, consumed.carbsG, consumed.fatG),
            height = 4.dp,
        )
    }
}

/**
 * "1584 left" and "940 / 1941 kcal", the one line both states draw.
 *
 * The figure counts to its new value rather than snapping, because a logged meal moving this number
 * is the confirmation that the entry landed. The **word is read off the animated figure too**, so
 * the two can never disagree mid-count and show "30 over" while the count is still positive; the
 * spoken announcement uses the settled value, since a screen reader has no business hearing a
 * number that is still moving.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RemainingLine(
    remaining: Int,
    consumedKcal: Int,
    goalKcal: Int,
    figureStyle: TextStyle,
    wordStyle: TextStyle,
) {
    val animated by animateIntAsState(
        targetValue = remaining,
        animationSpec = tween(durationMillis = Motion.Enter, easing = Motion.Standard),
        label = "remainingKcal",
    )
    // Resolved above the semantics lambda, which cannot read a resource.
    val announcement = remainingAnnouncement(remaining)
    // FlowRow so the two halves stack at a large font scale instead of the consumed line being
    // pushed off the edge. At the default scale it lays out exactly as the Row it replaced.
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        maxItemsInEachRow = 2,
    ) {
        // Read as one phrase — "1584 left" — instead of a number and a stray word after it.
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.clearAndSetSemantics {
                contentDescription = announcement
            },
        ) {
            Text(
                text = "${abs(animated)}",
                style = figureStyle.tabularNums,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                // Never `error`, and never `primary` for being under: a day is not a grade.
                text = stringResource(if (animated < 0) R.string.food_summary_over else R.string.food_summary_left),
                style = wordStyle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp),
            )
        }
        Text(
            text = stringResource(R.string.food_summary_of_goal, consumedKcal, goalKcal),
            style = MaterialTheme.typography.bodySmall.tabularNums,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 4.dp),
        )
    }
}

/** Same shape as Home's macro legend, so the two screens report a macro the same way. The colour
 * stays on the dot and the text stays `onSurfaceVariant`: three coloured strings on one card would
 * make the legend the loudest thing on a bar it is only annotating. */
@Composable
private fun MacroLegend(label: String, consumedG: Int, goalG: Int, color: Color) {
    val spoken = stringResource(R.string.food_macro_spoken, label, consumedG, goalG)
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.clearAndSetSemantics {
            contentDescription = spoken
        },
    ) {
        Box(modifier = Modifier.size(8.dp).background(color, RoundedCornerShape(2.dp)))
        Text(
            text = stringResource(R.string.food_macro_legend, label, consumedG, goalG),
            style = MaterialTheme.typography.labelSmall.tabularNums,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
internal fun remainingAnnouncement(remaining: Int): String =
    if (remaining < 0) {
        stringResource(R.string.food_kcal_over_spoken, -remaining)
    } else {
        stringResource(R.string.food_kcal_left_spoken, remaining)
    }

@PreviewLightDark
@Composable
private fun DiarySummaryBarPreview() {
    AppTheme {
        Surface {
            DiarySummaryBar(
                consumed = DiaryTotals(
                    calories = 940,
                    proteinG = 62,
                    carbsG = 88,
                    fatG = 31,
                    fiberG = 12,
                    sugarG = 40,
                    sodiumMg = 1240,
                ),
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
                consumed = DiaryTotals(calories = 2313, proteinG = 151, carbsG = 190, fatG = 78),
                goalKcal = 2061,
                proteinGoalG = 146,
                carbsGoalG = 194,
                fatGoalG = 65,
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

/** A day with a workout on it: the budget above is bigger than the target, and this is the only
 * line that says why. */
@PreviewLightDark
@Composable
private fun DiarySummaryBarCreditedPreview() {
    AppTheme {
        Surface {
            DiarySummaryBar(
                consumed = DiaryTotals(calories = 940, proteinG = 62, carbsG = 88, fatG = 31),
                goalKcal = 2304,
                proteinGoalG = 146,
                carbsGoalG = 194,
                fatGoalG = 65,
                burnedKcal = 363,
                exerciseCredited = true,
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

/** The scrolled state — the remaining figure is never absent, whatever else goes. */
@PreviewLightDark
@Composable
private fun DiarySummaryBarCollapsedPreview() {
    AppTheme {
        Surface {
            DiarySummaryBar(
                consumed = DiaryTotals(calories = 940, proteinG = 62, carbsG = 88, fatG = 31),
                goalKcal = 1941,
                proteinGoalG = 146,
                carbsGoalG = 194,
                fatGoalG = 65,
                collapsed = true,
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}
