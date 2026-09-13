package ph.mart.healthapp.feature.progress.ui.activity

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
internal fun rememberActivityState(): ActivityState =
    rememberSaveable(saver = ActivityState.Saver()) { ActivityState() }

/** UI-only: the range this page's chart is showing, and nothing else. Both charts share it — the second card's toggle is null. One key out of
 * `ProgressScreenState.ranges`, the way `SleepState` is. */
internal class ActivityState(range: ChartRange = DEFAULT_CHART_RANGE) {
    var range: ChartRange by mutableStateOf(range)

    companion object {
        @Suppress("UNCHECKED_CAST")
        fun Saver(): Saver<ActivityState, Any> = listSaver(
            // Appended, never renumbered — the rule `ProgressScreenState`'s saver keeps.
            save = { listOf(it.range.name) },
            restore = { saved -> ActivityState(ChartRange.valueOf(saved[0] as String)) },
        )
    }
}
