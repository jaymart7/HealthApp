package ph.mart.healthapp.core.data.health.local

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Upsert

@Dao
internal interface HealthLinkDao {
    @Upsert
    suspend fun upsert(entity: HealthLinkEntity)

    @Query("SELECT remoteName FROM health_link WHERE remoteName IN (:remoteNames)")
    suspend fun existing(remoteNames: List<String>): List<String>

    /** Where the next sync window starts. Null on a first sync — the caller picks the backfill. */
    @Query("SELECT MAX(remoteTimeMillis) FROM health_link WHERE dataType = :dataType AND pushed = 0")
    suspend fun latestImportedTime(dataType: String): Long?

    @Query("SELECT * FROM health_link WHERE pushed = :pushed")
    suspend fun links(pushed: Boolean): List<HealthLinkEntity>

    /** Which local rows have already been sent, so a push never sends the same meal twice. */
    @Query("SELECT localId FROM health_link WHERE pushed = 1 AND localTable = :localTable")
    suspend fun pushedLocalIds(localTable: String): List<Long>

    @Query("DELETE FROM health_link WHERE remoteName IN (:remoteNames)")
    suspend fun delete(remoteNames: List<String>)

    @Query("SELECT COUNT(*) FROM health_link WHERE pushed = 0")
    suspend fun importedCount(): Int

    @Query("DELETE FROM health_link")
    suspend fun clear()
}
