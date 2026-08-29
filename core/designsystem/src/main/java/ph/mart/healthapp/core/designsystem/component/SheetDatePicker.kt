package ph.mart.healthapp.core.designsystem.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import ph.mart.healthapp.core.designsystem.icon.AppIcons
import ph.mart.healthapp.core.designsystem.theme.AppTheme

private const val MILLIS_PER_DAY = 86_400_000L

private fun epochDayToCalendar(epochDay: Long): Calendar = Calendar.getInstance().apply {
    timeInMillis = epochDay * MILLIS_PER_DAY
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}

private fun Calendar.toEpochDay(): Long = timeInMillis / MILLIS_PER_DAY

fun todayEpochDay(): Long = epochDayToCalendar(System.currentTimeMillis() / MILLIS_PER_DAY).toEpochDay()

fun epochDayToDate(epochDay: Long): java.util.Date = epochDayToCalendar(epochDay).time

fun formatEpochDay(epochDay: Long): String =
    SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(epochDayToDate(epochDay))

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

/**
 * The calendar half of [SheetDatePicker], public because the food diary shows the same grid on its
 * own — tapping the date header opens it in a sheet, with [onBack] closing that sheet.
 */
@Composable
fun CalendarPanel(
    selectedDate: Long,
    markedDates: Set<Long>,
    maxDate: Long,
    onSelectDate: (Long) -> Unit,
    onBack: () -> Unit,
) {
    var visibleMonth by remember { mutableStateOf(epochDayToCalendar(selectedDate)) }
    val maxMonthCal = epochDayToCalendar(maxDate)
    val atMaxMonth = visibleMonth.get(Calendar.YEAR) == maxMonthCal.get(Calendar.YEAR) &&
        visibleMonth.get(Calendar.MONTH) == maxMonthCal.get(Calendar.MONTH)

    Column {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            IconButton(onClick = onBack) {
                Icon(AppIcons.Back, contentDescription = "Back")
            }
            Text(
                text = "Select date",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 4.dp),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { visibleMonth = (visibleMonth.clone() as Calendar).apply { add(Calendar.MONTH, -1) } }) {
                Icon(AppIcons.ChevronLeft, contentDescription = "Previous month")
            }
            Text(
                text = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(visibleMonth.time),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            IconButton(
                onClick = { visibleMonth = (visibleMonth.clone() as Calendar).apply { add(Calendar.MONTH, 1) } },
                enabled = !atMaxMonth,
            ) {
                Icon(AppIcons.ChevronRight, contentDescription = "Next month")
            }
        }
        Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
            listOf("S", "M", "T", "W", "T", "F", "S").forEach { day ->
                Text(
                    text = day,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        val today = todayEpochDay()
        val monthGrid = remember(visibleMonth.get(Calendar.YEAR), visibleMonth.get(Calendar.MONTH)) {
            buildMonthGrid(visibleMonth)
        }
        monthGrid.chunked(7).forEach { week ->
            Row(modifier = Modifier.fillMaxWidth()) {
                week.forEach { dayEpoch ->
                    Box(modifier = Modifier.weight(1f).aspectRatio(1f), contentAlignment = Alignment.Center) {
                        if (dayEpoch != null) {
                            DayCell(
                                epochDay = dayEpoch,
                                isToday = dayEpoch == today,
                                isSelected = dayEpoch == selectedDate,
                                isFuture = dayEpoch > maxDate,
                                isMarked = dayEpoch in markedDates,
                                onClick = { onSelectDate(dayEpoch) },
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun buildMonthGrid(monthCalendar: Calendar): List<Long?> {
    val firstOfMonth = (monthCalendar.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, 1) }
    val leadingBlanks = firstOfMonth.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY
    val daysInMonth = firstOfMonth.getActualMaximum(Calendar.DAY_OF_MONTH)
    val days = (1..daysInMonth).map { day ->
        (firstOfMonth.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, day) }.toEpochDay()
    }
    val blanks = List<Long?>(leadingBlanks) { null }
    val grid = blanks + days
    return grid + List((7 - grid.size % 7) % 7) { null }
}

@Composable
private fun DayCell(
    epochDay: Long,
    isToday: Boolean,
    isSelected: Boolean,
    isFuture: Boolean,
    isMarked: Boolean,
    onClick: () -> Unit,
) {
    val dayOfMonth = epochDayToCalendar(epochDay).get(Calendar.DAY_OF_MONTH)
    val contentColor = when {
        isSelected -> MaterialTheme.colorScheme.onPrimary
        isFuture -> MaterialTheme.colorScheme.outlineVariant
        else -> MaterialTheme.colorScheme.onSurface
    }
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
            .then(
                if (isToday && !isSelected) Modifier.border(1.dp, MaterialTheme.colorScheme.primary, CircleShape) else Modifier,
            )
            .clickable(enabled = !isFuture, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = dayOfMonth.toString(), color = contentColor, style = MaterialTheme.typography.bodyMedium)
        if (isMarked && !isSelected) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 4.dp)
                    .size(4.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
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

@PreviewLightDark
@Composable
private fun SheetDatePickerCalendarPreview() {
    AppTheme {
        Surface {
            Column(modifier = Modifier.padding(16.dp)) {
                val today = todayEpochDay()
                SheetDatePicker(
                    showingCalendar = true,
                    onShowCalendar = {},
                    onBackToFields = {},
                    selectedDate = today,
                    markedDates = setOf(today - 2, today - 5),
                    onSelectDate = {},
                ) {}
            }
        }
    }
}
