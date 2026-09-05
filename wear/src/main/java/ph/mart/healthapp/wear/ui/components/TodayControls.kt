package ph.mart.healthapp.wear.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import ph.mart.healthapp.core.today.TodaySnapshot
import ph.mart.healthapp.core.today.waterGoalReached
import ph.mart.healthapp.wear.R
import ph.mart.healthapp.wear.ui.formatClockTime

/**
 * The watch's two writes. Both are one tap with no confirmation and no picker, which is the whole
 * argument for a watch app at all — anything that needs a number typed belongs on the phone.
 *
 * [enabled] is false while a tap is in flight to the phone: the watch has no database, so there
 * is nothing to show optimistically, and a second tap would send a second glass.
 */
@Composable
internal fun WaterButton(
    snapshot: TodaySnapshot,
    enabled: Boolean,
    onAddGlass: () -> Unit,
    modifier: Modifier = Modifier,
    transformation: SurfaceTransformation? = null,
) {
    val reached = snapshot.waterGoalReached
    Button(
        onClick = onAddGlass,
        // Disabled rather than hidden at the goal: the row is the day's water count first and a
        // control second, and a card that vanished on a good day would take the count with it.
        enabled = enabled && !reached,
        modifier = modifier,
        transformation = transformation,
        label = { Text(stringResource(if (reached) R.string.wear_water_goal_hit else R.string.wear_water_add_glass)) },
        secondaryLabel = {
            Text(
                text = "${snapshot.glasses} / ${snapshot.goalGlasses} · ${snapshot.waterLabel}",
                style = MaterialTheme.typography.labelSmall,
            )
        },
    )
}

/**
 * Start or stop, off the same snapshot the phone pushed — the watch never decides which; it sends
 * one message and the phone reads Room to find out what that meant. A wrist showing a fast that
 * was already ended on the phone therefore cannot end it twice.
 *
 * The target *time* is what's shown, not the elapsed hours: the snapshot carries a target instant
 * precisely so a surface that can't tick stays true, and this screen deliberately doesn't tick
 * either — a watch app is open for two seconds.
 */
@Composable
internal fun FastButton(
    snapshot: TodaySnapshot,
    enabled: Boolean,
    onToggleFast: () -> Unit,
    modifier: Modifier = Modifier,
    transformation: SurfaceTransformation? = null,
) {
    val until = snapshot.fastingUntilMillis
    Button(
        onClick = onToggleFast,
        enabled = enabled,
        modifier = modifier,
        transformation = transformation,
        label = { Text(stringResource(if (until == null) R.string.wear_fast_start else R.string.wear_fast_stop)) },
        secondaryLabel = {
            Text(
                text = when {
                    until == null -> stringResource(R.string.wear_fast_none)
                    snapshot.fastingGoalReached -> stringResource(R.string.wear_fast_goal_reached)
                    else -> stringResource(R.string.wear_fast_until, formatClockTime(until))
                },
                style = MaterialTheme.typography.labelSmall,
            )
        },
    )
}
