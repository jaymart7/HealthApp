package ph.mart.healthapp.feature.profile.ui.routine.components

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.data.DAYS_IN_WEEK
import ph.mart.healthapp.core.data.hasWeekday
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.core.designsystem.theme.tabularNums
import ph.mart.healthapp.feature.profile.R
import ph.mart.healthapp.feature.profile.ui.shared.components.WeekdayPicker

/**
 * A routine's week, inside its card under a full-bleed rule.
 *
 * The picker used to be a bordered strip glued to the bottom of a grey card — the most
 * interactive thing on the screen, looking like the least. Here it sits in a labelled zone that
 * belongs to the routine, with the count of planned days on the right where the figures on every
 * other row of this app sit.
 *
 * An **unscheduled** routine swaps that count for the one sentence that says what to do about it,
 * in `primary` — the only `primary` text on the screen, and paired with the picker's dashed
 * cells so the state is a shape as well as a sentence. Nothing is saved: toggling a day writes
 * the whole mask immediately, which is why there is no button here.
 */
@Composable
internal fun RoutinePlanZone(days: Int, onDaysChange: (Int) -> Unit, modifier: Modifier = Modifier) {
    val plannedCount = (0 until DAYS_IN_WEEK).count { days.hasWeekday(it) }
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 12.dp, top = 8.dp, end = 12.dp, bottom = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(R.string.profile_routine_plan).uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            if (plannedCount == 0) {
                Text(
                    text = stringResource(R.string.profile_routine_unscheduled),
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.primary,
                )
            } else {
                Text(
                    text = pluralStringResource(
                        R.plurals.profile_routine_days_a_week,
                        plannedCount,
                        plannedCount,
                    ),
                    style = MaterialTheme.typography.bodySmall.tabularNums,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        WeekdayPicker(days = days, onDaysChange = onDaysChange)
    }
}

@PreviewLightDark
@Composable
private fun RoutinePlanZonePreview() {
    AppTheme {
        Surface(color = MaterialTheme.colorScheme.surfaceContainerLow) {
            Column {
                RoutinePlanZone(days = 0b0010101, onDaysChange = {})
                RoutinePlanZone(days = 0, onDaysChange = {})
            }
        }
    }
}
