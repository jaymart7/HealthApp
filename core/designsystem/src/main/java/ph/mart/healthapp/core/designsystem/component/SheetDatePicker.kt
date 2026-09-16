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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDialog
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import ph.mart.healthapp.core.designsystem.R
import ph.mart.healthapp.core.designsystem.theme.AppTheme

/**
 * The date and time rows + swap-in-place calendar, shared by
 * LogWeightSheet/AddPhotoPreviewScreen/AddMeasurementSheet/LogCycleSheet.
 * [AnimatedContent] handles the height/slide animation natively — no manual pixel-height hack.
 * System back while the calendar is showing returns to [fields] one level, without closing the
 * whole sheet (registers its own [NavigationBackHandler] on top of [AppBottomSheet]'s) — that and
 * picking a day are the whole way back, the grid drawing no chrome of its own under the sheet's.
 *
 * **The clock is a dialog where the calendar is a panel**, and the asymmetry is deliberate. A
 * third swap-in state would turn [showingCalendar] into a three-way enum at four call sites and
 * four `listSaver`s, for a picker that needs none of it: a dialog's back dismisses the dialog and
 * leaves the sheet under it open, which is exactly the one-level step this app's predictive-back
 * rule asks for, and it comes for free. It is also why the clock's open flag is held here rather
 * than hoisted — callers hide their Save button behind [showingCalendar], and a dialog drawn over
 * the sheet needs no such hiding.
 *
 * [selectedMinuteOfDay] is minutes past local midnight (`core.data.nowMinuteOfDay`), never an
 * instant: the day beside it is picked separately and can be backdated, so a timestamp here would
 * be a second, driftable copy of it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SheetDatePicker(
    showingCalendar: Boolean,
    onShowCalendar: () -> Unit,
    onBackToFields: () -> Unit,
    selectedDate: Long,
    markedDates: Set<Long>,
    onSelectDate: (Long) -> Unit,
    selectedMinuteOfDay: Int,
    onSelectTime: (Int) -> Unit,
    modifier: Modifier = Modifier,
    maxDate: Long = todayEpochDay(),
    fields: @Composable ColumnScope.() -> Unit,
) {
    if (showingCalendar) {
        val navigationState = rememberNavigationEventState(currentInfo = NavigationEventInfo.None)
        NavigationBackHandler(state = navigationState, onBackCompleted = onBackToFields)
    }
    var showingClock by rememberSaveable { mutableStateOf(false) }

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
        label = "sheetDatePicker",
    ) { isCalendar ->
        if (isCalendar) {
            CalendarPanel(
                selectedDate = selectedDate,
                markedDates = markedDates,
                maxDate = maxDate,
                onSelectDate = onSelectDate,
            )
        } else {
            Column {
                PickerRow(
                    label = stringResource(R.string.ds_date),
                    value = formatEpochDay(selectedDate),
                    onClick = onShowCalendar,
                )
                Spacer(modifier = Modifier.height(8.dp))
                PickerRow(
                    label = stringResource(R.string.ds_time),
                    value = formatMinuteOfDay(selectedMinuteOfDay),
                    onClick = { showingClock = true },
                )
                fields()
            }
        }
    }

    // Outside the AnimatedContent, so a transition running underneath cannot tear it down.
    if (showingClock) {
        val clock = rememberTimePickerState(
            initialHour = selectedMinuteOfDay / 60,
            initialMinute = selectedMinuteOfDay % 60,
        )
        TimePickerDialog(
            onDismissRequest = { showingClock = false },
            title = { Text(text = stringResource(R.string.ds_time)) },
            confirmButton = {
                TextButton(
                    label = stringResource(R.string.ds_time_set),
                    onClick = {
                        onSelectTime(clock.hour * 60 + clock.minute)
                        showingClock = false
                    },
                )
            },
            dismissButton = {
                TextButton(label = stringResource(R.string.ds_cancel), onClick = { showingClock = false })
            },
        ) {
            TimePicker(state = clock)
        }
    }
}

/** One tappable `label … value` row. Two of them: the date, and the time beside it. */
@Composable
private fun PickerRow(label: String, value: String, onClick: () -> Unit) {
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
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = value,
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
                    selectedMinuteOfDay = 6 * 60 + 30,
                    onSelectTime = {},
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
