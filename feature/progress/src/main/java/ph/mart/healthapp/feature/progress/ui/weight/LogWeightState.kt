package ph.mart.healthapp.feature.progress.ui.weight

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue

/** [initial] is the row being edited, or the blank form when the sheet is logging a new one. Its
 * date is a `rememberSaveable` input so that opening the sheet on a second record re-seeds the
 * fields rather than restoring the first one's. */
@Composable
internal fun rememberLogWeightState(initial: LogWeightForm = LogWeightForm()): LogWeightState =
    rememberSaveable(initial.dateEpochDay, saver = LogWeightState.Saver()) { LogWeightState(initial) }

internal class LogWeightState(
    form: LogWeightForm = LogWeightForm(),
    showingCalendar: Boolean = false,
    confirmingDelete: Boolean = false,
) {
    var form: LogWeightForm by mutableStateOf(form)
    var showingCalendar: Boolean by mutableStateOf(showingCalendar)

    /** The delete's confirm dialog. Asked rather than undone, for `BloodPressureScreen`'s reason:
     * an undo wants a snackbar host Progress hasn't got. */
    var confirmingDelete: Boolean by mutableStateOf(confirmingDelete)

    companion object {
        fun Saver(): Saver<LogWeightState, Any> = listSaver(
            // Appended, never renumbered.
            save = {
                listOf(it.form.dateEpochDay, it.form.weightKg, it.form.note, it.showingCalendar, it.confirmingDelete)
            },
            restore = { saved ->
                LogWeightState(
                    form = LogWeightForm(dateEpochDay = saved[0] as Long, weightKg = saved[1] as Double, note = saved[2] as String),
                    showingCalendar = saved[3] as Boolean,
                    confirmingDelete = saved[4] as Boolean,
                )
            },
        )
    }
}
