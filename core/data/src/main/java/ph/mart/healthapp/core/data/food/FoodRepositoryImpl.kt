package ph.mart.healthapp.core.data.food

import java.util.Calendar
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ph.mart.healthapp.core.data.food.local.FoodEntryDao
import ph.mart.healthapp.core.data.food.local.FoodEntryEntity

internal class FoodRepositoryImpl(private val dao: FoodEntryDao) : FoodRepository {

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
