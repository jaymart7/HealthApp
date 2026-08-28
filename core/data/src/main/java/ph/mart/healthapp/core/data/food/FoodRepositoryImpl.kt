package ph.mart.healthapp.core.data.food

import java.util.Calendar
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import ph.mart.healthapp.core.data.food.local.FavoriteFoodDao
import ph.mart.healthapp.core.data.food.local.FavoriteFoodEntity
import ph.mart.healthapp.core.data.food.local.FoodEntryDao
import ph.mart.healthapp.core.data.food.local.FoodEntryEntity

/** Recents are read a little deeper than [MAX_SUGGESTIONS], so favorites crowding the front of
 * the merged list don't starve it of recents. */
private const val RECENT_LIMIT = MAX_SUGGESTIONS * 2

internal class FoodRepositoryImpl(
    private val dao: FoodEntryDao,
    private val favoriteDao: FavoriteFoodDao,
) : FoodRepository {

    override fun observeTodayEntries(): Flow<List<FoodEntry>> =
        dao.observeForDate(todayEpochDay()).map { entities -> entities.map { it.toFoodEntry() } }

    override suspend fun addEntry(entry: FoodEntry) {
        // A dated entry only ever arrives from an import; everything logged in-app is "today".
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
}

// ponytail: local-midnight epoch day via Calendar, not java.time.LocalDate — the project has no
// core-library-desugaring configured, and this phase only ever needs "today". Switch to
// LocalDate + desugaring when Phase 6's date-pickers need arbitrary-date math.
private fun todayEpochDay(): Long {
    val calendar = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    return calendar.timeInMillis / 86_400_000L
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
