package ph.mart.healthapp.core.data.food.local

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import kotlinx.coroutines.flow.Flow

@Dao
internal interface FoodEntryDao {
    @Query("SELECT * FROM food_entry WHERE date = :date AND isDeleted = 0 ORDER BY loggedAt ASC")
    fun observeForDate(date: Long): Flow<List<FoodEntryEntity>>

    /** One row per distinct food name — the most recently inserted one, newest first. The
     * `MAX(id)` subquery is what makes "the row for this name" well-defined; a bare
     * `GROUP BY name` would leave the non-aggregate columns up to SQLite. */
    @Query(
        "SELECT * FROM food_entry WHERE id IN " +
            "(SELECT MAX(id) FROM food_entry WHERE isDeleted = 0 GROUP BY name) " +
            "ORDER BY loggedAt DESC LIMIT :limit",
    )
    fun observeRecent(limit: Int): Flow<List<FoodEntryEntity>>

    /** Bounded history for the Nutrition trend — the whole table is only ever read by export. */
    @Query("SELECT * FROM food_entry WHERE date >= :from AND isDeleted = 0 ORDER BY date ASC, loggedAt ASC")
    fun observeSince(from: Long): Flow<List<FoodEntryEntity>>

    @Query("SELECT * FROM food_entry WHERE isDeleted = 0 ORDER BY date ASC, loggedAt ASC")
    suspend fun allActive(): List<FoodEntryEntity>

    @Insert
    suspend fun insert(entity: FoodEntryEntity)

    @Insert
    suspend fun insertAll(entities: List<FoodEntryEntity>)

    @Query("UPDATE food_entry SET isDeleted = 1 WHERE id = :id")
    suspend fun softDelete(id: Long)

    @Query("UPDATE food_entry SET isDeleted = 1")
    suspend fun softDeleteAll()
}
