package ph.mart.healthapp.core.data.exercise

import kotlinx.coroutines.flow.Flow

/**
 * Named workout templates. Its own repository rather than four more methods on
 * [ExerciseRepository], which is injected in half a dozen places — Home, the widget, the export,
 * the Progress tabs — that will never want a routine.
 *
 * Shaped like the saved-meal half of `FoodRepository`: observe, add, update, soft-delete. There is
 * no "log a routine" call, because starting one writes nothing: it seeds the strength screen's
 * form, and saving that form is an ordinary [ExerciseRepository.addEntry].
 */
interface RoutineRepository {
    /** Newest first, lifts included. */
    fun observeRoutines(): Flow<List<Routine>>

    /** [days] is the weekday mask [setRoutineDays] takes, for a routine that arrives already
     * planned — the coach's `create_routine`. The editor adds with none and sets them after. */
    suspend fun addRoutine(name: String, lifts: List<RoutineLift>, days: Int = 0)

    /**
     * Rewrites a routine — name, lifts and plan — from Profile's edit sheet. The edit is a new row
     * (`RoutineDao.replace`), so the id changes and the edited routine becomes the newest:
     * `FoodRepository.updateSavedMeal`'s shape, for its reason.
     */
    suspend fun updateRoutine(id: Long, name: String, lifts: List<RoutineLift>, days: Int)

    /** The weekdays this routine is planned for, as the bitmask `TrainingPlan.kt` reads. Its own
     * call rather than an [updateRoutine], because a tap on the card's plan zone keeps the id. */
    suspend fun setRoutineDays(id: Long, days: Int)

    /** Soft delete, like everything else the user authored — and it touches no logged workout,
     * since nothing links one back to a routine. */
    suspend fun deleteRoutine(id: Long)
}
