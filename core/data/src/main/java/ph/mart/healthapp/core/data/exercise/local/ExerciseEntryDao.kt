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

    /**
     * The newest workouts that actually recorded lifts — the strength screen's repeat seed (the
     * first of them) and its lift-name chips. A one-shot read rather than a flow: the screen asks
     * once on entry.
     *
     * Selected on having sets rather than on `type = 'Strength'`: a strength session logged
     * through the sheet has no sets, and a run of those would fill the limit with workouts there
     * is nothing to repeat or suggest from. It also keeps an enum name out of the SQL.
     */
    @Query(
        """
        SELECT * FROM exercise_entry e
        WHERE e.isDeleted = 0
          AND EXISTS (SELECT 1 FROM strength_set s WHERE s.entryId = e.id)
        ORDER BY e.date DESC, e.loggedAt DESC
        LIMIT :limit
        """,
    )
    suspend fun recentStrength(limit: Int): List<ExerciseEntryEntity>

    @Query("SELECT * FROM exercise_entry WHERE id = :id AND isDeleted = 0")
    suspend fun entry(id: Long): ExerciseEntryEntity?

    /** Returns the new row id — a health-sync import has to link the remote data point to it. */
    @Insert
    suspend fun insert(entity: ExerciseEntryEntity): Long

    @Query("UPDATE exercise_entry SET isDeleted = 1 WHERE id = :id")
    suspend fun softDelete(id: Long)

    /** The row's original logging time, so an edit can keep it and stay where it is in the day. */
    @Query("SELECT loggedAt FROM exercise_entry WHERE id = :id")
    suspend fun loggedAt(id: Long): Long?

    /** The workout and its sets land together, so the diary's flow never emits a strength session
     * with nothing in it. */
    @Transaction
    suspend fun insertWithSets(entity: ExerciseEntryEntity, sets: List<StrengthSetEntity>): Long {
        val newId = insert(entity)
        if (sets.isNotEmpty()) insertSets(sets.map { it.copy(id = 0, entryId = newId) })
        return newId
    }

    /**
     * Twin of `FoodEntryDao.replace` — an edit supersedes the row rather than rewriting it, in
     * one transaction so the diary's flow never sees the gap.
     *
     * The id changes, which would orphan this workout's sets, so they are re-inserted under the
     * new one and the old rows hard-deleted. That is not a breach of soft-delete-only for the
     * reason `FastingRepository.discardActive()` gives: the superseding row carries the history,
     * and a child of a row that no longer exists never became history of its own.
     */
    @Transaction
    suspend fun replace(id: Long, entity: ExerciseEntryEntity, sets: List<StrengthSetEntity>) {
        deleteSets(id)
        softDelete(id)
        insertWithSets(entity, sets)
    }

    @Query("UPDATE exercise_entry SET isDeleted = 1")
    suspend fun softDeleteAll()

    // --- strength sets -------------------------------------------------------------------
    // They live on this DAO rather than one of their own: same domain, same Koin binding, and
    // every read joins back to exercise_entry anyway.

    /** Joined back to the parent so a soft-deleted workout's sets are never returned — that join
     * is what lets `strength_set` carry no deleted flag of its own. */
    @Query(
        """
        SELECT s.* FROM strength_set s
        JOIN exercise_entry e ON e.id = s.entryId
        WHERE e.date = :date AND e.isDeleted = 0
        ORDER BY s.id ASC
        """,
    )
    fun observeSetsForDate(date: Long): Flow<List<StrengthSetEntity>>

    @Query(
        """
        SELECT s.* FROM strength_set s
        JOIN exercise_entry e ON e.id = s.entryId
        WHERE e.date >= :from AND e.isDeleted = 0
        ORDER BY s.id ASC
        """,
    )
    fun observeSetsSince(from: Long): Flow<List<StrengthSetEntity>>

    @Query(
        """
        SELECT s.* FROM strength_set s
        JOIN exercise_entry e ON e.id = s.entryId
        WHERE e.isDeleted = 0
        ORDER BY s.id ASC
        """,
    )
    suspend fun allActiveSets(): List<StrengthSetEntity>

    @Query("SELECT * FROM strength_set WHERE entryId IN (:entryIds) ORDER BY id ASC")
    suspend fun setsFor(entryIds: List<Long>): List<StrengthSetEntity>

    @Insert
    suspend fun insertSets(entities: List<StrengthSetEntity>)

    @Query("DELETE FROM strength_set WHERE entryId = :entryId")
    suspend fun deleteSets(entryId: Long)

    /** Import is replace-in-full, and a soft-deleted parent's sets are unreachable but not gone. */
    @Query("DELETE FROM strength_set")
    suspend fun deleteAllSets()
}
