package ph.mart.healthapp.core.data.food

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import ph.mart.healthapp.core.data.food.local.FavoriteFoodDao
import ph.mart.healthapp.core.data.food.local.FavoriteFoodEntity
import ph.mart.healthapp.core.data.food.local.FoodEntryDao
import ph.mart.healthapp.core.data.food.local.FoodEntryEntity
import ph.mart.healthapp.core.data.food.local.SavedMealDao
import ph.mart.healthapp.core.data.food.local.SavedMealEntity
import ph.mart.healthapp.core.data.food.local.SavedMealItemEntity
import ph.mart.healthapp.core.data.forToday
import ph.mart.healthapp.core.data.todayEpochDay

/** Recents are read a little deeper than [MAX_SUGGESTIONS], so favorites crowding the front of
 * the merged list don't starve it of recents. */
private const val RECENT_LIMIT = MAX_SUGGESTIONS * 2

/** The library screen wants every row. A number rather than a second query without `LIMIT`, so
 * both windows go through the same DAO method and can't diverge in filter or order. */
private const val NO_LIMIT = Int.MAX_VALUE

internal class FoodRepositoryImpl(
    private val dao: FoodEntryDao,
    private val favoriteDao: FavoriteFoodDao,
    private val savedMealDao: SavedMealDao,
) : FoodRepository {

    override fun observeTodayEntries(): Flow<List<FoodEntry>> = forToday(::observeEntries)

    override fun observeEntries(dateEpochDay: Long): Flow<List<FoodEntry>> =
        dao.observeForDate(dateEpochDay).map { entities -> entities.map { it.toFoodEntry() } }

    override suspend fun addEntry(entry: FoodEntry) {
        // A dated entry arrives from an import, or from the diary pointed at a past day; anything
        // logged without one is "today".
        val date = entry.dateEpochDay.takeIf { it > 0 } ?: todayEpochDay()
        dao.insert(entry.toEntity(date = date, loggedAt = System.currentTimeMillis()))
    }

    override suspend fun addEntries(entries: List<FoodEntry>) {
        val loggedAt = System.currentTimeMillis()
        dao.insertAll(
            entries.map { entry ->
                entry.toEntity(date = entry.dateEpochDay.takeIf { it > 0 } ?: todayEpochDay(), loggedAt = loggedAt)
            },
        )
    }

    override suspend fun updateEntry(entry: FoodEntry) {
        val date = entry.dateEpochDay.takeIf { it > 0 } ?: todayEpochDay()
        // The original logging time comes across, so a corrected row doesn't jump to the bottom
        // of its meal section — every read of this table orders by it.
        val loggedAt = dao.loggedAt(entry.id) ?: System.currentTimeMillis()
        dao.replace(entry.id, entry.toEntity(date = date, loggedAt = loggedAt))
    }

    override suspend fun deleteEntry(id: Long) {
        dao.softDelete(id)
    }

    override suspend fun allEntries(): List<FoodEntry> = dao.allActive().map { it.toFoodEntry() }

    override suspend fun deleteAllEntries() {
        dao.softDeleteAll()
    }

    override fun observeSuggestions(): Flow<List<FoodSuggestion>> =
        combine(dao.observeRecent(RECENT_LIMIT, QUICK_ADD_NAME), favoriteDao.observeFavorites()) { recents, favorites ->
            mergeSuggestions(
                recents = recents.map { it.toSuggestion() },
                favorites = favorites.map { it.toSuggestion() },
            )
        }

    override suspend fun setFavorite(suggestion: FoodSuggestion, favorite: Boolean) {
        if (favorite) favoriteDao.upsert(suggestion.toEntity()) else favoriteDao.clearFavorite(suggestion.name)
    }

    override fun observeMyFoods(): Flow<List<ScannedProduct>> =
        favoriteDao.observeFavorites().map { rows -> rows.map { it.toProduct() } }

    override suspend fun deleteMyFood(name: String) {
        favoriteDao.clearFavorite(name)
    }

    override suspend fun renameMyFood(oldName: String, newName: String) {
        favoriteDao.rename(oldName, newName)
    }

    override fun observeSavedMeals(): Flow<List<SavedMeal>> = savedMeals(MAX_SAVED_MEALS)

    override fun observeAllSavedMeals(): Flow<List<SavedMeal>> = savedMeals(NO_LIMIT)

    /** One join for both windows, so the panel's list and the library's cannot drift apart in how
     * they group or order. */
    private fun savedMeals(limit: Int): Flow<List<SavedMeal>> =
        combine(savedMealDao.observeMeals(limit), savedMealDao.observeItems()) { meals, items ->
            groupSavedMeals(meals, items)
        }

    override suspend fun saveMeal(name: String, items: List<SavedMealItem>) {
        val mealId = savedMealDao.insertMeal(
            SavedMealEntity(name = name, createdAt = System.currentTimeMillis()),
        )
        savedMealDao.insertItems(items.map { it.toEntity(mealId) })
    }

    override suspend fun deleteSavedMeal(id: Long) {
        savedMealDao.softDelete(id)
    }

    override suspend fun renameSavedMeal(id: Long, name: String) {
        savedMealDao.rename(id, name)
    }

    override fun observeRecipes(): Flow<List<Recipe>> = recipes(MAX_RECIPES)

    override fun observeAllRecipes(): Flow<List<Recipe>> = recipes(NO_LIMIT)

    /** Twin of [savedMeals]. */
    private fun recipes(limit: Int): Flow<List<Recipe>> =
        combine(savedMealDao.observeRecipes(limit), savedMealDao.observeItems()) { recipes, items ->
            groupRecipes(recipes, items)
        }

    override suspend fun saveRecipe(name: String, servings: Int, items: List<SavedMealItem>) {
        val recipeId = savedMealDao.insertMeal(
            SavedMealEntity(name = name, createdAt = System.currentTimeMillis(), servings = servings),
        )
        savedMealDao.insertItems(items.map { it.toEntity(recipeId) })
    }

    /** Same soft delete as [deleteSavedMeal] — one table — but named for what the caller is
     * holding, so the recipe UI doesn't read like it's deleting a meal. */
    override suspend fun deleteRecipe(id: Long) {
        savedMealDao.softDelete(id)
    }

    /** Same one-column update as [renameSavedMeal], named for what the caller is holding. */
    override suspend fun renameRecipe(id: Long, name: String) {
        savedMealDao.rename(id, name)
    }

    // Anchored on today here, not in the feature layer: todayEpochDay() is internal to this
    // module, and the window has to match the query's lower bound exactly for the series to stay
    // dense. It rides forToday so the dense series gains its new day at midnight rather than
    // ending yesterday for as long as the process lives.
    override fun observeDailyNutrition(): Flow<List<DayNutrition>> = forToday { today ->
        val from = today - TREND_WINDOW_DAYS
        dao.observeSince(from).map { entities ->
            entities.map { it.toFoodEntry() }.dailySeries(fromEpochDay = from, toEpochDay = today)
        }
    }
}

/** Joins the two saved-meal tables in Kotlin — driven by [meals], so items whose parent was
 * soft-deleted (or fell past the recency limit) are dropped, and a meal with no items still
 * appears. */
internal fun groupSavedMeals(
    meals: List<SavedMealEntity>,
    items: List<SavedMealItemEntity>,
): List<SavedMeal> {
    val byMeal = items.groupBy { it.mealId }
    return meals.map { meal ->
        SavedMeal(
            id = meal.id,
            name = meal.name,
            items = byMeal[meal.id].orEmpty().map { it.toSavedMealItem() },
        )
    }
}

/** Twin of [groupSavedMeals] over the same two tables, for the rows that carry a servings count.
 * A null [SavedMealEntity.servings] can't reach here — the query filters it — but it is coerced
 * rather than forced, since a crash is a poor answer to a stray row. */
internal fun groupRecipes(
    recipes: List<SavedMealEntity>,
    items: List<SavedMealItemEntity>,
): List<Recipe> {
    val byRecipe = items.groupBy { it.mealId }
    return recipes.map { recipe ->
        Recipe(
            id = recipe.id,
            name = recipe.name,
            servings = (recipe.servings ?: 1).coerceAtLeast(1),
            items = byRecipe[recipe.id].orEmpty().map { it.toSavedMealItem() },
        )
    }
}

private fun SavedMealItemEntity.toSavedMealItem() = SavedMealItem(
    name = name,
    portionAmount = portionAmount,
    portionUnit = portionUnit,
    calories = calories,
    proteinG = proteinG,
    carbsG = carbsG,
    fatG = fatG,
    fiberG = fiberG,
    sugarG = sugarG,
    sodiumMg = sodiumMg,
)

private fun SavedMealItem.toEntity(mealId: Long) = SavedMealItemEntity(
    mealId = mealId,
    name = name,
    portionAmount = portionAmount,
    portionUnit = portionUnit,
    calories = calories,
    proteinG = proteinG,
    carbsG = carbsG,
    fatG = fatG,
    fiberG = fiberG,
    sugarG = sugarG,
    sodiumMg = sodiumMg,
)

private fun FoodEntryEntity.toFoodEntry() = FoodEntry(
    id = id,
    name = name,
    dateEpochDay = date,
    mealType = MealType.valueOf(mealType),
    portionAmount = portionAmount,
    portionUnit = portionUnit,
    calories = calories,
    proteinG = proteinG,
    carbsG = carbsG,
    fatG = fatG,
    fiberG = fiberG,
    sugarG = sugarG,
    sodiumMg = sodiumMg,
)

private fun FoodEntryEntity.toSuggestion() = FoodSuggestion(
    name = name,
    portionAmount = portionAmount,
    portionUnit = portionUnit,
    calories = calories,
    proteinG = proteinG,
    carbsG = carbsG,
    fatG = fatG,
    fiberG = fiberG,
    sugarG = sugarG,
    sodiumMg = sodiumMg,
    // A recent only knows it isn't starred; mergeSuggestions drops it if a favorite claims the name.
    isFavorite = false,
)

private fun FavoriteFoodEntity.toSuggestion() = FoodSuggestion(
    name = name,
    portionAmount = portionAmount,
    portionUnit = portionUnit,
    calories = calories,
    proteinG = proteinG,
    carbsG = carbsG,
    fatG = fatG,
    fiberG = fiberG,
    sugarG = sugarG,
    sodiumMg = sodiumMg,
    isFavorite = true,
)

/** The same row the suggestion panel stars, as the food search returns it. [ScannedProduct] rather
 * than a third type: a barcode hit, a built-in staple and one of the user's own foods all seed the
 * add-entry form through the one `ScannedProduct.toAddEntryForm()`. */
private fun FavoriteFoodEntity.toProduct() = ScannedProduct(
    name = name,
    portionAmount = portionAmount,
    portionUnit = portionUnit,
    calories = calories,
    proteinG = proteinG,
    carbsG = carbsG,
    fatG = fatG,
    fiberG = fiberG,
    sugarG = sugarG,
    sodiumMg = sodiumMg,
)

private fun FoodSuggestion.toEntity() = FavoriteFoodEntity(
    name = name,
    portionAmount = portionAmount,
    portionUnit = portionUnit,
    calories = calories,
    proteinG = proteinG,
    carbsG = carbsG,
    fatG = fatG,
    fiberG = fiberG,
    sugarG = sugarG,
    sodiumMg = sodiumMg,
)

private fun FoodEntry.toEntity(date: Long, loggedAt: Long) = FoodEntryEntity(
    id = id,
    name = name,
    mealType = mealType.name,
    date = date,
    loggedAt = loggedAt,
    portionAmount = portionAmount,
    portionUnit = portionUnit,
    calories = calories,
    proteinG = proteinG,
    carbsG = carbsG,
    fatG = fatG,
    fiberG = fiberG,
    sugarG = sugarG,
    sodiumMg = sodiumMg,
)
