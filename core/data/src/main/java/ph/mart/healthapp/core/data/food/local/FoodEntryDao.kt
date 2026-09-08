package ph.mart.healthapp.core.data.food.local

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import androidx.room3.Transaction
import kotlinx.coroutines.flow.Flow

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
