package ph.mart.healthapp.core.data.fasting.local

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
internal interface FastSessionDao {
    /** At most one row can match — [stop] and [start]'s guard together keep it that way. The
     * ORDER BY/LIMIT is belt and braces, not a case the app can reach. */
    @Query("SELECT * FROM fast_session WHERE endMillis IS NULL ORDER BY startMillis DESC LIMIT 1")
    fun observeActive(): Flow<FastSessionEntity?>

    @Query("SELECT * FROM fast_session WHERE endMillis IS NULL ORDER BY startMillis DESC LIMIT 1")
    suspend fun activeNow(): FastSessionEntity?

    @Query("SELECT * FROM fast_session WHERE endMillis IS NOT NULL ORDER BY endMillis ASC")
    fun observeCompleted(): Flow<List<FastSessionEntity>>

    @Query("SELECT * FROM fast_session WHERE endMillis IS NOT NULL ORDER BY endMillis ASC")
    suspend fun allCompleted(): List<FastSessionEntity>

    @Query("INSERT INTO fast_session(startMillis, endMillis, goalHours) VALUES(:start, NULL, :goalHours)")
    suspend fun insertActive(start: Long, goalHours: Int)

    @Query("UPDATE fast_session SET endMillis = :now WHERE endMillis IS NULL")
    suspend fun stopActive(now: Long)

    /** The only delete in this domain, and only ever of an *unfinished* fast — a mis-tap being
     * undone, not a record being removed. Completed sessions have no delete path at all. */
    @Query("DELETE FROM fast_session WHERE endMillis IS NULL")
    suspend fun deleteActive()

    /** Whole-row write — the import and the debug seed, which know both timestamps. */
    @Upsert
    suspend fun upsert(entity: FastSessionEntity)

    @Query("DELETE FROM fast_session")
    suspend fun clearAll()
}
