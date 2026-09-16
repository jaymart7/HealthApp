package ph.mart.healthapp.core.data.food.local

import androidx.room3.Entity

/**
 * A string the user typed into a field that turned out to be worth keeping, so the screen it was
 * typed into can offer it back.
 *
 * Two kinds live here — the history search's queries and talk-to-log's sentences — separated by
 * [kind] rather than by a second table. They are the same row with the same rules and differ only
 * in who reads them; a parallel entity and DAO would have been this file twice.
 *
 * The text **is** the key, per kind: asking for "chicken" twice is one recent query with a newer
 * timestamp, not two rows. That is also why this table has no `isDeleted` column and breaks the
 * soft-delete rule the domains follow — nothing here is the user's data. It is a list of words
 * they typed, reconstructible by typing them again, and a row that stops being offered is one that
 * fell past the `LIMIT`, not one that was deleted.
 */
@Entity(tableName = "food_search_query", primaryKeys = ["kind", "query"])
internal data class SearchQueryEntity(
    /** Which field it was typed into — `SEARCH_KIND` or `VOICE_KIND`, both in `FoodRepositoryImpl`. */
    val kind: String,
    val query: String,
    val lastUsedAt: Long,
)
