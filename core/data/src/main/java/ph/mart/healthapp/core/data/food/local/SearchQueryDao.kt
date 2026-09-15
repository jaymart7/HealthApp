package ph.mart.healthapp.core.data.food.local

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
internal interface SearchQueryDao {
    /** The most recently used queries, newest first. Capped by the caller — the screen shows three
     * and the table holds however many have ever been useful. */
    @Query("SELECT * FROM food_search_query ORDER BY lastUsedAt DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<SearchQueryEntity>>

    /** Records a query, or moves one already here to the front. Upsert, because the text is the
     * key. */
    // ponytail: no pruning — a few hundred short strings, and the read is already capped. Add a
    // trim if a diary ever accumulates enough of them to notice.
    @Upsert
    suspend fun record(entity: SearchQueryEntity)
}
