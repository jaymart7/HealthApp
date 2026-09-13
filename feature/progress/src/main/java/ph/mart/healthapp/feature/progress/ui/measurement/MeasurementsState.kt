package ph.mart.healthapp.feature.progress.ui.measurement

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import ph.mart.healthapp.core.data.progress.MeasurementPart

@Composable
internal fun rememberMeasurementsState(): MeasurementsState =
    rememberSaveable(saver = MeasurementsState.Saver()) { MeasurementsState() }

/**
 * UI-only, and the one converted page's state with **no chart range** in it: Measurements is a
 * table of six sparse histories rather than a series with an axis, so there is nothing to slice.
 *
 * [part] is what a tapped row pre-fills the sheet with; null is the Add button's "pick one". Both
 * fields moved out of `ProgressScreenState` outright — the sheet has no second door the way the
 * cycle and blood-pressure ones do — so that saver renumbers in the same commit.
 */
internal class MeasurementsState(
    sheetOpen: Boolean = false,
    part: MeasurementPart? = null,
) {
    var sheetOpen: Boolean by mutableStateOf(sheetOpen)
    var part: MeasurementPart? by mutableStateOf(part)

    fun openSheet(part: MeasurementPart?) {
        this.part = part
        sheetOpen = true
    }

    companion object {
        @Suppress("UNCHECKED_CAST")
        fun Saver(): Saver<MeasurementsState, Any> = listSaver(
            // Appended, never renumbered — the rule `ProgressScreenState`'s saver keeps.
            save = { listOf(it.sheetOpen, it.part?.name) },
            restore = { saved ->
                MeasurementsState(
                    sheetOpen = saved[0] as Boolean,
                    part = (saved[1] as String?)?.let(MeasurementPart::valueOf),
                )
            },
        )
    }
}
