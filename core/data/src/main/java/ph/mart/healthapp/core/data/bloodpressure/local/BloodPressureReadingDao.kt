package ph.mart.healthapp.core.data.bloodpressure.local

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import kotlinx.coroutines.flow.Flow

@Dao
internal interface BloodPressureReadingDao {
    @Query("SELECT * FROM blood_pressure_reading WHERE isDeleted = 0 ORDER BY takenAtMillis ASC")
    fun observeAll(): Flow<List<BloodPressureReadingEntity>>

    /** Home's card. The newest reading whenever it was taken, not today's — see the card. */
    @Query("SELECT * FROM blood_pressure_reading WHERE isDeleted = 0 ORDER BY takenAtMillis DESC LIMIT 1")
    fun observeLatest(): Flow<BloodPressureReadingEntity?>

    @Query("SELECT * FROM blood_pressure_reading WHERE isDeleted = 0 ORDER BY takenAtMillis ASC")
    suspend fun allActive(): List<BloodPressureReadingEntity>

    /** Returns the new row id, which a Health Connect import records against the record. */
    @Insert
    suspend fun insert(entity: BloodPressureReadingEntity): Long

    @Query("UPDATE blood_pressure_reading SET isDeleted = 1 WHERE id = :id")
    suspend fun softDelete(id: Long)

    @Query("UPDATE blood_pressure_reading SET isDeleted = 1")
    suspend fun softDeleteAll()
}
