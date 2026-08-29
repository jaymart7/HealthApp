package ph.mart.healthapp.core.data.food

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import ph.mart.healthapp.core.data.food.local.FavoriteFoodDao
import ph.mart.healthapp.core.data.food.local.FavoriteFoodEntity
import ph.mart.healthapp.core.data.food.local.FoodEntryDao
import ph.mart.healthapp.core.data.food.local.FoodEntryEntity
import ph.mart.healthapp.core.data.todayEpochDay

/** Recents are read a little deeper than [MAX_SUGGESTIONS], so favorites crowding the front of
 * the merged list don't starve it of recents. */
private const val RECENT_LIMIT = MAX_SUGGESTIONS * 2

internal class FoodRepositoryImpl(
    private val dao: FoodEntryDao,
    private val favoriteDao: FavoriteFoodDao,
) : FoodRepository {

    override fun observeTodayEntries(): Flow<List<FoodEntry>> = observeEntries(todayEpochDay())

    override fun observeEntries(dateEpochDay: Long): Flow<List<FoodEntry>> =
        dao.observeForDate(dateEpochDay).map { entities -> entities.map { it.toFoodEntry() } }

    override suspend fun addEntry(entry: FoodEntry) {
        // A dated entry arrives from an import, or from the diary pointed at a past day; anything
        // logged without one is "today".
        val date = entry.dateEpochDay.takeIf { it > 0 } ?: todayEpochDay()
        dao.insert(entry.toEntity(date = date, loggedAt = System.currentTimeMillis()))
    }

    override suspend fun deleteEntry(id: Long) {
        dao.softDelete(id)
    }

    override suspend fun allEntries(): List<FoodEntry> = dao.allActive().map { it.toFoodEntry() }

    override suspend fun deleteAllEntries() {
        dao.softDeleteAll()
    }

    override fun observeSuggestions(): Flow<List<FoodSuggestion>> =
        combine(dao.observeRecent(RECENT_LIMIT), favoriteDao.observeFavorites()) { recents, favorites ->
            mergeSuggestions(
                recents = recents.map { it.toSuggestion() },
                favorites = favorites.map { it.toSuggestion() },
            )
        }

    override suspend fun setFavorite(suggestion: FoodSuggestion, favorite: Boolean) {
        if (favorite) favoriteDao.upsert(suggestion.toEntity()) else favoriteDao.clearFavorite(suggestion.name)
    }

    override fun observeDailyNutrition(): Flow<List<DayNutrition>> {
        // Anchored on today here, not in the feature layer: todayEpochDay() is internal to this
        // module, and the window has to match the query's lower bound exactly for the series to
        // stay dense.
        val today = todayEpochDay()
        val from = today - TREND_WINDOW_DAYS
        return dao.observeSince(from).map { entities ->
            entities.map { it.toFoodEntry() }.dailySeries(fromEpochDay = from, toEpochDay = today)
        }
    }
}

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
)

private fun FoodEntryEntity.toSuggestion() = FoodSuggestion(
    name = name,
    portionAmount = portionAmount,
    portionUnit = portionUnit,
    calories = calories,
    proteinG = proteinG,
    carbsG = carbsG,
    fatG = fatG,
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
    isFavorite = true,
)

private fun FoodSuggestion.toEntity() = FavoriteFoodEntity(
    name = name,
    portionAmount = portionAmount,
    portionUnit = portionUnit,
    calories = calories,
    proteinG = proteinG,
    carbsG = carbsG,
    fatG = fatG,
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
)
