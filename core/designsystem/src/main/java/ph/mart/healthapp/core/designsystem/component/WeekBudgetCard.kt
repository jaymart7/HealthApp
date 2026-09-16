package ph.mart.healthapp.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import ph.mart.healthapp.core.designsystem.R
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.core.designsystem.theme.tabularNums

/**
 * The week's calorie bank — what the days behind today left over, and what the days ahead can hold.
 * Every figure is derived in `core.data.food.weekBudget()`; this only formats, the treatment
 * [GoalProjectionLine] and [NutrientPanel] get.
 *
 * Plain Ints rather than that `WeekBudget` type, because this module is a leaf with no dependency
 * on `:core:data` (see its build file) — and it draws in two features, which is what puts it here
 * rather than in either one.
 *
 * Two things it deliberately does not do:
 *
 * - **No status dot.** `CLAUDE.md` fixes the 8dp mark to the four cards where on-track is a fact
 *   the app measures, "and nowhere else"; this card is a fifth reading of one of them.
 * - **No colour on the figure.** Under budget is on track for Lose and off track for Build, and
 *   the trend-arrow rule forbids defaulting to green-for-under. The one colour it spends is
 *   `error`, on the floor line, which is the same rule [NutrientPanel] follows for a limit.
 */
@Composable
fun WeekBudgetCard(
    bankedKcal: Int,
    daysCounted: Int,
    daysClosed: Int,
    daysLeft: Int,
    perDayKcal: Int,
    belowFloor: Boolean,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    AppCard(modifier = modifier, onClick = onClick) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.ds_week_budget_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(R.string.ds_week_budget_span),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Nothing behind today has been logged: a fresh Monday, or a week nobody logged. The
            // card still draws — the line below it is the week's allowance, which is true either
            // way — but it says the bank is empty rather than printing a confident 0.
            if (daysCounted == 0) {
                Text(
                    text = stringResource(R.string.ds_week_budget_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        // The sign is carried by the word beside it, not by a glyph — "520 kcal
                        // over" reads as a sentence where "−520" reads as a temperature.
                        text = "${abs(bankedKcal)}",
                        style = MaterialTheme.typography.headlineMedium.tabularNums,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = stringResource(
                            if (bankedKcal < 0) R.string.ds_week_budget_over else R.string.ds_week_budget_banked,
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 4.dp),
                    )
                }
                // What the figure counted, so a week with four days in it never reads as seven.
                Text(
                    text = stringResource(R.string.ds_week_budget_days, daysCounted, daysClosed),
                    style = MaterialTheme.typography.labelSmall.tabularNums,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Text(
                text = stringResource(
                    R.string.ds_week_budget_even,
                    pluralStringResource(R.plurals.ds_week_budget_left, daysLeft, daysLeft),
                    perDayKcal,
                ),
                style = MaterialTheme.typography.labelSmall.tabularNums,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (belowFloor) {
                Text(
                    text = stringResource(R.string.ds_week_budget_floor, perDayKcal),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun WeekBudgetCardPreview() {
    AppTheme {
        Surface {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(16.dp)) {
                WeekBudgetCard(
                    bankedKcal = 840,
                    daysCounted = 4,
                    daysClosed = 5,
                    daysLeft = 2,
                    perDayKcal = 2420,
                    belowFloor = false,
                    onClick = {},
                )
                WeekBudgetCard(
                    bankedKcal = -1300,
                    daysCounted = 6,
                    daysClosed = 6,
                    daysLeft = 1,
                    perDayKcal = 1500,
                    belowFloor = true,
                    onClick = {},
                )
                // A fresh Monday.
                WeekBudgetCard(
                    bankedKcal = 0,
                    daysCounted = 0,
                    daysClosed = 0,
                    daysLeft = 7,
                    perDayKcal = 2000,
                    belowFloor = false,
                    onClick = {},
                )
            }
        }
    }
}
