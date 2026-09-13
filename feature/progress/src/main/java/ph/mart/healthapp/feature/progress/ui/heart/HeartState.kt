package ph.mart.healthapp.feature.progress.ui.heart

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
internal fun rememberHeartState(): HeartState =
    rememberSaveable(saver = HeartState.Saver()) { HeartState() }

/** UI-only: the range this page's chart is showing, and nothing else. The series is import-only, so there is no sheet to track. One key out of
 * `ProgressScreenState.ranges`, the way `SleepState` is. */
internal class HeartState(range: ChartRange = DEFAULT_CHART_RANGE) {
    var range: ChartRange by mutableStateOf(range)

    companion object {
        @Suppress("UNCHECKED_CAST")
        fun Saver(): Saver<HeartState, Any> = listSaver(
            // Appended, never renumbered — the rule `ProgressScreenState`'s saver keeps.
            save = { listOf(it.range.name) },
            restore = { saved -> HeartState(ChartRange.valueOf(saved[0] as String)) },
        )
    }
}
