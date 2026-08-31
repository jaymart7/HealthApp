package ph.mart.healthapp.core.data.health.local

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import kotlinx.coroutines.flow.Flow

@Dao
internal interface HeartDayDao {
    @Query("SELECT * FROM heart_day WHERE date = :date")
    fun observeForDate(date: Long): Flow<HeartDayEntity?>

    /** Every imported day, oldest first — the Progress tab's Heart series. Sparse by nature: a day
     * the watch never recorded has no row, and the chart draws that gap. */
    @Query("SELECT * FROM heart_day ORDER BY date")
    fun observeAll(): Flow<List<HeartDayEntity>>

    /**
     * REPLACE, not accumulate: a sync re-queries whole days and writes the aggregate the API
     * reports right now, so re-running it is idempotent and a sample the watch later revised
     * corrects itself.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: HeartDayEntity)

    /**
     * The sync cursor. Heart rate rides no `health_link` row, for the same reason steps don't —
     * the API returns intra-day samples and FitPulse stores a daily aggregate, so there is no
     * remote resource name to key a link by. Derived from the rows we actually wrote, which is the
     * property that makes `health_link`'s own cursor safe.
     */
    @Query("SELECT MAX(date) FROM heart_day")
    suspend fun latestDate(): Long?

    @Query("DELETE FROM heart_day")
    suspend fun clear()
}
