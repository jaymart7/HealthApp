package ph.mart.healthapp.feature.progress.ui.pressure

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue

@Composable
internal fun rememberLogBloodPressureState(): LogBloodPressureState =
    rememberSaveable(saver = LogBloodPressureState.Saver()) { LogBloodPressureState() }

/** UI-only: the half-typed reading, which has no meaning outside the sheet holding it. */
internal class LogBloodPressureState(form: BloodPressureForm = BloodPressureForm()) {
    var form: BloodPressureForm by mutableStateOf(form)

    companion object {
        fun Saver(): Saver<LogBloodPressureState, Any> = listSaver(
            save = { listOf(it.form.systolic, it.form.diastolic, it.form.pulseBpm) },
            restore = { saved ->
                LogBloodPressureState(
                    form = BloodPressureForm(
                        systolic = saved[0] as Int,
                        diastolic = saved[1] as Int,
                        pulseBpm = saved[2] as Int,
                    ),
                )
            },
        )
    }
}
