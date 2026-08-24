package ph.mart.healthapp.feature.progress.ui

import ph.mart.healthapp.core.data.profile.Goal
import ph.mart.healthapp.core.data.profile.UnitSystem
import ph.mart.healthapp.core.data.progress.MeasurementEntry
import ph.mart.healthapp.core.data.progress.MeasurementPart
import ph.mart.healthapp.core.data.progress.ProgressPhoto
import ph.mart.healthapp.core.data.progress.WeightEntry

enum class ProgressTab { Weight, Photos, Measurements }

/** Pure read model — Progress has nothing of its own to write; weight/photo/measurement writes
 * all happen through the FAB's [ph.mart.healthapp.feature.progress.ui.LogWeightSheet]/
 * [ph.mart.healthapp.feature.progress.ui.AddPhotoSheet] or the screen-local
 * [ph.mart.healthapp.feature.progress.ui.AddMeasurementSheet], each with its own container. */
data class ProgressUiState(
    val weightEntries: List<WeightEntry> = emptyList(),
    val measurements: Map<MeasurementPart, List<MeasurementEntry>> = emptyMap(),
    val photos: List<ProgressPhoto> = emptyList(),
    val goalWeightKg: Double? = null,
    val goal: Goal? = null,
    val preferredUnit: UnitSystem = UnitSystem.Metric,
)
