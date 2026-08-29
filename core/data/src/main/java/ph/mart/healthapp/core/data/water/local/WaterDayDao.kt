package ph.mart.healthapp.core.data.water.local

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
internal interface WaterDayDao {
    @Query("SELECT * FROM water_day WHERE dateEpochDay = :date")
    fun observeForDate(date: Long): Flow<WaterDayEntity?>

    @Query("SELECT * FROM water_day WHERE glasses > 0 ORDER BY dateEpochDay ASC")
    suspend fun allNonZero(): List<WaterDayEntity>

    /** Dates only — the streak needs which days were logged, never how much. */
    @Query("SELECT dateEpochDay FROM water_day WHERE glasses > 0 AND dateEpochDay >= :from")
    fun observeLoggedDaysSince(from: Long): Flow<List<Long>>

    @Upsert
    suspend fun upsert(entity: WaterDayEntity)

    @Query("UPDATE water_day SET glasses = 0")
    suspend fun clearAll()
}
