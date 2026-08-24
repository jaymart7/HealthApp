package ph.mart.healthapp.core.data.progress.local

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import kotlinx.coroutines.flow.Flow

@Dao
internal interface ProgressPhotoDao {
    @Query("SELECT * FROM progress_photo ORDER BY date ASC")
    fun observeAll(): Flow<List<ProgressPhotoEntity>>

    @Insert
    suspend fun insert(entity: ProgressPhotoEntity)
}
