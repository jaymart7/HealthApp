package ph.mart.healthapp.feature.food.ui.voice.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.designsystem.component.FullScreenState
import ph.mart.healthapp.core.designsystem.component.MascotAvatar
import ph.mart.healthapp.core.designsystem.component.MascotState
import ph.mart.healthapp.core.designsystem.component.PrimaryButton
import ph.mart.healthapp.core.designsystem.theme.AppTheme

/** The sentence named nothing edible. A real answer with its own screen, not a failure — so it
 * offers the sentence back rather than a retry of the same words. */
@Composable
internal fun NoFoodHeardScreen(onEdit: () -> Unit) {
    FullScreenState(
        icon = { MascotAvatar(state = MascotState.Thinking, size = 64.dp) },
        heading = "No food in that one",
        body = "Try naming what you ate and roughly how much — \"two eggs and a slice of toast\".",
        actions = {
            PrimaryButton(label = "Edit what you said", onClick = onEdit, modifier = Modifier.fillMaxWidth())
        },
    )
}

/** The call didn't work. The words survive, so the retry is the same sentence again. */
@Composable
internal fun VoiceFailedScreen(onRetry: () -> Unit) {
    FullScreenState(
        icon = { MascotAvatar(state = MascotState.Sleepy, size = 64.dp) },
        heading = "That didn't work",
        body = "Couldn't work out what that adds up to. Your words are still there — try again.",
        actions = {
            PrimaryButton(label = "Try again", onClick = onRetry, modifier = Modifier.fillMaxWidth())
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
        heading = "No connection",
        body = if (retried) {
            "Still nothing. Reading a meal out of a sentence needs a connection — the diary's own " +
                "add button works offline."
        } else {
            "Reading a meal out of a sentence needs a connection. The diary's own add button works " +
                "offline, as does everything else."
        },
        actions = {
            PrimaryButton(label = "Try again", onClick = onRetry, modifier = Modifier.fillMaxWidth())
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
