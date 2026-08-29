package ph.mart.healthapp.core.data.mood.local

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
internal interface MoodDayDao {
    @Query("SELECT * FROM mood_day WHERE dateEpochDay = :date")
    fun observeForDate(date: Long): Flow<MoodDayEntity?>

    @Query("SELECT * FROM mood_day WHERE mood > 0 OR energy > 0 ORDER BY dateEpochDay ASC")
    fun observeLogged(): Flow<List<MoodDayEntity>>

    @Query("SELECT * FROM mood_day WHERE mood > 0 OR energy > 0 ORDER BY dateEpochDay ASC")
    suspend fun allLogged(): List<MoodDayEntity>

    /** Partial upsert, not [upsert]: tapping a mood face must not wipe that day's energy. */
    @Query(
        "INSERT INTO mood_day(dateEpochDay, mood, energy) VALUES(:date, :mood, 0) " +
            "ON CONFLICT(dateEpochDay) DO UPDATE SET mood = :mood",
    )
    suspend fun setMood(date: Long, mood: Int)

    @Query(
        "INSERT INTO mood_day(dateEpochDay, mood, energy) VALUES(:date, 0, :energy) " +
            "ON CONFLICT(dateEpochDay) DO UPDATE SET energy = :energy",
    )
    suspend fun setEnergy(date: Long, energy: Int)

    /** Whole-row write — the import and the debug seed, which know both values. */
    @Upsert
    suspend fun upsert(entity: MoodDayEntity)

    @Query("UPDATE mood_day SET mood = 0, energy = 0")
    suspend fun clearAll()
}
