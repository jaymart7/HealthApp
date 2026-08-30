package ph.mart.healthapp.core.designsystem.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import ph.mart.healthapp.core.designsystem.theme.AppTheme

/**
 * The date row + swap-in-place calendar, shared by LogWeightSheet/AddPhotoSheet/AddMeasurementSheet.
 * [AnimatedContent] handles the height/slide animation natively — no manual pixel-height hack.
 * System back while the calendar is showing returns to [fields] one level, without closing the
 * whole sheet (registers its own [NavigationBackHandler] on top of [AppBottomSheet]'s).
 */
@Composable
fun SheetDatePicker(
    showingCalendar: Boolean,
    onShowCalendar: () -> Unit,
    onBackToFields: () -> Unit,
    selectedDate: Long,
    markedDates: Set<Long>,
    onSelectDate: (Long) -> Unit,
    modifier: Modifier = Modifier,
    maxDate: Long = todayEpochDay(),
    fields: @Composable ColumnScope.() -> Unit,
) {
    if (showingCalendar) {
        val navigationState = rememberNavigationEventState(currentInfo = NavigationEventInfo.None)
        NavigationBackHandler(state = navigationState, onBackCompleted = onBackToFields)
    }

    AnimatedContent(
        targetState = showingCalendar,
        transitionSpec = {
            if (targetState) {
                (slideInHorizontally(tween(220)) { it } togetherWith slideOutHorizontally(tween(220)) { -it })
            } else {
                (slideInHorizontally(tween(220)) { -it } togetherWith slideOutHorizontally(tween(220)) { it })
            }
        },
        modifier = modifier,
        label = "SheetDatePicker",
    ) { isCalendar ->
        if (isCalendar) {
            CalendarPanel(
                selectedDate = selectedDate,
                markedDates = markedDates,
                maxDate = maxDate,
                onSelectDate = onSelectDate,
                onBack = onBackToFields,
            )
        } else {
            Column {
                DateRow(selectedDate = selectedDate, onClick = onShowCalendar)
                fields()
            }
        }
    }
}

@Composable
private fun DateRow(selectedDate: Long, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Date",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = formatEpochDay(selectedDate),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun SheetDatePickerFieldsPreview() {
    AppTheme {
        Surface {
            Column(modifier = Modifier.padding(16.dp)) {
                SheetDatePicker(
                    showingCalendar = false,
                    onShowCalendar = {},
                    onBackToFields = {},
                    selectedDate = todayEpochDay(),
                    markedDates = emptySet(),
                    onSelectDate = {},
                ) {
                    Text(
                        text = "Fields go here",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
            }
        }
    }
}
