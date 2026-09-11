package ph.mart.healthapp.feature.training.ui.training

import ph.mart.healthapp.core.data.exercise.ExerciseEntry
import ph.mart.healthapp.core.data.exercise.PlanDay
import ph.mart.healthapp.core.data.exercise.Routine
import ph.mart.healthapp.core.data.profile.UnitSystem

/** Recent sessions worth a glance. Five is what fits under the day without the tab becoming a
 * history — Progress is the history. */
const val RECENT_SESSIONS = 5

/**
 * Read-only, and there is no `TrainingEvent`: every button on this tab navigates or opens a sheet
 * `AppScaffold` hosts, so nothing here writes. Deletion is the diary's, where it already is.
 *
 * [recent] is the last year, oldest first — what `ExerciseRepository.observeRecentEntries()`
 * returns. The tab shows [recentSessions] of it; [ph.mart.healthapp.core.data.exercise.trainingWeek]
 * scores the week from the same list, so neither can disagree with the other.
 */
data class TrainingUiState(
    val loaded: Boolean = false,
    val today: List<ExerciseEntry> = emptyList(),
    val recent: List<ExerciseEntry> = emptyList(),
    val routines: List<Routine> = emptyList(),
    val trainingWeek: List<PlanDay> = emptyList(),
    val unit: UnitSystem = UnitSystem.Metric,
)

/**
 * The recent block's rows: newest first, today's excluded — the Today block above is already
 * showing those, and repeating them reads as double-logging.
 *
 * Ties break on id rather than being left to the input order: two sessions on one day arrive
 * oldest-first from the repository, and "newest first" has to mean it within a day too.
 */
fun List<ExerciseEntry>.recentSessions(
    todayEpochDay: Long,
    limit: Int = RECENT_SESSIONS,
): List<ExerciseEntry> = asSequence()
    .filter { it.dateEpochDay < todayEpochDay }
    .sortedWith(compareByDescending<ExerciseEntry> { it.dateEpochDay }.thenByDescending { it.id })
    .take(limit)
    .toList()
