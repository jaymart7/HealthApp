package ph.mart.healthapp.core.data.food

import kotlinx.coroutines.flow.Flow

enum class MealType { Breakfast, Lunch, Dinner, Snacks }

/** [dateEpochDay] is 0 for a not-yet-stored entry — the repository stamps today on insert.
 * A non-zero value is only ever set by a read, or by an import restoring a dated entry. */
data class FoodEntry(
    val id: Long = 0,
    val name: String,
    val dateEpochDay: Long = 0,
    val mealType: MealType,
    val portionAmount: Double,
    val portionUnit: String,
    val calories: Int,
    val proteinG: Int,
    val carbsG: Int,
    val fatG: Int,
)

/** A one-tap re-log candidate in the add-entry sheet: either a recently logged food (derived
 * from the diary itself — nothing extra is written when you log) or a starred favorite. [name] is
 * the identity; two suggestions with the same name, ignoring case, are the same food. */
data class FoodSuggestion(
    val name: String,
    val portionAmount: Double,
    val portionUnit: String,
    val calories: Int,
    val proteinG: Int,
    val carbsG: Int,
    val fatG: Int,
    val isFavorite: Boolean,
)

/** Favorites lead, then recents that aren't already starred. A pure fold over two small lists —
 * not a third Room query joining them, same reasoning as [dailyTotals]. */
fun mergeSuggestions(
    recents: List<FoodSuggestion>,
    favorites: List<FoodSuggestion>,
    limit: Int = MAX_SUGGESTIONS,
): List<FoodSuggestion> {
    val starred = favorites.mapTo(mutableSetOf()) { it.name.lowercase() }
    return (favorites + recents.filterNot { it.name.lowercase() in starred }).take(limit)
}

/** Matches FoodSearchPanel's visible-hit cap, and for the same reason: the list sits in a bottom
 * sheet above the entry form, and a longer one pushes the form off-screen. */
const val MAX_SUGGESTIONS = 5

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

    /** Full history, oldest first — for data export. The diary itself never needs this. */
    suspend fun allEntries(): List<FoodEntry>

    /** Soft-deletes every entry, for import's replace-in-full semantics. */
    suspend fun deleteAllEntries()

    /** Re-log candidates for the add-entry sheet: starred favorites first, then recently logged
     * foods, deduped by name. */
    fun observeSuggestions(): Flow<List<FoodSuggestion>>

    suspend fun setFavorite(suggestion: FoodSuggestion, favorite: Boolean)

    /** Dense daily nutrition for the last [TREND_WINDOW_DAYS], oldest first, ending today — the
     * Progress tab's Nutrition series. */
    fun observeDailyNutrition(): Flow<List<DayNutrition>>

}
