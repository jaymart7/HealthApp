package ph.mart.healthapp.core.data.exercise.local

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import androidx.room3.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
internal interface ExerciseEntryDao {
    @Query("SELECT * FROM exercise_entry WHERE date = :date AND isDeleted = 0 ORDER BY loggedAt ASC")
    fun observeForDate(date: Long): Flow<List<ExerciseEntryEntity>>

    /** Dates only — the streak needs which days had activity, never how much. */
    @Query("SELECT DISTINCT date FROM exercise_entry WHERE date >= :from AND isDeleted = 0")
    fun observeLoggedDaysSince(from: Long): Flow<List<Long>>

    /** Every entry since [from], oldest first — the Progress Activity tab's burn series. Bounded
     * rather than unbounded: the charts never look past a year. */
    @Query("SELECT * FROM exercise_entry WHERE date >= :from AND isDeleted = 0 ORDER BY date ASC, loggedAt ASC")
    fun observeSince(from: Long): Flow<List<ExerciseEntryEntity>>

    @Query("SELECT * FROM exercise_entry WHERE isDeleted = 0 ORDER BY date ASC, loggedAt ASC")
    suspend fun allActive(): List<ExerciseEntryEntity>

    /** Returns the new row id — a health-sync import has to link the remote data point to it. */
    @Insert
    suspend fun insert(entity: ExerciseEntryEntity): Long

    @Query("UPDATE exercise_entry SET isDeleted = 1 WHERE id = :id")
    suspend fun softDelete(id: Long)

    /** The row's original logging time, so an edit can keep it and stay where it is in the day. */
    @Query("SELECT loggedAt FROM exercise_entry WHERE id = :id")
    suspend fun loggedAt(id: Long): Long?

    /** Twin of `FoodEntryDao.replace` — an edit supersedes the row rather than rewriting it, in
     * one transaction so the diary's flow never sees the gap. */
    @Transaction
    suspend fun replace(id: Long, entity: ExerciseEntryEntity) {
        softDelete(id)
        insert(entity)
    }

    @Query("UPDATE exercise_entry SET isDeleted = 1")
    suspend fun softDeleteAll()
}
