package ph.mart.healthapp.feature.home.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import ph.mart.healthapp.core.data.fasting.FastSession
import ph.mart.healthapp.core.data.fasting.durationMillis
import ph.mart.healthapp.core.data.fasting.formatClockTime
import ph.mart.healthapp.core.data.fasting.formatElapsed
import ph.mart.healthapp.core.data.fasting.goalReachedMillis
import ph.mart.healthapp.core.designsystem.component.AppCard
import ph.mart.healthapp.core.designsystem.component.DiscardConfirmDialog
import ph.mart.healthapp.core.designsystem.component.PrimaryButton
import ph.mart.healthapp.core.designsystem.component.TextButton
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.core.designsystem.theme.tabularNums
import ph.mart.healthapp.feature.home.R

private const val TICK_MILLIS = 1_000L
private val BAR_HEIGHT = 4.dp

/**
 * Start and end a fast, and watch it run. The whole card is two states off one nullable session:
 * `null` is the invitation, anything else is the timer.
 *
 * It is the one card whose **width moves with its state**, which is why the caller asks
 * `isHalf(fastRunning)` rather than reading a fixed table. A running fast owns a timer, a goal bar
 * and two buttons and needs the row; an idle one is a label and a Start button, which is exactly
 * the shape every other half card has — so it borrows [MetricCard] rather than drawing a zeroed
 * timer nobody started.
 *
 * Start/end are inline controls rather than a sheet — the same shape [WaterCard] uses — so this
 * card adds no navigation level and needs no `NavigationEventHandler`. The one dialog is the
 * shared [DiscardConfirmDialog], because throwing away a running fast is the one action here that
 * can't be undone by repeating it.
 *
 * Feature-local rather than `:core:designsystem`: Home is the only screen that starts a fast.
 */
@Composable
fun FastingCard(
    activeFast: FastSession?,
    goalHours: Int,
    onStart: () -> Unit,
    onEnd: () -> Unit,
    onDiscard: () -> Unit,
    wide: Boolean,
    modifier: Modifier = Modifier,
    /** Fixed in previews, which have no coroutine clock to tick against. */
    nowMillisOverride: Long? = null,
) {
    if (activeFast == null) {
        MetricCard(
            label = stringResource(R.string.home_fasting_title),
            value = stringResource(R.string.home_fasting_none),
            wide = wide,
            modifier = modifier,
        ) {
            MetaButton(label = stringResource(R.string.home_fasting_start), onClick = onStart)
        }
    } else {
        ActiveCard(
            fast = activeFast,
            onEnd = onEnd,
            onDiscard = onDiscard,
            nowMillisOverride = nowMillisOverride,
            modifier = modifier,
        )
    }
}

@Composable
private fun ActiveCard(
    fast: FastSession,
    onEnd: () -> Unit,
    onDiscard: () -> Unit,
    nowMillisOverride: Long?,
    modifier: Modifier = Modifier,
) {
    // Ticks only while a fast is open — the composable isn't reached otherwise, so there is no
    // idle timer to leak. Keyed on the session so ending one and starting another restarts it.
    val ticked by produceState(initialValue = System.currentTimeMillis(), fast.id) {
        while (true) {
            value = System.currentTimeMillis()
            delay(TICK_MILLIS)
        }
    }
    val now = nowMillisOverride ?: ticked
    val reached = now >= fast.goalReachedMillis

    AppCard(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.home_fasting_title),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    // The one figure on this card the app can call: the goal the user set, met.
                    StatusDot(if (reached) StatusMark.OnTrack else StatusMark.None)
                }
                Text(
                    text = stringResource(R.string.home_fasting_goal, fast.goalHours),
                    style = MaterialTheme.typography.labelMedium.tabularNums,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom,
            ) {
                Text(
                    text = formatElapsed(fast.durationMillis(now)),
                    style = MaterialTheme.typography.headlineMedium.tabularNums,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = if (reached) {
                        stringResource(R.string.home_fasting_reached, formatClockTime(fast.startMillis))
                    } else {
                        stringResource(
                            R.string.home_fasting_running,
                            formatClockTime(fast.startMillis),
                            formatClockTime(fast.goalReachedMillis),
                        )
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
            }

            GoalBar(
                // A lambda, not a Float: the per-second tick is then read inside the draw lambda and
                // settles in the Draw phase instead of recomposing the card once a second.
                progress = {
                    val span = (fast.goalReachedMillis - fast.startMillis).coerceAtLeast(1L)
                    ((now - fast.startMillis).toFloat() / span).coerceIn(0f, 1f)
                },
            )

            var confirmDiscard by remember { mutableStateOf(false) }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PrimaryButton(label = stringResource(R.string.home_fasting_end), onClick = onEnd)
                TextButton(label = stringResource(R.string.home_fasting_discard), onClick = { confirmDiscard = true })
            }
            if (confirmDiscard) {
                DiscardConfirmDialog(
                    title = stringResource(R.string.home_fasting_discard_title),
                    body = stringResource(R.string.home_fasting_discard_body),
                    confirmLabel = stringResource(R.string.home_fasting_discard),
                    dismissLabel = stringResource(R.string.home_fasting_discard_keep),
                    onConfirm = {
                        confirmDiscard = false
                        onDiscard()
                    },
                    onDismiss = { confirmDiscard = false },
                )
            }
        }
    }
}

/** Rounded track and fill, drawn rather than composed so the ticking progress never invalidates
 * composition. `primary` throughout — a fast in progress is on-track, not a warning. */
@Composable
private fun GoalBar(progress: () -> Float, modifier: Modifier = Modifier) {
    val trackColor = MaterialTheme.colorScheme.surfaceContainerHigh
    val fillColor = MaterialTheme.colorScheme.primary
    Canvas(modifier = modifier.fillMaxWidth().height(BAR_HEIGHT)) {
        val radius = CornerRadius(size.height / 2f)
        drawRoundRect(color = trackColor, size = size, cornerRadius = radius)
        val width = size.width * progress()
        if (width > 0f) {
            drawRoundRect(
                color = fillColor,
                size = Size(width.coerceAtLeast(size.height), size.height),
                cornerRadius = radius,
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun FastingCardPreview() {
    val now = 1_700_000_000_000L
    AppTheme {
        Surface {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(16.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    FastingCard(
                        activeFast = null,
                        goalHours = 16,
                        onStart = {},
                        onEnd = {},
                        onDiscard = {},
                        wide = false,
                        nowMillisOverride = now,
                        modifier = Modifier.weight(1f),
                    )
                    FastingCard(
                        activeFast = null,
                        goalHours = 16,
                        onStart = {},
                        onEnd = {},
                        onDiscard = {},
                        wide = true,
                        nowMillisOverride = now,
                        modifier = Modifier.weight(1f),
                    )
                }
                FastingCard(
                    activeFast = FastSession(id = 1, startMillis = now - 9 * 3_600_000L, goalHours = 16),
                    goalHours = 16,
                    onStart = {},
                    onEnd = {},
                    onDiscard = {},
                    wide = false,
                    nowMillisOverride = now,
                )
                FastingCard(
                    activeFast = FastSession(id = 2, startMillis = now - 17 * 3_600_000L, goalHours = 16),
                    goalHours = 16,
                    onStart = {},
                    onEnd = {},
                    onDiscard = {},
                    wide = false,
                    nowMillisOverride = now,
                )
            }
        }
    }
}
