package ph.mart.healthapp.feature.progress.ui.water

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import ph.mart.healthapp.core.data.progress.ChartRange
import ph.mart.healthapp.feature.progress.ui.progress.DEFAULT_CHART_RANGE

@Composable
internal fun rememberWaterState(): WaterState =
    rememberSaveable(saver = WaterState.Saver()) { WaterState() }

/** UI-only: the range this page's chart is showing, and nothing else. Logging a glass is Home's
 * card and the diary's row. One key out of `ProgressScreenState.ranges`, the way `FastingState` is. */
internal class WaterState(range: ChartRange = DEFAULT_CHART_RANGE) {
    var range: ChartRange by mutableStateOf(range)

    companion object {
        @Suppress("UNCHECKED_CAST")
        fun Saver(): Saver<WaterState, Any> = listSaver(
            // Appended, never renumbered — the rule `ProgressScreenState`'s saver keeps.
            save = { listOf(it.range.name) },
            restore = { saved -> WaterState(ChartRange.valueOf(saved[0])) },
        )
    }
}
