package ph.mart.healthapp.core.data.food.local

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import androidx.room3.Transaction
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

    /** Every *live* item, for every meal and recipe, so the grouping happens in Kotlin rather than
     * in a per-parent query. Items of the other kind are dropped by the grouping, which is driven
     * by the parent list.
     *
     * The subquery is what bounds it. `softDelete` retires the parent and leaves its items behind —
     * soft delete only, so they are never reclaimed — and without this filter every row a user ever
     * deleted was still read, and re-grouped, on each emission of a flow that both the diary panel
     * and the food library collect. */
    @Query(
        "SELECT * FROM saved_meal_item " +
            "WHERE mealId IN (SELECT id FROM saved_meal WHERE isDeleted = 0) ORDER BY id ASC",
    )
    fun observeItems(): Flow<List<SavedMealItemEntity>>

    @Insert
    suspend fun insertMeal(entity: SavedMealEntity): Long

    @Insert
    suspend fun insertItems(entities: List<SavedMealItemEntity>)

    /** The parent and its items in one transaction. Two separate writes could be interrupted
     * between them, and `groupSavedMeals` renders a parent with no items as an empty meal rather
     * than as the corruption it is — so the half-written state is invisible, which is what makes
     * the transaction worth having. The shape [FavoriteFoodDao.rename] and
     * [ph.mart.healthapp.core.data.supplement.local.SupplementDao.setTakenOn] already use. */
    @Transaction
    suspend fun insertMealWithItems(
        meal: SavedMealEntity,
        items: (mealId: Long) -> List<SavedMealItemEntity>,
    ): Long {
        val mealId = insertMeal(meal)
        insertItems(items(mealId))
        return mealId
    }

    @Query("UPDATE saved_meal SET isDeleted = 1 WHERE id = :id")
    suspend fun softDelete(id: Long)

    /**
     * An edit from the food library: the old row is soft-deleted and the new one inserted with its
     * items, in one transaction — `FoodEntryDao.replace`'s rule, so the library never emits a frame
     * with the recipe in neither place. Items carry no delete flag of their own; the old ones go
     * out of view with their parent through [observeItems]'s subquery.
     *
     * The caller passes `servings` through unchanged, so a saved meal stays a saved meal.
     */
    @Transaction
    suspend fun replace(
        id: Long,
        meal: SavedMealEntity,
        items: (mealId: Long) -> List<SavedMealItemEntity>,
    ): Long {
        softDelete(id)
        return insertMealWithItems(meal, items)
    }
}
