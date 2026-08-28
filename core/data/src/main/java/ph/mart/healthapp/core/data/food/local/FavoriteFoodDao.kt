package ph.mart.healthapp.core.data.food.local

import androidx.room3.Dao
import androidx.room3.Query
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
}
