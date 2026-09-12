package ph.mart.healthapp.feature.training.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import ph.mart.healthapp.core.designsystem.component.SecondaryButton
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.core.designsystem.theme.tabularNums
import ph.mart.healthapp.feature.training.R

/** What "Skip" and a finished rest both leave behind: no rest running. */
internal const val NO_REST = 0L

/** The seconds a "+30 sec" adds. */
internal const val REST_EXTEND_SECONDS = 30

/** Off, then the four rests a programme actually uses. Off is a real choice, not a missing one —
 * a lifter who counts their own rest should not have to dismiss a card every set. */
internal val REST_CHOICES = listOf(0, 60, 90, 120, 180)

/** Rounds **up**, so a rest reads "1:30" for the whole of its first second rather than flicking to
 * 1:29 immediately, and reaches 0:00 only once it is genuinely over. Floored at zero: an end time
 * already past is simply not a rest. */
internal fun remainingSeconds(endAtMillis: Long, nowMillis: Long): Int {
    val left = endAtMillis - nowMillis
    if (left <= 0L) return 0
    return ((left + 999L) / 1000L).toInt()
}

/** `M:SS`. A figure rather than copy, which is why it stays in Kotlin — `RestTimerTest` is what
 * earns that, per the localization rule. */
internal fun formatRest(seconds: Int): String {
    val safe = seconds.coerceAtLeast(0)
    return "${safe / 60}:${(safe % 60).toString().padStart(2, '0')}"
}

/**
 * The rest between sets. Two states in one card: idle offers the durations, running counts one
 * down.
 *
 * It sits between the set list and the set editor because that is what it is about — the set just
 * added and the one about to be. It owns no workout data: a rest is not part of the session, so
 * nothing here reaches [ph.mart.healthapp.feature.training.ui.LogExerciseForm] and backing out of a
 * running rest asks no more than backing out of a still one.
 *
 * [endAtMillis] is wall-clock, not a tick count, so a rotation or a trip to another app resumes on
 * the time that actually remains rather than restarting. [durationSeconds] of 0 is Off.
 */
@Composable
internal fun RestTimerCard(
    endAtMillis: Long,
    durationSeconds: Int,
    onDurationChange: (Int) -> Unit,
    onExtend: () -> Unit,
    onSkip: () -> Unit,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.medium,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(12.dp)) {
            Text(
                text = stringResource(R.string.training_rest_title),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (endAtMillis > NO_REST) {
                RunningRest(
                    endAtMillis = endAtMillis,
                    durationSeconds = durationSeconds,
                    onExtend = onExtend,
                    onSkip = onSkip,
                    onFinished = onFinished,
                )
            } else {
                val offLabel = stringResource(R.string.training_rest_off)
                NameChipRow(
                    names = REST_CHOICES.map { if (it == 0) offLabel else formatRest(it) },
                    selected = if (durationSeconds == 0) offLabel else formatRest(durationSeconds),
                    onSelect = { label ->
                        onDurationChange(REST_CHOICES.first { label == if (it == 0) offLabel else formatRest(it) })
                    },
                )
            }
        }
    }
}

@Composable
private fun RunningRest(
    endAtMillis: Long,
    durationSeconds: Int,
    onExtend: () -> Unit,
    onSkip: () -> Unit,
    onFinished: () -> Unit,
) {
    var remaining by remember(endAtMillis) {
        mutableIntStateOf(remainingSeconds(endAtMillis, System.currentTimeMillis()))
    }
    val haptic = LocalHapticFeedback.current
    // `delay`, deliberately, not a frame clock: frames stop when the app is stopped and this must
    // keep counting with the phone face-down on the bench.
    //
    // ponytail: the cue is a buzz, and only while the process lives — a killed app loses the rest.
    // A notification-backed timer (AlarmManager + a channel) is the upgrade if either starts to
    // matter.
    LaunchedEffect(endAtMillis) {
        var left = remainingSeconds(endAtMillis, System.currentTimeMillis())
        // A rest that ran out while the screen was away has nothing left to announce.
        val announces = left > 0
        while (left > 0) {
            remaining = left
            delay(250)
            left = remainingSeconds(endAtMillis, System.currentTimeMillis())
        }
        remaining = 0
        if (announces) haptic.performHapticFeedback(HapticFeedbackType.Confirm)
        onFinished()
    }

    // Resolved here because a semantics lambda cannot read a resource — and it says only that a
    // rest is running, so a screen reader isn't handed a new number every second.
    val runningDescription = stringResource(R.string.training_rest_running)
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = formatRest(remaining),
            style = MaterialTheme.typography.titleLarge.tabularNums,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .weight(1f)
                .clearAndSetSemantics { contentDescription = runningDescription },
        )
        SecondaryButton(label = stringResource(R.string.training_rest_extend), onClick = onExtend)
        SecondaryButton(label = stringResource(R.string.training_rest_skip), onClick = onSkip)
    }
    LinearProgressIndicator(
        // Coerced because "+30 sec" can push the rest past the duration it started on.
        progress = { (remaining.toFloat() / durationSeconds.coerceAtLeast(1)).coerceIn(0f, 1f) },
        modifier = Modifier.fillMaxWidth(),
    )
}

/** Idle — the durations, with 1:30 chosen. */
@PreviewLightDark
@Composable
private fun RestTimerCardPreview() {
    AppTheme {
        Surface {
            RestTimerCard(
                endAtMillis = NO_REST,
                durationSeconds = 90,
                onDurationChange = {},
                onExtend = {},
                onSkip = {},
                onFinished = {},
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

/** Running — what the card looks like for the minute and a half after "Add set". */
@PreviewLightDark
@Composable
private fun RestTimerCardRunningPreview() {
    AppTheme {
        Surface {
            RestTimerCard(
                endAtMillis = System.currentTimeMillis() + 72_000L,
                durationSeconds = 90,
                onDurationChange = {},
                onExtend = {},
                onSkip = {},
                onFinished = {},
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}
