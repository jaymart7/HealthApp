package ph.mart.healthapp.wear.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import ph.mart.healthapp.core.today.TodaySnapshot
import ph.mart.healthapp.wear.R

/**
 * The streak and the day's steps, on one line each and both omitted when they're zero — a streak
 * you haven't started and a watch that synced no steps are absences, not zeros. Home and the
 * widget drop them for the same reason.
 */
@Composable
internal fun StreakAndSteps(snapshot: TodaySnapshot, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        if (snapshot.streakDays > 0) {
            Text(
                text = stringResource(R.string.wear_today_streak, snapshot.streakDays),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        if (snapshot.steps > 0) {
            Text(
                text = stringResource(R.string.wear_today_steps, snapshot.steps),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * Nothing has ever been pushed to this watch, or the profile isn't finished — either way there is
 * no honest number to draw, so it says so instead of rendering a zero day the user has been
 * eating through. The phone is named because the phone is where the fix is.
 */
@Composable
internal fun NoDataMessage(message: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

/** Shown only when the pushed day has since ended — that is, when the phone has been out of range
 * across a midnight. Saying "yesterday" is what stops the numbers below reading as today's. */
@Composable
internal fun StaleNote(modifier: Modifier = Modifier) {
    Text(
        text = stringResource(R.string.wear_today_stale),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = modifier,
    )
}
