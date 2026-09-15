package ph.mart.healthapp.core.data.food.local

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import androidx.room3.Transaction
import kotlinx.coroutines.flow.Flow
import ph.mart.healthapp.core.data.food.SearchCount

@Dao
internal interface FoodEntryDao {
    @Query("SELECT * FROM food_entry WHERE date = :date AND isDeleted = 0 ORDER BY loggedAt ASC")
    fun observeForDate(date: Long): Flow<List<FoodEntryEntity>>

    /** One row per distinct food name — the most recently inserted one, newest first. The
     * `MAX(id)` subquery is what makes "the row for this name" well-defined; a bare
     * `GROUP BY name` would leave the non-aggregate columns up to SQLite.
     *
     * [exclude] is the quick-add name: every nameless entry collapses into one row under it, and
     * "Quick add · 1 serving · 650 kcal" is not a food anyone wants to re-log. */
    @Query(
        "SELECT * FROM food_entry WHERE id IN " +
            "(SELECT MAX(id) FROM food_entry WHERE isDeleted = 0 AND name != :exclude GROUP BY name) " +
            "ORDER BY loggedAt DESC LIMIT :limit",
    )
    fun observeRecent(limit: Int, exclude: String): Flow<List<FoodEntryEntity>>

    /**
     * One page of the logged rows whose name matches, newest first — the diary's history search.
     *
     * Bounded for the reason every other read in here is: the whole table is only ever read by
     * export. It is a *page* rather than a ceiling, though — [offset] is where the last page ended,
     * and the screen appends rather than stopping at a cap. [pattern] arrives already wrapped and
     * escaped — see [likeContains][ph.mart.healthapp.core.data.food.likeContains], which is the
     * other half of the `ESCAPE` clause below.
     *
     * Suspend rather than a [Flow]: the screen re-asks on every keystroke, and a flow would tear
     * down and re-subscribe a query each time instead of just running it.
     *
     * [meal] is the screen's meal filter, `null` for "All". It is applied **here** rather than over
     * the returned list, so a page counts rows the user asked for: filtering after `LIMIT` would
     * hand back whatever survived the newest page, which is not the newest page of lunches.
     *
     * The `WHERE` clause is repeated character for character by [searchCount], which counts what
     * this pages through. Change one and change the other, or the count and the rows disagree.
     */
    // ponytail: no debounce — one query per keystroke over a local table of a few thousand rows.
    // Debounce the caller if a diary ever gets big enough to feel it.
    @Query(
        "SELECT * FROM food_entry WHERE isDeleted = 0 AND name LIKE :pattern ESCAPE '\\' " +
            "AND (:meal IS NULL OR mealType = :meal) " +
            "ORDER BY date DESC, loggedAt DESC LIMIT :limit OFFSET :offset",
    )
    suspend fun searchByName(pattern: String, meal: String?, limit: Int, offset: Int): List<FoodEntryEntity>

    /**
     * How much there is to page through — the two figures the history's count line carries.
     *
     * A second read rather than a count over the rows, for [dayTotals]'s reason one level up: a
     * page cannot say how big the thing it is a page *of* is, and "50 matches" when there are two
     * hundred is the page size reported as an answer.
     *
     * The `WHERE` clause is [searchByName]'s, repeated — see the note there.
     */
    @Query(
        "SELECT COUNT(*) AS matches, COUNT(DISTINCT date) AS days FROM food_entry " +
            "WHERE isDeleted = 0 AND name LIKE :pattern ESCAPE '\\' " +
            "AND (:meal IS NULL OR mealType = :meal)",
    )
    suspend fun searchCount(pattern: String, meal: String?): SearchCount

    /**
     * What each of [dates] came to across the **whole** day, matched rows and unmatched alike —
     * the figure the history list's day header carries.
     *
     * A second read rather than a sum over the results: a day header saying "412 kcal" because
     * that is what the word "chicken" matched would be a claim about the day that isn't true.
     */
    @Query(
        "SELECT date, SUM(calories) AS kcal FROM food_entry " +
            "WHERE date IN (:dates) AND isDeleted = 0 GROUP BY date",
    )
    suspend fun dayTotals(dates: List<Long>): List<DayTotal>

    /** Bounded history for the Nutrition trend — the whole table is only ever read by export. */
    @Query("SELECT * FROM food_entry WHERE date >= :from AND isDeleted = 0 ORDER BY date ASC, loggedAt ASC")
    fun observeSince(from: Long): Flow<List<FoodEntryEntity>>

    @Query("SELECT * FROM food_entry WHERE isDeleted = 0 ORDER BY date ASC, loggedAt ASC")
    suspend fun allActive(): List<FoodEntryEntity>

    /** The meals that kept their photo, newest first — the Progress tab's meal-photo history.
     * Capped for the same reason every other read here is: the whole table is only ever read by
     * export. */
    @Query(
        "SELECT * FROM food_entry WHERE photoPath IS NOT NULL AND isDeleted = 0 " +
            "ORDER BY date DESC, loggedAt DESC LIMIT :limit",
    )
    fun observeWithPhoto(limit: Int): Flow<List<FoodEntryEntity>>

    /** Every path on disk, newest first — **including soft-deleted rows**, whose files are still
     * there and still have to be reclaimable. What the prune counts down from. */
    @Query("SELECT photoPath FROM food_entry WHERE photoPath IS NOT NULL ORDER BY date DESC, loggedAt DESC")
    suspend fun photoPaths(): List<String>

    /** Forgets an aged-out photo. The meal itself is untouched — a row whose picture was pruned is
     * still every calorie it ever was. */
    @Query("UPDATE food_entry SET photoPath = NULL WHERE photoPath IN (:paths)")
    suspend fun clearPhotos(paths: List<String>)

    @Insert
    suspend fun insert(entity: FoodEntryEntity)

    @Insert
    suspend fun insertAll(entities: List<FoodEntryEntity>)

    @Query("UPDATE food_entry SET isDeleted = 1 WHERE id = :id")
    suspend fun softDelete(id: Long)

    /** The row's original logging time, so an edit can keep it and stay where it is in the day. */
    @Query("SELECT loggedAt FROM food_entry WHERE id = :id")
    suspend fun loggedAt(id: Long): Long?

    /**
     * An edit supersedes a row rather than rewriting it — see
     * [FoodRepository.updateEntry][ph.mart.healthapp.core.data.food.FoodRepository.updateEntry]
     * for why the id changes.
     *
     * One transaction, so the diary's flow sees one emission rather than a frame with the row gone.
     */
    @Transaction
    suspend fun replace(id: Long, entity: FoodEntryEntity) {
        softDelete(id)
        // `id = 0` because the old row is still there — soft-deleted, not gone — and the insert
        // that follows is a *new* row, which is what superseding means. Carrying the caller's id
        // across would re-insert a primary key the table still holds and abort the transaction.
        insert(entity.copy(id = 0))
    }

    @Query("UPDATE food_entry SET isDeleted = 1")
    suspend fun softDeleteAll()
}

/** One row of [FoodEntryDao.dayTotals] — a day and what it came to. A projection rather than a
 * `Map` return, because Room reads a `Map` by grouping a cursor it has to be told how to key. */
internal data class DayTotal(val date: Long, val kcal: Int)
