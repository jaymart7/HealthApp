package ph.mart.healthapp.feature.progress.ui

import ph.mart.healthapp.core.data.food.DayNutrition
import ph.mart.healthapp.core.data.profile.DailyTargets
import ph.mart.healthapp.core.data.profile.Goal
import ph.mart.healthapp.core.data.profile.UnitSystem
import ph.mart.healthapp.core.data.progress.MeasurementEntry
import ph.mart.healthapp.core.data.progress.MeasurementPart
import ph.mart.healthapp.core.data.progress.ProgressPhoto
import ph.mart.healthapp.core.data.progress.WeightEntry

/** [label] rather than the entry name in the toggle: four equal-weight SegmentedToggle pills
 * leave ~80dp each on a 360dp screen, and "Measurements" needs more than that at labelLarge. */
enum class ProgressTab(val label: String) {
    Weight("Weight"),
    Nutrition("Nutrition"),
    Photos("Photos"),
    Measurements("Body"),
}

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
    /** Dense, one row per day for the last year — the Nutrition tab slices it per selected range. */
    val dailyNutrition: List<DayNutrition> = emptyList(),
    val targets: DailyTargets? = null,
)
