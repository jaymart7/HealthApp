package ph.mart.healthapp.core.data.food

import android.content.Context
import android.graphics.Bitmap
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
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

/** Where a kept plate lives. Beside `progress_photos/`, and excluded from the cloud backup for the
 * same reason it is — see `backup_rules.xml`. */
private const val PHOTO_DIR = "meal_photos"

/** JPEG quality for a stored plate. Below the progress photos' 90 because these are thumbnails and
 * a gallery frame, never a before/after comparison anyone studies. */
private const val PHOTO_QUALITY = 85

internal class FoodRepositoryImpl(
    private val context: Context,
    private val dao: FoodEntryDao,
    private val favoriteDao: FavoriteFoodDao,
    private val savedMealDao: SavedMealDao,
) : FoodRepository {

    override fun observeTodayEntries(): Flow<List<FoodEntry>> = forToday(::observeEntries)

    override fun observeEntries(dateEpochDay: Long): Flow<List<FoodEntry>> =
        dao.observeForDate(dateEpochDay).map { entities -> entities.map { it.toFoodEntry() } }

    override suspend fun addEntry(entry: FoodEntry, photo: Bitmap?) {
        // A dated entry arrives from an import, or from the diary pointed at a past day; anything
        // logged without one is "today".
        val date = entry.dateEpochDay.takeIf { it > 0 } ?: todayEpochDay()
        val path = photo?.let { writePhoto(it) }
        dao.insert(
            entry.copy(photoPath = path ?: entry.photoPath)
                .toEntity(date = date, loggedAt = System.currentTimeMillis()),
        )
        // After the insert, so the row being written is itself the newest one the cap counts.
        if (path != null) prunePhotos()
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

    override suspend fun searchEntries(query: String): List<FoodEntry> =
        dao.searchByName(likeContains(query), MAX_HISTORY_RESULTS).map { it.toFoodEntry() }

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
    override fun observeMealPhotos(): Flow<List<FoodEntry>> =
        dao.observeWithPhoto(MAX_MEAL_PHOTOS).map { entities -> entities.map { it.toFoodEntry() } }

    /**
     * Scales the plate to [MEAL_PHOTO_EDGE] and writes it, returning the path — or null if the
     * write failed, which logs the meal without its picture rather than losing the meal.
     *
     * The scale is what separates this from [ProgressRepository.addPhoto][ph.mart.healthapp.core.data.progress.ProgressRepository.addPhoto],
     * which compresses the capture as it stands: a progress photo is taken every fortnight and one
     * per meal is three a day.
     */
    private suspend fun writePhoto(photo: Bitmap): String? = withContext(Dispatchers.IO) {
        runCatching {
            val dir = File(context.filesDir, PHOTO_DIR).apply { mkdirs() }
            val file = File(dir, "${UUID.randomUUID()}.jpg")
            FileOutputStream(file).use { out ->
                photo.scaledToEdge(MEAL_PHOTO_EDGE).compress(Bitmap.CompressFormat.JPEG, PHOTO_QUALITY, out)
            }
            file.absolutePath
        }.getOrNull()
    }

    /** Drops everything past the newest [MAX_MEAL_PHOTOS]: the files go, the column is nulled, and
     * the meals themselves are untouched. Soft-deleted rows are counted too — their files are on
     * the same disk. */
    private suspend fun prunePhotos() {
        val stale = dao.photoPaths().drop(MAX_MEAL_PHOTOS)
        if (stale.isEmpty()) return
        withContext(Dispatchers.IO) { stale.forEach { File(it).delete() } }
        dao.clearPhotos(stale)
    }

    override fun observeDailyNutrition(): Flow<List<DayNutrition>> = forToday { today ->
        val from = today - TREND_WINDOW_DAYS
        dao.observeSince(from).map { entities ->
            entities.map { it.toFoodEntry() }.dailySeries(fromEpochDay = from, toEpochDay = today)
        }
    }
}

/** The bitmap at most [edge] on its long side, or itself when it is already smaller — a picked
 * gallery image can be either. `filter = true` because this is a real downscale, and nearest
 * neighbour on a 1280 → 768 reduction is visibly ragged. */
private fun Bitmap.scaledToEdge(edge: Int): Bitmap {
    val longEdge = maxOf(width, height)
    if (longEdge <= edge) return this
    val factor = edge.toDouble() / longEdge
    return Bitmap.createScaledBitmap(this, (width * factor).toInt(), (height * factor).toInt(), true)
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
    nutrients = nutrients,
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
    nutrients = nutrients,
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
    nutrients = nutrients,
    photoPath = photoPath,
)

private fun FoodEntryEntity.toSuggestion() = FoodSuggestion(
    name = name,
    portionAmount = portionAmount,
    portionUnit = portionUnit,
    calories = calories,
    proteinG = proteinG,
    carbsG = carbsG,
    fatG = fatG,
    nutrients = nutrients,
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
    nutrients = nutrients,
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
    nutrients = nutrients,
)

private fun FoodSuggestion.toEntity() = FavoriteFoodEntity(
    name = name,
    portionAmount = portionAmount,
    portionUnit = portionUnit,
    calories = calories,
    proteinG = proteinG,
    carbsG = carbsG,
    fatG = fatG,
    nutrients = nutrients,
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
    nutrients = nutrients,
    // Carried both ways, which is the whole of "an edit keeps its photo": updateEntry rebuilds
    // the entity from the entry the form produced, and the row it supersedes is gone.
    photoPath = photoPath,
)
