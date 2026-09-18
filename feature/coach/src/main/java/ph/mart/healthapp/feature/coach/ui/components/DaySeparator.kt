package ph.mart.healthapp.feature.coach.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.data.todayEpochDay
import ph.mart.healthapp.core.designsystem.component.formatDayMonth
import ph.mart.healthapp.core.designsystem.component.formatWeekday
import ph.mart.healthapp.core.designsystem.icon.AppIcons
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.coach.R

/**
 * The line between one day of the conversation and the next.
 *
 * A ruled, centred label rather than a timestamp on every bubble: the question is almost never
 * *when* a turn happened but *which day it belongs to*, and forty small grey times down the side of
 * a transcript is forty things to read past. It is also the shape [StoppedMarker] borrows, which is
 * the point of splitting it out — something that happened *to* the conversation reads the same way
 * whether it was midnight or a stop button.
 */
@Composable
internal fun DaySeparator(epochDay: Long, modifier: Modifier = Modifier) {
    RuledLabel(label = dayLabel(epochDay), modifier = modifier)
}

/**
 * Today, yesterday, or the weekday it fell on.
 *
 * A weekday rather than a date for anything inside the last week, because that is how anyone talks
 * about a recent day; past that the weekday stops being unique and the date takes over.
 * `formatWeekday`/`formatDayMonth` are `:core:designsystem`'s, so a separator and the diary's own
 * header cannot start writing the same day two ways.
 */
@Composable
private fun dayLabel(epochDay: Long): String {
    val today = todayEpochDay()
    return when {
        epochDay == today -> stringResource(R.string.coach_day_today)
        epochDay == today - 1 -> stringResource(R.string.coach_day_yesterday)
        epochDay > today - 7 -> formatWeekday(epochDay)
        else -> formatDayMonth(epochDay)
    }
}

/** A turn the user stopped, marked where it happened. Not a failure notice: nothing went wrong, so
 * it takes the day separator's shape — something that happened to the conversation — rather than
 * the bordered notice a failed turn gets. */
@Composable
internal fun StoppedMarker(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        RuledLabel(
            label = stringResource(R.string.coach_stopped_label),
            icon = AppIcons.StopCircle,
        )
        Text(
            text = stringResource(R.string.coach_stopped_body),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 280.dp),
        )
    }
}

/** The shared shape: a centred uppercase label with a rule running out to each edge. */
@Composable
private fun RuledLabel(label: String, modifier: Modifier = Modifier, icon: ImageVector? = null) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant,
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp),
                )
            }
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant,
        )
    }
}

@PreviewLightDark
@Composable
private fun DaySeparatorPreview() {
    AppTheme {
        Surface {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                DaySeparator(epochDay = todayEpochDay() - 3)
                DaySeparator(epochDay = todayEpochDay())
                StoppedMarker()
            }
        }
    }
}
