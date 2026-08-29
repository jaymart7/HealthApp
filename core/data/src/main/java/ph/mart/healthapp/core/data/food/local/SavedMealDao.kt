package ph.mart.healthapp.core.data.food.local

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import kotlinx.coroutines.flow.Flow

@Dao
internal interface SavedMealDao {
    /** Newest first — id order is save order, and nothing renumbers it. */
    @Query("SELECT * FROM saved_meal WHERE isDeleted = 0 ORDER BY id DESC LIMIT :limit")
    fun observeMeals(limit: Int): Flow<List<SavedMealEntity>>

    /** Every item, for every meal — a handful of rows in total, so the grouping happens in Kotlin
     * rather than in a per-meal query. */
    @Query("SELECT * FROM saved_meal_item ORDER BY id ASC")
    fun observeItems(): Flow<List<SavedMealItemEntity>>

    @Insert
    suspend fun insertMeal(entity: SavedMealEntity): Long

    @Insert
    suspend fun insertItems(entities: List<SavedMealItemEntity>)

    @Query("UPDATE saved_meal SET isDeleted = 1 WHERE id = :id")
    suspend fun softDelete(id: Long)
}
