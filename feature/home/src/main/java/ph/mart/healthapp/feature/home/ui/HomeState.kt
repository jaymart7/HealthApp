package ph.mart.healthapp.feature.home.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue

@Composable
internal fun rememberHomeScreenState(): HomeScreenState =
    rememberSaveable(saver = HomeScreenState.Saver()) { HomeScreenState() }

/** UI-only — dismissing the insight card is a per-session presentation choice with no business
 * meaning, so it never reaches Room or [HomeUiState]. */
internal class HomeScreenState(insightDismissed: Boolean = false) {
    var insightDismissed: Boolean by mutableStateOf(insightDismissed)

    companion object {
        fun Saver(): Saver<HomeScreenState, Any> = listSaver(
            save = { listOf(it.insightDismissed) },
            restore = { saved -> HomeScreenState(insightDismissed = saved[0] as Boolean) },
        )
    }
}
