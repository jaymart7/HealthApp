package ph.mart.healthapp.feature.progress.ui.progress

import ph.mart.healthapp.core.data.bloodpressure.BloodPressureReading
import ph.mart.healthapp.core.data.fasting.DEFAULT_FAST_GOAL_HOURS
import ph.mart.healthapp.core.data.fasting.FastSession
import ph.mart.healthapp.core.data.food.DayNutrition
import ph.mart.healthapp.core.data.health.HeartDay
import ph.mart.healthapp.core.data.health.SleepNight
import ph.mart.healthapp.core.data.mood.MoodDay
import ph.mart.healthapp.core.data.profile.DailyTargets
import ph.mart.healthapp.core.data.profile.Goal
import ph.mart.healthapp.core.data.profile.UnitSystem
import ph.mart.healthapp.core.data.progress.MeasurementEntry
import ph.mart.healthapp.core.data.progress.MeasurementPart
import ph.mart.healthapp.core.data.progress.ProgressPhoto
import ph.mart.healthapp.core.data.progress.WeightEntry
import ph.mart.healthapp.core.data.supplement.SupplementDay

/** [label] rather than the entry name in the toggle: labels are trimmed to keep the pills wide
 * enough to read — "Measurements" to "Body" and "Nutrition" to "Food". Past five options the
 * toggle stops splitting the width evenly and scrolls instead (see SegmentedToggle), which is
 * what makes every tab past the fifth possible without trimming any further. */
enum class ProgressTab(val label: String) {
    Weight("Weight"),
    Nutrition("Food"),
    Photos("Photos"),
    Measurements("Body"),
    Mood("Mood"),
    Sleep("Sleep"),
    Heart("Heart"),
    Fasting("Fasting"),
    Supplements("Supplements"),
    BloodPressure("Blood pressure"),
}

/** Pure read model — Progress has nothing of its own to write; weight/photo/measurement writes
 * all happen through the FAB's [ph.mart.healthapp.feature.progress.ui.weight.LogWeightSheet]/
 * [ph.mart.healthapp.feature.progress.ui.photo.AddPhotoSheet] or the screen-local
 * [ph.mart.healthapp.feature.progress.ui.measurement.AddMeasurementSheet], each with its own container. */
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
    /** Sparse too, and import-only: FitPulse cannot measure sleep, so a night with no row is a
     * night Google Health never sent. The Sleep tab places them by date, like [moodDays]. */
    val sleepNights: List<SleepNight> = emptyList(),
    /** Sparse and import-only like [sleepNights], and dated by the local day each reading was
     * taken. The Heart tab places them by date. */
    val heartDays: List<HeartDay> = emptyList(),
    /** Completed fasts only, oldest first — a running one would keep growing under the chart.
     * Sparse and dated by the day each fast *ended*, like [sleepNights]. */
    val fastSessions: List<FastSession> = emptyList(),
    /** The profile's current target — the chart's goal line. Each bar carries its own snapshot,
     * so this only ever moves the line, never a bar. */
    val fastingGoalHours: Int = DEFAULT_FAST_GOAL_HOURS,
    /** Sparse, one row per supplement per day it was due — a day with no rows is a gap, not a
     * miss. Each row carries the target it was scored against, so a later edit can't rewrite a
     * bar already drawn. */
    val supplementDays: List<SupplementDay> = emptyList(),
    /** Sparse and per *reading*, not per day — the only series here that can hold several points
     * on one day. The tab folds it with `byDay()` before charting it. */
    val bloodPressure: List<BloodPressureReading> = emptyList(),
    /** Every day anything was logged, across all four domains — the streak's definition, reused
     * by the weekly recap so the two can't disagree about what a logged day is. */
    val activeDays: Set<Long> = emptySet(),
    val targets: DailyTargets? = null,
)
