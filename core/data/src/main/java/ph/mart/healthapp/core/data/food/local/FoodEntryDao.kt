package ph.mart.healthapp.core.data.food.local

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import kotlinx.coroutines.flow.Flow

@Dao
internal interface FoodEntryDao {
    @Query("SELECT * FROM food_entry WHERE date = :date AND isDeleted = 0 ORDER BY loggedAt ASC")
    fun observeForDate(date: Long): Flow<List<FoodEntryEntity>>

    @Insert
    suspend fun insert(entity: FoodEntryEntity)

    @Query("UPDATE food_entry SET isDeleted = 1 WHERE id = :id")
    suspend fun softDelete(id: Long)
}
