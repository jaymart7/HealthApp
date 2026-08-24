package ph.mart.healthapp.feature.progress.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import ph.mart.healthapp.core.data.progress.MeasurementPart

/** [preselectedPart] wins when tapping an existing tracked row; otherwise defaults to the first
 * untracked part, per the prototype's "+ Add measurement" behavior. */
@Composable
internal fun rememberAddMeasurementState(preselectedPart: MeasurementPart?, trackedParts: Set<MeasurementPart>): AddMeasurementState {
    val initialPart = preselectedPart ?: MeasurementPart.entries.firstOrNull { it !in trackedParts }
    return rememberSaveable(saver = AddMeasurementState.Saver()) { AddMeasurementState(form = AddMeasurementForm(part = initialPart)) }
}

internal class AddMeasurementState(form: AddMeasurementForm = AddMeasurementForm(), showingCalendar: Boolean = false) {
    var form: AddMeasurementForm by mutableStateOf(form)
    var showingCalendar: Boolean by mutableStateOf(showingCalendar)

    companion object {
        fun Saver(): Saver<AddMeasurementState, Any> = listSaver(
            save = { listOf(it.form.part?.name, it.form.dateEpochDay, it.form.valueCm, it.showingCalendar) },
            restore = { saved ->
                AddMeasurementState(
                    form = AddMeasurementForm(
                        part = (saved[0] as String?)?.let(MeasurementPart::valueOf),
                        dateEpochDay = saved[1] as Long,
                        valueCm = saved[2] as Double,
                    ),
                    showingCalendar = saved[3] as Boolean,
                )
            },
        )
    }
}
