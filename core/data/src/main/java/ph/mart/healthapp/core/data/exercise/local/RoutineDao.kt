package ph.mart.healthapp.core.data.exercise.local

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import androidx.room3.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
internal interface RoutineDao {
    /** Newest first — id order is save order, and nothing renumbers it. No LIMIT, unlike
     * `SavedMealDao`: the strength screen's chips scroll horizontally and Profile's list scrolls
     * vertically, so neither surface has a height a window would be protecting. */
    @Query("SELECT * FROM routine WHERE isDeleted = 0 ORDER BY id DESC")
    fun observeRoutines(): Flow<List<RoutineEntity>>

    /** Every lift of every routine — a handful of rows in total, so the grouping happens in Kotlin
     * rather than in a per-parent query. Lifts of a deleted routine are dropped by that grouping,
     * which is driven by the parent list. */
    @Query("SELECT * FROM routine_lift ORDER BY id ASC")
    fun observeLifts(): Flow<List<RoutineLiftEntity>>

    @Insert
    suspend fun insertRoutine(entity: RoutineEntity): Long

    @Insert
    suspend fun insertLifts(entities: List<RoutineLiftEntity>)

    /** The routine and its lifts land together, so the chip row never offers one with nothing in
     * it — `ExerciseEntryDao.insertWithSets`' reasoning, one table over. */
    @Transaction
    suspend fun insertWithLifts(entity: RoutineEntity, lifts: List<RoutineLiftEntity>) {
        val newId = insertRoutine(entity)
        if (lifts.isNotEmpty()) insertLifts(lifts.map { it.copy(id = 0, routineId = newId) })
    }

    @Query("UPDATE routine SET isDeleted = 1 WHERE id = :id")
    suspend fun softDelete(id: Long)

    /** Only the name, for the reason `SavedMealDao.rename` gives: an upsert of the whole entity is
     * one typo away from rewriting what the routine is. */
    @Query("UPDATE routine SET name = :name WHERE id = :id")
    suspend fun rename(id: Long, name: String)

    /** One column, for [rename]'s reason — and the whole write surface of the training plan. */
    @Query("UPDATE routine SET days = :days WHERE id = :id")
    suspend fun setDays(id: Long, days: Int)
}
