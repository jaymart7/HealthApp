package ph.mart.healthapp.core.data.supplement.local

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
internal interface SupplementDao {
    @Query("SELECT * FROM supplement WHERE deleted = 0 ORDER BY createdAt ASC, id ASC")
    fun observeActive(): Flow<List<SupplementEntity>>

    @Query("SELECT * FROM supplement WHERE deleted = 0 ORDER BY createdAt ASC, id ASC")
    suspend fun active(): List<SupplementEntity>

    /** Soft-deleted rows included: an exported `supplement_day` points at an id, and dropping the
     * row it names would leave the tick with no subject. */
    @Query("SELECT * FROM supplement ORDER BY createdAt ASC, id ASC")
    suspend fun all(): List<SupplementEntity>

    @Query("SELECT * FROM supplement_day WHERE dateEpochDay = :date")
    fun observeForDate(date: Long): Flow<List<SupplementDayEntity>>

    @Query("SELECT * FROM supplement_day ORDER BY dateEpochDay ASC")
    fun observeAllDays(): Flow<List<SupplementDayEntity>>

    /** Zeroed days are not exported, the same rule `water_day` follows. */
    @Query("SELECT * FROM supplement_day WHERE taken > 0 ORDER BY dateEpochDay ASC")
    suspend fun allTakenDays(): List<SupplementDayEntity>

    @Upsert
    suspend fun upsert(entity: SupplementEntity)

    @Upsert
    suspend fun upsertDay(entity: SupplementDayEntity)

    /** IGNORE, not REPLACE: this seeds the day's missing rows and must never reset a count the
     * user already tapped. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertDaysIfAbsent(entities: List<SupplementDayEntity>)

    @Query("UPDATE supplement_day SET taken = :taken WHERE dateEpochDay = :date AND supplementId = :id")
    suspend fun setTaken(date: Long, id: Long, taken: Int)

    @Query("UPDATE supplement SET deleted = 1 WHERE id = :id")
    suspend fun softDelete(id: Long)

    /**
     * One transaction so the day can never be half-seeded: every active supplement gets a row for
     * [date] before the tapped one is set. Without the seed the adherence chart's denominator
     * would be only whatever was ticked, and someone who took 1 of 3 would chart 100%.
     */
    @Transaction
    suspend fun setTakenOn(date: Long, id: Long, taken: Int) {
        insertDaysIfAbsent(
            active().map { SupplementDayEntity(date, it.id, taken = 0, dueTimes = it.timesPerDay) },
        )
        setTaken(date, id, taken)
    }

    @Query("DELETE FROM supplement")
    suspend fun clearSupplements()

    @Query("DELETE FROM supplement_day")
    suspend fun clearDays()
}
