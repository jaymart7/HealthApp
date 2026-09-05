package ph.mart.healthapp.core.data.cycle.local

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
internal interface CycleDayDao {
    @Query("SELECT * FROM cycle_day WHERE dateEpochDay = :date")
    fun observeForDate(date: Long): Flow<CycleDayEntity?>

    @Query("SELECT * FROM cycle_day WHERE flow > 0 OR symptoms != '' ORDER BY dateEpochDay ASC")
    fun observeLogged(): Flow<List<CycleDayEntity>>

    @Query("SELECT * FROM cycle_day WHERE flow > 0 OR symptoms != '' ORDER BY dateEpochDay ASC")
    suspend fun allLogged(): List<CycleDayEntity>

    @Query("SELECT flow FROM cycle_day WHERE dateEpochDay = :date")
    suspend fun flowOn(date: Long): Int?

    /** Partial upsert, not [upsert]: tapping a flow must not wipe that day's symptoms — the same
     * reason `MoodDayDao.setMood` leaves that day's energy alone. */
    @Query(
        "INSERT INTO cycle_day(dateEpochDay, flow, symptoms) VALUES(:date, :flow, '') " +
            "ON CONFLICT(dateEpochDay) DO UPDATE SET flow = :flow",
    )
    suspend fun setFlow(date: Long, flow: Int)

    @Query(
        "INSERT INTO cycle_day(dateEpochDay, flow, symptoms) VALUES(:date, 0, :symptoms) " +
            "ON CONFLICT(dateEpochDay) DO UPDATE SET symptoms = :symptoms",
    )
    suspend fun setSymptoms(date: Long, symptoms: String)

    /** Whole-row write — the logging sheet, the import and the debug seed, which know both. */
    @Upsert
    suspend fun upsert(entity: CycleDayEntity)

    @Query("UPDATE cycle_day SET flow = 0, symptoms = ''")
    suspend fun clearAll()
}
