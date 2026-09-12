package ph.mart.healthapp.feature.progress.ui.recap

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue

@Composable
internal fun rememberRecapState(): RecapState = rememberSaveable(saver = RecapState.Saver) { RecapState() }

/** UI-only, and down to one flag: the period that used to sit beside it is what selects the report,
 * so it lives in [RecapViewModel]. Whether the share sheet is up means nothing outside this screen. */
internal class RecapState(sharing: Boolean = false) {
    var sharing: Boolean by mutableStateOf(sharing)

    companion object {
        val Saver: Saver<RecapState, Boolean> = Saver(save = { it.sharing }, restore = ::RecapState)
    }
}
