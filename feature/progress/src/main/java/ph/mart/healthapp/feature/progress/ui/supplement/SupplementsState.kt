package ph.mart.healthapp.feature.progress.ui.supplement

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import ph.mart.healthapp.core.data.progress.ChartRange
import ph.mart.healthapp.core.data.todayEpochDay
import ph.mart.healthapp.feature.progress.ui.progress.DEFAULT_CHART_RANGE

@Composable
internal fun rememberSupplementsState(): SupplementsState =
    rememberSaveable(saver = SupplementsState.Saver()) { SupplementsState() }

/**
 * UI-only: the range this page's chart is showing and the day its checklist is on. Neither is
 * persisted and neither belongs in the ViewModel — [selectedDate] is the same kind of transient
 * choice [range] is, and the diary holds its own day the same way.
 *
 * The authoring is still Profile's list. One key out of `ProgressScreenState.ranges`, the way
 * `SleepState` is.
 */
internal class SupplementsState(
    range: ChartRange = DEFAULT_CHART_RANGE,
    selectedDate: Long = todayEpochDay(),
) {
    var range: ChartRange by mutableStateOf(range)

    /** The day the catch-up checklist is showing. Bounded by the screen, not here. */
    var selectedDate: Long by mutableLongStateOf(selectedDate)

    companion object {
        @Suppress("UNCHECKED_CAST")
        fun Saver(): Saver<SupplementsState, Any> = listSaver(
            // Appended, never renumbered — the rule `ProgressScreenState`'s saver keeps.
            save = { listOf<Any>(it.range.name, it.selectedDate) },
            restore = { saved ->
                SupplementsState(
                    range = ChartRange.valueOf(saved[0] as String),
                    selectedDate = saved[1] as Long,
                )
            },
        )
    }
}
