package ph.mart.healthapp.feature.profile.ui.routine.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.data.exercise.DAYS_IN_WEEK
import ph.mart.healthapp.core.data.exercise.WEEKDAY_INITIALS
import ph.mart.healthapp.core.data.exercise.WEEKDAY_NAMES
import ph.mart.healthapp.core.data.exercise.hasWeekday
import ph.mart.healthapp.core.data.exercise.toggleWeekday
import ph.mart.healthapp.core.designsystem.theme.AppTheme

/**
 * Which weekdays a routine is planned for. Seven cells sharing the width equally — the
 * `SegmentedToggle` argument, and the one row in the app whose labels genuinely cannot be
 * shortened further.
 *
 * `M T W T F S S` repeats two letters, so the initial is decoration and the **full day name rides
 * a `contentDescription`**: a picker whose cells all read "T" to a screen reader is not a picker.
 *
 * Chips rather than switches: the whole point is reading the week at a glance, and seven rows of
 * switches is a screen, not a row.
 */
@Composable
internal fun WeekdayPicker(days: Int, onDaysChange: (Int) -> Unit, modifier: Modifier = Modifier) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        (0 until DAYS_IN_WEEK).forEach { index ->
            val selected = days.hasWeekday(index)
            Surface(
                onClick = { onDaysChange(days.toggleWeekday(index)) },
                shape = RoundedCornerShape(999.dp),
                color = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface,
                contentColor = if (selected) {
                    MaterialTheme.colorScheme.onSecondaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                border = BorderStroke(
                    width = if (selected) 1.5.dp else 1.dp,
                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .semantics {
                        contentDescription =
                            if (selected) "${WEEKDAY_NAMES[index]}, planned" else WEEKDAY_NAMES[index]
                    },
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(text = WEEKDAY_INITIALS[index], style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun WeekdayPickerPreview() {
    AppTheme {
        Surface {
            Box(modifier = Modifier.padding(16.dp)) {
                WeekdayPicker(days = 0b0010101, onDaysChange = {})
            }
        }
    }
}

/** Nothing planned — the state every routine saved before the plan existed is in. */
@PreviewLightDark
@Composable
private fun WeekdayPickerEmptyPreview() {
    AppTheme {
        Surface { WeekdayPicker(days = 0, onDaysChange = {}) }
    }
}
