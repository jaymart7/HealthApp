package ph.mart.healthapp.core.data.exercise

import kotlinx.coroutines.flow.Flow

/**
 * Named workout templates. Its own repository rather than four more methods on
 * [ExerciseRepository], which is injected in half a dozen places — Home, the widget, the export,
 * the Progress tabs — that will never want a routine.
 *
 * Shaped like the saved-meal half of `FoodRepository`: observe, add, rename, soft-delete. There is
 * no "log a routine" call, because starting one writes nothing: it seeds the strength screen's
 * form, and saving that form is an ordinary [ExerciseRepository.addEntry].
 */
interface RoutineRepository {
    /** Newest first, lifts included. */
    fun observeRoutines(): Flow<List<Routine>>

    suspend fun addRoutine(name: String, lifts: List<RoutineLift>)

    suspend fun renameRoutine(id: Long, name: String)

    /** Soft delete, like everything else the user authored — and it touches no logged workout,
     * since nothing links one back to a routine. */
    suspend fun deleteRoutine(id: Long)
}
