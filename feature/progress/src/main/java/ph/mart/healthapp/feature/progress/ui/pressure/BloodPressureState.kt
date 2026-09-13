package ph.mart.healthapp.feature.progress.ui.pressure

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
internal fun rememberBloodPressureState(): BloodPressureState =
    rememberSaveable(saver = BloodPressureState.Saver()) { BloodPressureState() }

/**
 * UI-only: the range this page's chart is showing, whether its log sheet is up, and the reading
 * whose delete is waiting on its confirmation dialog.
 *
 * The sheet flag is the page's own, the way `CycleState`'s is:
 * `ProgressScreenState.activeBloodPressureSheet` survives beside it for the overview's empty-card
 * hint, which opens the same sheet from a surface this page cannot see.
 * [pendingDeleteReadingId] has no such twin — only the page has a list to delete from — so it
 * moves here outright, and `ProgressScreenState`'s saver renumbers in the same commit.
 */
internal class BloodPressureState(
    range: ChartRange = DEFAULT_CHART_RANGE,
    sheetOpen: Boolean = false,
    pendingDeleteReadingId: Long? = null,
) {
    var range: ChartRange by mutableStateOf(range)
    var sheetOpen: Boolean by mutableStateOf(sheetOpen)
    var pendingDeleteReadingId: Long? by mutableStateOf(pendingDeleteReadingId)

    companion object {
        @Suppress("UNCHECKED_CAST")
        fun Saver(): Saver<BloodPressureState, Any> = listSaver(
            // Appended, never renumbered — the rule `ProgressScreenState`'s saver keeps.
            save = { listOf(it.range.name, it.sheetOpen, it.pendingDeleteReadingId) },
            restore = { saved ->
                BloodPressureState(
                    range = ChartRange.valueOf(saved[0] as String),
                    sheetOpen = saved[1] as Boolean,
                    pendingDeleteReadingId = saved[2] as Long?,
                )
            },
        )
    }
}
