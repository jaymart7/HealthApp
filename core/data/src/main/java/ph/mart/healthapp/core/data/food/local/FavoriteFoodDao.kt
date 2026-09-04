package ph.mart.healthapp.core.data.food.local

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
internal interface FavoriteFoodDao {
    @Query("SELECT * FROM favorite_food WHERE isFavorite = 1 ORDER BY name ASC")
    fun observeFavorites(): Flow<List<FavoriteFoodEntity>>

    @Upsert
    suspend fun upsert(entity: FavoriteFoodEntity)

    @Query("UPDATE favorite_food SET isFavorite = 0 WHERE name = :name")
    suspend fun clearFavorite(name: String)

    @Query("SELECT * FROM favorite_food WHERE name = :name")
    suspend fun find(name: String): FavoriteFoodEntity?

    /** [name] is the primary key, so a rename is a move, not an `UPDATE`: the row is written again
     * under the new name and the old one is retired. One transaction, so the food library never
     * emits a frame with the row in neither place — `FoodEntryDao.replace`'s rule.
     *
     * The old name stays behind as an `isFavorite = 0` tombstone, keeping its macros for a re-star,
     * which is what the table has always done. Renaming onto a name that already exists overwrites
     * it: name *is* the identity of a food here. */
    @Transaction
    suspend fun rename(oldName: String, newName: String) {
        val row = find(oldName) ?: return
        if (oldName == newName) return
        upsert(row.copy(name = newName))
        clearFavorite(oldName)
    }
}
