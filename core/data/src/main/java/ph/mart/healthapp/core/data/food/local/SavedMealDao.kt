package ph.mart.healthapp.core.data.food.local

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import kotlinx.coroutines.flow.Flow

@Dao
internal interface SavedMealDao {
    /** Newest first — id order is save order, and nothing renumbers it. `servings IS NULL` is what
     * keeps recipes out: the two share the table, so without it a recipe would evict a saved meal
     * from this limit. */
    @Query("SELECT * FROM saved_meal WHERE isDeleted = 0 AND servings IS NULL ORDER BY id DESC LIMIT :limit")
    fun observeMeals(limit: Int): Flow<List<SavedMealEntity>>

    /** The other half of [observeMeals] — same table, same order, the rows that carry a servings
     * count. */
    @Query("SELECT * FROM saved_meal WHERE isDeleted = 0 AND servings IS NOT NULL ORDER BY id DESC LIMIT :limit")
    fun observeRecipes(limit: Int): Flow<List<SavedMealEntity>>

    /** Every item, for every meal and recipe — a handful of rows in total, so the grouping happens
     * in Kotlin rather than in a per-parent query. Items of the other kind are dropped by the
     * grouping, which is driven by the parent list. */
    @Query("SELECT * FROM saved_meal_item ORDER BY id ASC")
    fun observeItems(): Flow<List<SavedMealItemEntity>>

    @Insert
    suspend fun insertMeal(entity: SavedMealEntity): Long

    @Insert
    suspend fun insertItems(entities: List<SavedMealItemEntity>)

    @Query("UPDATE saved_meal SET isDeleted = 1 WHERE id = :id")
    suspend fun softDelete(id: Long)

    /** Only the name. A recipe and a saved meal are the same row shape here, and `servings` is
     * what tells them apart — an upsert of the whole entity would be one typo away from turning
     * a recipe into a saved meal. */
    @Query("UPDATE saved_meal SET name = :name WHERE id = :id")
    suspend fun rename(id: Long, name: String)
}
