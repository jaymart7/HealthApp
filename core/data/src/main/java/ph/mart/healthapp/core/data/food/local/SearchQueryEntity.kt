package ph.mart.healthapp.core.data.food.local

import androidx.room3.Entity
import androidx.room3.PrimaryKey

/**
 * A query the history search has been used with, so the screen can offer it back.
 *
 * The query text **is** the key: asking for "chicken" twice is one recent query with a newer
 * timestamp, not two rows. That is also why this table has no `isDeleted` column and breaks the
 * soft-delete rule the domains follow — nothing here is the user's data. It is a list of words
 * they typed, reconstructible by typing them again, and a row that stops being offered is one that
 * fell past the `LIMIT`, not one that was deleted.
 */
@Entity(tableName = "food_search_query")
internal data class SearchQueryEntity(
    @PrimaryKey val query: String,
    val lastUsedAt: Long,
)
