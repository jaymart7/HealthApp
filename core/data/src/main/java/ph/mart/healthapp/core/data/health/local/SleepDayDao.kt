package ph.mart.healthapp.core.data.health.local

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import kotlinx.coroutines.flow.Flow

@Dao
internal interface SleepDayDao {
    @Query("SELECT * FROM sleep_day WHERE date = :date")
    fun observeForDate(date: Long): Flow<SleepDayEntity?>

    /** Every imported night, oldest first — the Progress tab's Sleep series. Sparse by nature:
     * a night the watch never recorded has no row, and the chart draws that gap. */
    @Query("SELECT * FROM sleep_day ORDER BY date")
    fun observeAll(): Flow<List<SleepDayEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: SleepDayEntity)

    @Query("DELETE FROM sleep_day WHERE date = :date")
    suspend fun delete(date: Long)
}
