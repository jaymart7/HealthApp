package ph.mart.healthapp.feature.progress.ui.cycle

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import ph.mart.healthapp.core.data.cycle.CycleSymptom
import ph.mart.healthapp.core.data.cycle.cycleSymptoms
import ph.mart.healthapp.core.data.cycle.encodeCycleSymptoms
import ph.mart.healthapp.core.data.todayEpochDay

@Composable
internal fun rememberLogCycleState(): LogCycleState =
    rememberSaveable(saver = LogCycleState.Saver()) { LogCycleState() }

/** UI-only: the half-filled day and whether the calendar is swapped in, neither of which means
 * anything outside the sheet holding it. */
internal class LogCycleState(
    form: CycleLogForm = CycleLogForm(todayEpochDay()),
    showingCalendar: Boolean = false,
) {
    var form: CycleLogForm by mutableStateOf(form)
    var showingCalendar: Boolean by mutableStateOf(showingCalendar)

    companion object {
        /** The symptom set rides the saver as its stored string — the format Room holds it in, so
         * a rotation can't produce a set the table couldn't. */
        fun Saver(): Saver<LogCycleState, Any> = listSaver(
            save = { listOf(it.form.dateEpochDay, it.form.flow, encodeCycleSymptoms(it.form.symptoms), it.showingCalendar) },
            restore = { saved ->
                LogCycleState(
                    form = CycleLogForm(
                        dateEpochDay = saved[0] as Long,
                        flow = saved[1] as Int,
                        symptoms = cycleSymptoms(saved[2] as String),
                    ),
                    showingCalendar = saved[3] as Boolean,
                )
            },
        )
    }
}

/** Declaration order, so the chips never reshuffle under a finger. */
internal val SymptomChips: List<CycleSymptom> = CycleSymptom.entries
