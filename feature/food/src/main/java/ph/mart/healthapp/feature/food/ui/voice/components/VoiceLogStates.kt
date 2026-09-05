package ph.mart.healthapp.feature.food.ui.voice.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.designsystem.component.FullScreenState
import ph.mart.healthapp.core.designsystem.component.MascotAvatar
import ph.mart.healthapp.core.designsystem.component.MascotState
import ph.mart.healthapp.core.designsystem.component.PrimaryButton
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.food.R

/** The sentence named nothing edible. A real answer with its own screen, not a failure — so it
 * offers the sentence back rather than a retry of the same words. */
@Composable
internal fun NoFoodHeardScreen(onEdit: () -> Unit) {
    FullScreenState(
        icon = { MascotAvatar(state = MascotState.Thinking, size = 64.dp) },
        heading = stringResource(R.string.food_voice_none_heading),
        body = stringResource(R.string.food_voice_none_body),
        actions = {
            PrimaryButton(label = stringResource(R.string.food_voice_edit), onClick = onEdit, modifier = Modifier.fillMaxWidth())
        },
    )
}

/** The call didn't work. The words survive, so the retry is the same sentence again. */
@Composable
internal fun VoiceFailedScreen(onRetry: () -> Unit) {
    FullScreenState(
        icon = { MascotAvatar(state = MascotState.Sleepy, size = 64.dp) },
        heading = stringResource(R.string.food_voice_failed_heading),
        body = stringResource(R.string.food_voice_failed_body),
        actions = {
            PrimaryButton(label = stringResource(R.string.food_try_again), onClick = onRetry, modifier = Modifier.fillMaxWidth())
        },
    )
}

/**
 * Checked before the call, `MealIdeasViewModel`'s rule: a `FirebaseAIException` cannot tell
 * "offline" from "that didn't work", and the screen says different things about them.
 *
 * No "Log manually" button, unlike `PhotoOfflineScreen`: that flow's manual door is a state inside
 * itself, while back out of this route lands on the diary, where the add-entry sheet is. Saying so
 * is cheaper than a button that only navigates.
 */
@Composable
internal fun VoiceOfflineScreen(retried: Boolean, onRetry: () -> Unit) {
    FullScreenState(
        icon = { MascotAvatar(state = MascotState.Sleepy, size = 64.dp) },
        heading = stringResource(R.string.food_no_connection),
        body = if (retried) {
            stringResource(R.string.food_voice_offline_retry)
        } else {
            stringResource(R.string.food_voice_offline)
        },
        actions = {
            PrimaryButton(label = stringResource(R.string.food_try_again), onClick = onRetry, modifier = Modifier.fillMaxWidth())
        },
    )
}

@PreviewLightDark
@Composable
private fun NoFoodHeardScreenPreview() {
    AppTheme { NoFoodHeardScreen(onEdit = {}) }
}

@PreviewLightDark
@Composable
private fun VoiceFailedScreenPreview() {
    AppTheme { VoiceFailedScreen(onRetry = {}) }
}

@PreviewLightDark
@Composable
private fun VoiceOfflineScreenPreview() {
    AppTheme { VoiceOfflineScreen(retried = false, onRetry = {}) }
}

/** The second look, after a retry that found nothing — a different sentence, same screen. */
@PreviewLightDark
@Composable
private fun VoiceOfflineScreenRetriedPreview() {
    AppTheme { VoiceOfflineScreen(retried = true, onRetry = {}) }
}
