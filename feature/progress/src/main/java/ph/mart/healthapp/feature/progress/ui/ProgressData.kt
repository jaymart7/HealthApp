package ph.mart.healthapp.feature.progress.ui

import ph.mart.healthapp.core.data.food.DayNutrition
import ph.mart.healthapp.core.data.mood.MoodDay
import ph.mart.healthapp.core.data.profile.DailyTargets
import ph.mart.healthapp.core.data.profile.Goal
import ph.mart.healthapp.core.data.profile.UnitSystem
import ph.mart.healthapp.core.data.progress.MeasurementEntry
import ph.mart.healthapp.core.data.progress.MeasurementPart
import ph.mart.healthapp.core.data.progress.ProgressPhoto
import ph.mart.healthapp.core.data.progress.WeightEntry

/** [label] rather than the entry name in the toggle: five equal-weight SegmentedToggle pills
 * leave ~64dp each on a 360dp screen, so every label is trimmed to fit at labelLarge —
 * "Measurements" to "Body" and "Nutrition" to "Food". "Photos" is the longest that survives. */
enum class ProgressTab(val label: String) {
    Weight("Weight"),
    Nutrition("Food"),
    Photos("Photos"),
    Measurements("Body"),
    Mood("Mood"),
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
    /** Sparse — logged days only, unlike [dailyNutrition]. The Mood tab places them by date. */
    val moodDays: List<MoodDay> = emptyList(),
    /** Every day anything was logged, across all four domains — the streak's definition, reused
     * by the weekly recap so the two can't disagree about what a logged day is. */
    val activeDays: Set<Long> = emptySet(),
    val targets: DailyTargets? = null,
)
