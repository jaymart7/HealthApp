package ph.mart.healthapp.feature.progress.ui.weight

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import ph.mart.healthapp.core.data.progress.ChartRange
import ph.mart.healthapp.feature.progress.ui.progress.DEFAULT_CHART_RANGE

/** No record is being edited. A sentinel rather than a null because the saver below is a
 * `listSaver` over `Any`, which has nowhere to put one. */
internal const val NO_EDIT = -1L

/**
 * How many more records reaching the foot of the page appends, and where the window opens.
 *
 * ponytail: a counter over a list already in memory rather than Paging3 — every weigh-in arrives on
 * one Room flow, so what this saves is composition, not a query. `FOOD_PAGE_SIZE` is the precedent.
 */
internal const val RECORDS_PAGE_SIZE = 20

@Composable
internal fun rememberWeightState(): WeightState =
    rememberSaveable(saver = WeightState.Saver()) { WeightState() }

/** UI-only: the range this page's chart is showing, and whether its energy check-in is up. The
 * range was one key out of `ProgressScreenState.ranges`, the way `SleepState`'s is. */
internal class WeightState(
    range: ChartRange = DEFAULT_CHART_RANGE,
    checkInOpen: Boolean = false,
    editDateEpochDay: Long = NO_EDIT,
    shownRecords: Int = RECORDS_PAGE_SIZE,
) {
    var range: ChartRange by mutableStateOf(range)

    /** The energy check-in, drawn over this page. It moved out of `ProgressScreenState` outright:
     * the insight card on this page is its only door, so there is no second surface to keep a copy
     * for, and that saver renumbers in the same commit. */
    var checkInOpen: Boolean by mutableStateOf(checkInOpen)

    /** The record the log sheet is open on, by its date — the table is keyed by one, so the date is
     * the whole identity. [NO_EDIT] when the sheet is closed. */
    var editDateEpochDay: Long by mutableStateOf(editDateEpochDay)

    /** How many of the window's weigh-ins the records list has rendered so far. */
    var shownRecords: Int by mutableStateOf(shownRecords)

    /**
     * Clamped for `FoodSearchUiState.withMore()`'s reason: at the end of the list this leaves the
     * count where it is, so a page already scrolled to its foot can keep asking without looping.
     * Never below one page, or switching the chart to a shorter range would strand the window at
     * however few rows that range holds.
     */
    fun showMoreRecords(total: Int) {
        shownRecords = (shownRecords + RECORDS_PAGE_SIZE).coerceAtMost(maxOf(total, RECORDS_PAGE_SIZE))
    }

    companion object {
        @Suppress("UNCHECKED_CAST")
        fun Saver(): Saver<WeightState, Any> = listSaver(
            // Appended, never renumbered — the rule `ProgressScreenState`'s saver keeps.
            save = { listOf(it.range.name, it.checkInOpen, it.editDateEpochDay, it.shownRecords) },
            restore = { saved ->
                WeightState(
                    range = ChartRange.valueOf(saved[0] as String),
                    checkInOpen = saved[1] as Boolean,
                    editDateEpochDay = saved[2] as Long,
                    shownRecords = saved[3] as Int,
                )
            },
        )
    }
}
