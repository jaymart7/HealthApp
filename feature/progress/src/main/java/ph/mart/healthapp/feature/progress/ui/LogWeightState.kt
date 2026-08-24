package ph.mart.healthapp.feature.progress.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue

@Composable
internal fun rememberLogWeightState(): LogWeightState =
    rememberSaveable(saver = LogWeightState.Saver()) { LogWeightState() }

internal class LogWeightState(form: LogWeightForm = LogWeightForm(), showingCalendar: Boolean = false) {
    var form: LogWeightForm by mutableStateOf(form)
    var showingCalendar: Boolean by mutableStateOf(showingCalendar)

    companion object {
        fun Saver(): Saver<LogWeightState, Any> = listSaver(
            save = { listOf(it.form.dateEpochDay, it.form.weightKg, it.form.note, it.showingCalendar) },
            restore = { saved ->
                LogWeightState(
                    form = LogWeightForm(dateEpochDay = saved[0] as Long, weightKg = saved[1] as Double, note = saved[2] as String),
                    showingCalendar = saved[3] as Boolean,
                )
            },
        )
    }
}
