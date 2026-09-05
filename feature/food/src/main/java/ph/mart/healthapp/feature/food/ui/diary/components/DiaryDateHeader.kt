package ph.mart.healthapp.feature.food.ui.diary.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.designsystem.component.AppTextField
import ph.mart.healthapp.core.designsystem.component.formatEpochDay
import ph.mart.healthapp.core.designsystem.icon.AppIcons
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.core.designsystem.theme.Motion
import ph.mart.healthapp.feature.food.R

/** "Today" / "Yesterday" / "Aug 27, 2026" — pure, so the two relative cases are testable, and
 * `DiaryDateHeaderTest` asserts both words. That test is what keeps the two of them in Kotlin,
 * the reading `StreakCard`'s `dayCountLabel` and `goalProjectionLine()` got. */
internal fun diaryDateLabel(epochDay: Long, today: Long): String = when (epochDay) {
    today -> "Today"
    today - 1 -> "Yesterday"
    else -> formatEpochDay(epochDay)
}

/**
 * Which day the diary is showing, and — folded into the same row — the filter over that day.
 *
 * Stepping is a day at a time; the label opens the calendar for longer jumps. There is no forward
 * step past [today]: the app has no notion of a planned meal, so the chevron stays present and
 * disabled rather than disappearing, because a control that vanishes at the edge of its range
 * teaches nothing about where the edge is. It is **still enabled on a past day** — that is how you
 * walk Tuesday forward to today.
 *
 * The filter used to be a pinned row of its own beneath this one, which cost the diary a third
 * pinned block for a control most days never touch. It is a 48dp icon here and becomes the row
 * when tapped, so the header's height is the same in both states and nothing below it moves.
 * Closing is the caller's job to make destructive — it clears the query, because a filter you can
 * no longer see is a filter you will not remember hiding rows behind.
 */
@Composable
internal fun DiaryDateHeader(
    selectedDate: Long,
    today: Long,
    onSelectDate: (Long) -> Unit,
    onOpenCalendar: () -> Unit,
    filterExpanded: Boolean,
    onFilterExpandedChange: (Boolean) -> Unit,
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Crossfade(
        targetState = filterExpanded,
        animationSpec = tween(durationMillis = Motion.Feedback, easing = Motion.Standard),
        modifier = modifier.fillMaxWidth(),
        label = "diaryFilter",
    ) { expanded ->
        Row(
            // The floor, not the height: at a large font scale the date label needs more than 48dp
            // and clipping it would be worse than a taller header.
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (expanded) {
                FilterField(query = query, onQueryChange = onQueryChange, onClose = { onFilterExpandedChange(false) })
            } else {
                DateControls(
                    selectedDate = selectedDate,
                    today = today,
                    onSelectDate = onSelectDate,
                    onOpenCalendar = onOpenCalendar,
                    onOpenFilter = { onFilterExpandedChange(true) },
                )
            }
        }
    }
}

@Composable
private fun RowScope.DateControls(
    selectedDate: Long,
    today: Long,
    onSelectDate: (Long) -> Unit,
    onOpenCalendar: () -> Unit,
    onOpenFilter: () -> Unit,
) {
    IconButton(onClick = { onSelectDate(selectedDate - 1) }, modifier = Modifier.size(48.dp)) {
        Icon(
            imageVector = AppIcons.ChevronLeft,
            contentDescription = stringResource(R.string.food_previous_day),
            tint = MaterialTheme.colorScheme.onSurface,
        )
    }
    // One 48dp button, not a label with a separate affordance beside it: the chevron is part of
    // what says "this opens something", so it has to be inside the target rather than next to it.
    Surface(
        onClick = onOpenCalendar,
        color = Color.Transparent,
        modifier = Modifier.weight(1f).heightIn(min = 48.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = diaryDateLabel(selectedDate, today),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
            Icon(
                imageVector = AppIcons.ChevronDown,
                // The button's own label already names the day; this glyph only says it opens.
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp),
            )
        }
    }
    IconButton(
        onClick = { onSelectDate(selectedDate + 1) },
        // Enabled on any past day — this is the way back to today. Only today itself is the edge.
        enabled = selectedDate < today,
        colors = IconButtonDefaults.iconButtonColors(
            contentColor = MaterialTheme.colorScheme.onSurface,
            disabledContentColor = MaterialTheme.colorScheme.outlineVariant,
        ),
        modifier = Modifier.size(48.dp),
    ) {
        Icon(AppIcons.ChevronRight, contentDescription = stringResource(R.string.food_next_day))
    }
    IconButton(onClick = onOpenFilter, modifier = Modifier.size(48.dp)) {
        Icon(
            imageVector = AppIcons.Filter,
            contentDescription = stringResource(R.string.food_filter_open),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun RowScope.FilterField(query: String, onQueryChange: (String) -> Unit, onClose: () -> Unit) {
    val focusRequester = remember { FocusRequester() }
    // Tapping the icon is the whole of the user's intent to type, so the field opens focused.
    // The requester sits on AppTextField's outer Column, which is not itself focusable — Compose
    // delegates the request to the first focus target beneath it, which is the field.
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    AppTextField(
        value = query,
        onValueChange = onQueryChange,
        // No visible label: the placeholder already says it, and AppTextField hands the
        // placeholder to the screen reader when there is no label.
        placeholder = stringResource(R.string.food_filter_placeholder),
        modifier = Modifier.weight(1f).focusRequester(focusRequester),
    )
    IconButton(onClick = onClose, modifier = Modifier.size(48.dp)) {
        Icon(
            imageVector = AppIcons.Close,
            contentDescription = stringResource(R.string.food_filter_close),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@PreviewLightDark
@Composable
private fun DiaryDateHeaderPreview() {
    AppTheme {
        Surface {
            val today = 20_000L
            DiaryDateHeader(
                selectedDate = today,
                today = today,
                onSelectDate = {},
                onOpenCalendar = {},
                filterExpanded = false,
                onFilterExpandedChange = {},
                query = "",
                onQueryChange = {},
                modifier = Modifier.padding(horizontal = 8.dp),
            )
        }
    }
}

/** A past day — the one state where the forward chevron is live. */
@PreviewLightDark
@Composable
private fun DiaryDateHeaderPastPreview() {
    AppTheme {
        Surface {
            val today = 20_000L
            DiaryDateHeader(
                selectedDate = today - 1,
                today = today,
                onSelectDate = {},
                onOpenCalendar = {},
                filterExpanded = false,
                onFilterExpandedChange = {},
                query = "",
                onQueryChange = {},
                modifier = Modifier.padding(horizontal = 8.dp),
            )
        }
    }
}

/** The filter open, in the same 48dp the date row occupies — nothing below it may move. */
@PreviewLightDark
@Composable
private fun DiaryDateHeaderFilteringPreview() {
    AppTheme {
        Surface {
            val today = 20_000L
            DiaryDateHeader(
                selectedDate = today,
                today = today,
                onSelectDate = {},
                onOpenCalendar = {},
                filterExpanded = true,
                onFilterExpandedChange = {},
                query = "yog",
                onQueryChange = {},
                modifier = Modifier.padding(horizontal = 8.dp),
            )
        }
    }
}
