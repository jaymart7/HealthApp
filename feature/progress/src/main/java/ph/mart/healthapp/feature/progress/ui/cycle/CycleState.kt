package ph.mart.healthapp.feature.progress.ui.cycle

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
internal fun rememberCycleState(): CycleState =
    rememberSaveable(saver = CycleState.Saver()) { CycleState() }

/**
 * UI-only: the range this page's chart is showing and whether its log sheet is up.
 *
 * The sheet flag is the page's own. `ProgressScreenState.activeCycleSheet` survives beside it and
 * is **not** the same flag: the overview's empty-card hint opens the sheet too, from a surface this
 * page cannot see, so that one stays where it is rather than moving here. Two flags, two surfaces,
 * one sheet.
 */
internal class CycleState(
    range: ChartRange = DEFAULT_CHART_RANGE,
    sheetOpen: Boolean = false,
) {
    var range: ChartRange by mutableStateOf(range)
    var sheetOpen: Boolean by mutableStateOf(sheetOpen)

    companion object {
        @Suppress("UNCHECKED_CAST")
        fun Saver(): Saver<CycleState, Any> = listSaver(
            // Appended, never renumbered — the rule `ProgressScreenState`'s saver keeps.
            save = { listOf(it.range.name, it.sheetOpen) },
            restore = { saved -> CycleState(ChartRange.valueOf(saved[0] as String), saved[1] as Boolean) },
        )
    }
}
