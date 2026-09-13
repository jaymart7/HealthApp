package ph.mart.healthapp.feature.progress.ui.activity

import ph.mart.healthapp.core.data.exercise.ExerciseEntry
import ph.mart.healthapp.core.data.health.DEFAULT_STEP_GOAL
import ph.mart.healthapp.core.data.health.StepDay

/**
 * The two halves of the burn series and the goal line between them.
 *
 * [stepDays] is sparse and import-only — a day with no row is a day the watch never reported.
 * [exerciseEntries] is the last year's logged workouts, which `burnSeries()` folds together with
 * the step days so a walk the watch already counted is never counted twice.
 *
 * [stepGoal] is the profile's current target and is deliberately **not** snapshotted per day:
 * `step_day` rows are replaced wholesale on every re-sync, so a target stored beside them would be
 * overwritten. The page's stat says "today's goal" for that reason.
 */
data class ActivityUiState(
    val stepDays: List<StepDay> = emptyList(),
    val exerciseEntries: List<ExerciseEntry> = emptyList(),
    val stepGoal: Int = DEFAULT_STEP_GOAL,
)
