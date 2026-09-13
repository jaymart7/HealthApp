package ph.mart.healthapp.feature.progress.ui.cycle

import ph.mart.healthapp.core.data.cycle.CycleDay
import ph.mart.healthapp.core.data.cycle.CycleSymptom

/**
 * A day being logged or corrected. Seeded from the row that day already has, so opening the sheet
 * on a day you logged yesterday shows what you said — the same read `LogWeightSheet` gives a date
 * that already has an entry.
 *
 * A flow of 0 with no symptoms is a valid save: it is how a day logged by mistake is taken back,
 * and it writes a zero row rather than deleting one — soft delete, without a deleted flag.
 */
data class CycleLogForm(
    val dateEpochDay: Long,
    val flow: Int = 0,
    val symptoms: Set<CycleSymptom> = emptySet(),
) {
    fun toDay() = CycleDay(dateEpochDay = dateEpochDay, flow = flow, symptoms = symptoms)

    fun toggle(symptom: CycleSymptom): CycleLogForm =
        copy(symptoms = if (symptom in symptoms) symptoms - symptom else symptoms + symptom)
}

/** What the sheet shows for [date] — the row already logged there, or a blank day. */
fun seedCycleForm(days: List<CycleDay>, date: Long): CycleLogForm =
    days.firstOrNull { it.dateEpochDay == date }
        ?.let { CycleLogForm(date, it.flow, it.symptoms) }
        ?: CycleLogForm(date)

sealed interface CycleEvent {
    data class OnSave(val form: CycleLogForm) : CycleEvent
}

sealed interface CycleSideEffect {
    data object Saved : CycleSideEffect
}
