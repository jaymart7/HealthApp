package ph.mart.healthapp.core.data.health.local

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import kotlinx.coroutines.flow.Flow

@Dao
internal interface StepDayDao {
    @Query("SELECT * FROM step_day WHERE date = :date")
    fun observeForDate(date: Long): Flow<StepDayEntity?>

    /**
     * REPLACE, not accumulate: a sync re-queries whole days and writes the total the API reports
     * right now, so re-running it is idempotent and a bucket the watch later revised corrects
     * itself.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: StepDayEntity)

    /**
     * The sync cursor. Steps are the one imported type that doesn't ride `health_link` — the API
     * returns intra-day buckets and FitPulse stores a daily total, so there is no remote resource
     * name to key a link by. Derived from the rows we actually wrote, which is the same property
     * that makes `health_link`'s own cursor safe.
     */
    @Query("SELECT MAX(date) FROM step_day")
    suspend fun latestDate(): Long?

    @Query("DELETE FROM step_day")
    suspend fun clear()
}
