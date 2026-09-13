package ph.mart.healthapp.feature.progress.ui.sleep

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
internal fun rememberSleepState(): SleepState =
    rememberSaveable(saver = SleepState.Saver()) { SleepState() }

/**
 * UI-only: the range this page's chart is showing, and nothing else — the page reads, and Sleep is
 * import-only, so there is no sheet and no overlay to track.
 *
 * It was one key of `ProgressScreenState.ranges`, a map keyed by subject because thirteen pages
 * shared one state holder. A route holds its own, so the map loses a key and the range cannot
 * outlive the page that set it.
 */
internal class SleepState(range: ChartRange = DEFAULT_CHART_RANGE) {
    var range: ChartRange by mutableStateOf(range)

    companion object {
        @Suppress("UNCHECKED_CAST")
        fun Saver(): Saver<SleepState, Any> = listSaver(
            // Appended, never renumbered — the rule `ProgressScreenState`'s saver keeps.
            save = { listOf(it.range.name) },
            restore = { saved -> SleepState(ChartRange.valueOf(saved[0] as String)) },
        )
    }
}
