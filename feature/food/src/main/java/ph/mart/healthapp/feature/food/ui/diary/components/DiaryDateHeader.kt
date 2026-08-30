package ph.mart.healthapp.feature.food.ui.diary.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.designsystem.icon.AppIcons
import ph.mart.healthapp.core.designsystem.component.formatEpochDay
import ph.mart.healthapp.core.designsystem.theme.AppTheme

/** "Today" / "Yesterday" / "Aug 27, 2026" — pure, so the two relative cases are testable. */
internal fun diaryDateLabel(epochDay: Long, today: Long): String = when (epochDay) {
    today -> "Today"
    today - 1 -> "Yesterday"
    else -> formatEpochDay(epochDay)
}

/**
 * Which day the diary is showing. Stepping is a day at a time; the label opens the calendar for
 * longer jumps. There is no forward step past [today] — the app has no notion of a planned meal.
 */
@Composable
internal fun DiaryDateHeader(
    selectedDate: Long,
    today: Long,
    onSelectDate: (Long) -> Unit,
    onOpenCalendar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        IconButton(onClick = { onSelectDate(selectedDate - 1) }) {
            Icon(AppIcons.ChevronLeft, contentDescription = "Previous day")
        }
        Surface(
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onOpenCalendar),
        ) {
            Text(
                text = diaryDateLabel(selectedDate, today),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            )
        }
        IconButton(
            onClick = { onSelectDate(selectedDate + 1) },
            enabled = selectedDate < today,
        ) {
            Icon(AppIcons.ChevronRight, contentDescription = "Next day")
        }
    }
}

@PreviewLightDark
@Composable
private fun DiaryDateHeaderPreview() {
    AppTheme {
        Surface {
            val today = 20_000L
            DiaryDateHeader(selectedDate = today, today = today, onSelectDate = {}, onOpenCalendar = {})
        }
    }
}

@PreviewLightDark
@Composable
private fun DiaryDateHeaderPastPreview() {
    AppTheme {
        Surface {
            val today = 20_000L
            DiaryDateHeader(selectedDate = today - 1, today = today, onSelectDate = {}, onOpenCalendar = {})
        }
    }
}
