package ph.mart.healthapp.feature.progress.ui.pressure

import ph.mart.healthapp.core.data.bloodpressure.BloodPressureReading

/** Seeded at the top of the Normal band, so the first save is a few taps from any real reading. */
data class BloodPressureForm(
    val systolic: Int = 120,
    val diastolic: Int = 80,
    val pulseBpm: Int = 0,
) {
    /**
     * The only rule a cuff reading has to obey. Everything else is clamped by the repository
     * rather than rejected, but a systolic under its diastolic is a transposition, not a value.
     */
    val isValid: Boolean get() = systolic > diastolic

    /** No date or time is asked for: a reading is logged when it is taken. */
    fun toReading(takenAtMillis: Long) = BloodPressureReading(
        takenAtMillis = takenAtMillis,
        systolic = systolic,
        diastolic = diastolic,
        pulseBpm = pulseBpm,
    )
}

sealed interface BloodPressureEvent {
    data class OnSave(val form: BloodPressureForm) : BloodPressureEvent
    data class OnDelete(val id: Long) : BloodPressureEvent
}

sealed interface BloodPressureSideEffect {
    data object Saved : BloodPressureSideEffect
}
