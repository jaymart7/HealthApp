package ph.mart.healthapp.feature.food.ui.diary.components

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.designsystem.component.MascotAvatar
import ph.mart.healthapp.core.designsystem.component.MascotState
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.food.R
import ph.mart.healthapp.feature.food.ui.shared.defaultMealTypeForNow
import ph.mart.healthapp.feature.food.ui.shared.labelRes

/**
 * The day before anything is on it.
 *
 * A fresh diary used to be five identical grey lines — "Nothing logged for Breakfast yet." four
 * times over, then the same again for Exercise — which is the app's core screen at its single most
 * common starting state, and the one screen Bibo never appeared on at all. One mascot and one
 * sentence replace all five.
 *
 * The suggestion is the app's own [defaultMealTypeForNow] heuristic, the same one that preselects
 * a meal for a photo or a scan, so at 7pm it says Dinner rather than a fixed Breakfast. On a past
 * day there is no meal to suggest — you are looking at Tuesday, and "start with dinner" would be
 * advice about a day that has already gone.
 *
 * The meal cards still render above and below this — it sits between Lunch and Dinner rather than
 * above all four, so the day still reads as a day with a note in the middle of it rather than a
 * banner with four empty cards under it. Every "+" stays reachable and nothing here stands
 * between the user and logging.
 *
 * Sleepy, not Idle: the day has not started. It is also the only mascot state on this screen, so
 * the per-section "Nothing here yet." lines are suppressed while it is showing — see
 * [MealSection]'s `dayIsEmpty`, which is what finally made this block's own KDoc true.
 */
@Composable
internal fun EmptyDiaryDay(isToday: Boolean, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MascotAvatar(state = MascotState.Sleepy, size = 48.dp)
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = stringResource(if (isToday) R.string.food_empty_today else R.string.food_empty_past),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = if (isToday) {
                    stringResource(R.string.food_empty_start, stringResource(defaultMealTypeForNow().labelRes()))
                } else {
                    stringResource(R.string.food_empty_add)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun EmptyDiaryDayPreview() {
    AppTheme {
        Surface {
            EmptyDiaryDay(isToday = true)
        }
    }
}

/** A past day, where there is no meal worth suggesting. */
@PreviewLightDark
@Composable
private fun EmptyDiaryDayPastPreview() {
    AppTheme {
        Surface {
            EmptyDiaryDay(isToday = false)
        }
    }
}
