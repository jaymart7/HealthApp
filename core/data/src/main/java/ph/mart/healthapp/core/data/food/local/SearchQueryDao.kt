package ph.mart.healthapp.core.data.food.local

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
internal interface SearchQueryDao {
    /** The most recently used strings of one [kind], newest first. Capped by the caller — each
     * screen shows three and the table holds however many have ever been useful. */
    @Query("SELECT * FROM food_search_query WHERE kind = :kind ORDER BY lastUsedAt DESC LIMIT :limit")
    fun observeRecent(kind: String, limit: Int): Flow<List<SearchQueryEntity>>

    /** Records one, or moves one already here to the front. Upsert, because the kind and the text
     * are together the key. */
    // ponytail: no pruning — a few hundred short strings across both kinds, and the reads are
    // already capped. Add a trim if a diary ever accumulates enough of them to notice.
    @Upsert
    suspend fun record(entity: SearchQueryEntity)
}
