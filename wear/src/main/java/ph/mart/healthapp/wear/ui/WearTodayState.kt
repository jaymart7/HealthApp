package ph.mart.healthapp.wear.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue

@Composable
internal fun rememberWearTodayState(): WearTodayScreenState =
    rememberSaveable(saver = WearTodayScreenState.Saver()) { WearTodayScreenState() }

/**
 * The screen's one UI-only flag: whether the "couldn't reach your phone" confirmation is up.
 * There is no form here — the watch edits nothing, it sends two taps — so this class holds
 * exactly that.
 */
internal class WearTodayScreenState(failureShown: Boolean = false) {
    var failureShown: Boolean by mutableStateOf(failureShown)

    companion object {
        fun Saver(): Saver<WearTodayScreenState, Any> = Saver(
            save = { it.failureShown },
            restore = { WearTodayScreenState(failureShown = it as Boolean) },
        )
    }
}
