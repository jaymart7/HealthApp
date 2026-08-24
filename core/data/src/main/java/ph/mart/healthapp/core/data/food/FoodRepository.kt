package ph.mart.healthapp.core.data.food

import kotlinx.coroutines.flow.Flow

enum class MealType { Breakfast, Lunch, Dinner, Snacks }

data class FoodEntry(
    val id: Long = 0,
    val name: String,
    val mealType: MealType,
    val portionAmount: Double,
    val portionUnit: String,
    val calories: Int,
    val proteinG: Int,
    val carbsG: Int,
    val fatG: Int,
)

data class DiaryTotals(val calories: Int, val proteinG: Int, val carbsG: Int, val fatG: Int)

/** Diary aggregation is a pure fold over the (small, single-day) entry list — not a stored
 * entity or a second Room query. */
fun List<FoodEntry>.dailyTotals(): DiaryTotals = fold(DiaryTotals(0, 0, 0, 0)) { acc, entry ->
    DiaryTotals(
        calories = acc.calories + entry.calories,
        proteinG = acc.proteinG + entry.proteinG,
        carbsG = acc.carbsG + entry.carbsG,
        fatG = acc.fatG + entry.fatG,
    )
}

interface FoodRepository {
    fun observeTodayEntries(): Flow<List<FoodEntry>>
    suspend fun addEntry(entry: FoodEntry)
    suspend fun deleteEntry(id: Long)
}
