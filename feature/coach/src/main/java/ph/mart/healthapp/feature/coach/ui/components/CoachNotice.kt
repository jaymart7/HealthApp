package ph.mart.healthapp.feature.coach.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.designsystem.component.SecondaryButton
import ph.mart.healthapp.core.designsystem.component.TextButton
import ph.mart.healthapp.core.designsystem.icon.AppIcons
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.core.designsystem.theme.tabularNums
import ph.mart.healthapp.feature.coach.R
import ph.mart.healthapp.feature.coach.ui.CoachFailure

/**
 * A send that didn't come back. **Not a bubble** — and that is the whole of the redesign here.
 *
 * It used to be the mascot looking sleepy beside an apology, with the on-device fallback line drawn
 * in a speech bubble under it. Two things were wrong with that. The mascot speaking means *the
 * coach answered*, and the coach did not: nothing was read and nothing was written. And the
 * fallback is not the coach's sentence at all — it is three local rules over the diary, the same
 * ones Home falls back to — so putting it in the coach's own bubble was the app quietly passing off
 * its arithmetic as a model's answer.
 *
 * So: a bordered `surfaceContainerLow` notice at full content width, no avatar, no tail, no mascot,
 * and the fallback sits in a panel inside it that **says where it came from**.
 *
 * **No `error` colour on any of this.** A turn that didn't come back is not a crash, and red would
 * put the failure on the user's own data. `error` on this screen is one thing and one thing only:
 * "Clear chat" in the overflow.
 *
 * The two cases differ in temper, not in shape. Offline is a **state** — it will still be true in a
 * second, so the notice can afford to offer something to do meanwhile, and the fallback and the
 * door to the diary are both things that work with no network. A failed turn is an **event**: one
 * action, no fallback panel, because the honest answer is that the same question usually works on a
 * second try.
 */
@Composable
internal fun CoachNotice(
    failure: CoachFailure,
    onRetry: () -> Unit,
    onOpenDiary: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = if (failure.offline) AppIcons.CloudOff else AppIcons.SyncProblem,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp),
                )
                Text(
                    text = stringResource(
                        if (failure.offline) R.string.coach_notice_offline_heading
                        else R.string.coach_notice_failed_heading,
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            Text(
                text = stringResource(
                    if (failure.offline) R.string.coach_notice_offline_body
                    else R.string.coach_notice_failed_body,
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            // Only offline, and only when there is something to say: a failed turn has a working
            // connection, so the coach itself is the better second try.
            if (failure.offline && failure.insight != null) {
                FallbackPanel(insight = failure.insight)
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SecondaryButton(
                    label = stringResource(R.string.coach_retry),
                    onClick = onRetry,
                    icon = AppIcons.Refresh,
                )
                // The action that works with no network, offered where the one that doesn't just
                // failed. A failed turn gets no second action: there is nothing wrong with the
                // diary, so sending the user to it would be a shrug.
                if (failure.offline) {
                    TextButton(label = stringResource(R.string.coach_notice_open_diary), onClick = onOpenDiary)
                }
            }
        }
    }
}

/**
 * The rule-based line for the same day, **attributed**.
 *
 * Offline the app can still say something true about today, and it should — but it has to say who
 * worked it out. The eyebrow names the source and the caption underneath names the method, because
 * "FitPulse worked this out on your device" is the difference between a fallback and a lie.
 */
@Composable
private fun FallbackPanel(insight: String, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = stringResource(R.string.coach_notice_fallback_label).uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = insight,
                style = MaterialTheme.typography.bodyMedium.tabularNums,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(R.string.coach_notice_fallback_attribution),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Offline: the state, with the on-device line and the two things that still work. */
@PreviewLightDark
@Composable
private fun CoachNoticeOfflinePreview() {
    AppTheme {
        Surface {
            CoachNotice(
                failure = CoachFailure(
                    offline = true,
                    insight = "You're 88 g short on protein today.",
                    question = "How am I doing today?",
                ),
                onRetry = {},
                onOpenDiary = {},
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

/** Failed: the event, one action, nothing to fall back to. */
@PreviewLightDark
@Composable
private fun CoachNoticeFailedPreview() {
    AppTheme {
        Surface {
            CoachNotice(
                failure = CoachFailure(offline = false, insight = null, question = "How am I doing today?"),
                onRetry = {},
                onOpenDiary = {},
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}
