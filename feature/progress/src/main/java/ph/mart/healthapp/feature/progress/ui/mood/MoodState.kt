package ph.mart.healthapp.feature.progress.ui.mood

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
internal fun rememberMoodState(): MoodState =
    rememberSaveable(saver = MoodState.Saver()) { MoodState() }

/** UI-only: the range this page's chart is showing, and nothing else — the page reads, and the
 * logging is Home's card. One key out of `ProgressScreenState.ranges`, the way `SleepState` is. */
internal class MoodState(range: ChartRange = DEFAULT_CHART_RANGE) {
    var range: ChartRange by mutableStateOf(range)

    companion object {
        @Suppress("UNCHECKED_CAST")
        fun Saver(): Saver<MoodState, Any> = listSaver(
            // Appended, never renumbered — the rule `ProgressScreenState`'s saver keeps.
            save = { listOf(it.range.name) },
            restore = { saved -> MoodState(ChartRange.valueOf(saved[0] as String)) },
        )
    }
}
