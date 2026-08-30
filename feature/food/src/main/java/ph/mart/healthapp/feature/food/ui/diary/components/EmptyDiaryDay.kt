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
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.designsystem.component.MascotAvatar
import ph.mart.healthapp.core.designsystem.component.MascotState
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.food.ui.shared.defaultMealTypeForNow

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
 * The meal headers still render above and below this, so the "+" on each is reachable and nothing
 * here stands between the user and logging.
 */
@Composable
internal fun EmptyDiaryDay(isToday: Boolean, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MascotAvatar(state = MascotState.Idle, size = 48.dp)
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = if (isToday) "Nothing logged yet today." else "Nothing was logged on this day.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = if (isToday) {
                    "${defaultMealTypeForNow().name} is a good place to start."
                } else {
                    "You can still add to it."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** What a single empty section says once the day has *something* on it. The header directly above
 * already names the meal, so repeating the name four times down the screen was the noise. */
internal const val EMPTY_SECTION_LABEL = "Nothing here yet."

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
