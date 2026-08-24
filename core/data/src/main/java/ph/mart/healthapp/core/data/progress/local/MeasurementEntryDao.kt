package ph.mart.healthapp.core.data.progress.local

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import kotlinx.coroutines.flow.Flow

@Dao
internal interface MeasurementEntryDao {
    @Query("SELECT * FROM measurement_entry ORDER BY date ASC")
    fun observeAll(): Flow<List<MeasurementEntryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: MeasurementEntryEntity)
}
