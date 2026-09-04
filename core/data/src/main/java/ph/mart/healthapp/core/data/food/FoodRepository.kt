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
    val fiberG: Int = 0,
    val sugarG: Int = 0,
    val sodiumMg: Int = 0,
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
    val fiberG: Int = 0,
    val sugarG: Int = 0,
    val sodiumMg: Int = 0,
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

/** What an entry logged without a name is stored as — the diary's escape hatch for a meal the
 * user isn't going to look up. It lives here rather than in `:feature:food` because the recents
 * query has to exclude it; see [FoodEntryDao.observeRecent][ph.mart.healthapp.core.data.food.local.FoodEntryDao]. */
const val QUICK_ADD_NAME = "Quick add"

data class DiaryTotals(
    val calories: Int,
    val proteinG: Int,
    val carbsG: Int,
    val fatG: Int,
    val fiberG: Int = 0,
    val sugarG: Int = 0,
    val sodiumMg: Int = 0,
)

/** Diary aggregation is a pure fold over the (small, single-day) entry list — not a stored
 * entity or a second Room query. */
fun List<FoodEntry>.dailyTotals(): DiaryTotals = fold(DiaryTotals(0, 0, 0, 0)) { acc, entry ->
    DiaryTotals(
        calories = acc.calories + entry.calories,
        proteinG = acc.proteinG + entry.proteinG,
        carbsG = acc.carbsG + entry.carbsG,
        fatG = acc.fatG + entry.fatG,
        fiberG = acc.fiberG + entry.fiberG,
        sugarG = acc.sugarG + entry.sugarG,
        sodiumMg = acc.sodiumMg + entry.sodiumMg,
    )
}

interface FoodRepository {
    fun observeTodayEntries(): Flow<List<FoodEntry>>

    /** One day's entries — the diary, which can be pointed at any past day. */
    fun observeEntries(dateEpochDay: Long): Flow<List<FoodEntry>>
    suspend fun addEntry(entry: FoodEntry)

    /** Logs several foods as one write, so a saved meal lands in the diary in a single emission
     * instead of appearing item by item. */
    suspend fun addEntries(entries: List<FoodEntry>)
    /**
     * Corrects a logged entry. The corrected row *supersedes* the old one — soft delete plus a
     * fresh insert in one transaction — so [FoodEntry.id] changes while the row's place in the
     * day does not: it keeps the original logging time.
     *
     * That is also what keeps Google Health honest without a line of sync code. A push skips
     * entries it has already sent, so an in-place update would leave the remote copy stale
     * forever; retiring the id lets the existing delete-then-push pass do the right thing.
     */
    suspend fun updateEntry(entry: FoodEntry)
    suspend fun deleteEntry(id: Long)

    /** Full history, oldest first — for data export. The diary itself never needs this. */
    suspend fun allEntries(): List<FoodEntry>

    /** Soft-deletes every entry, for import's replace-in-full semantics. */
    suspend fun deleteAllEntries()

    /** Re-log candidates for the add-entry sheet: starred favorites first, then recently logged
     * foods, deduped by name. */
    fun observeSuggestions(): Flow<List<FoodSuggestion>>

    /**
     * Stars or un-stars a food — and, since a starred food *is* a food the user owns, this is also
     * the whole write path behind the food library: authoring one from the add-entry sheet is the
     * same upsert with no diary row behind it, and deleting one from Profile is the same
     * soft delete. [FoodSuggestion.name] is `favorite_food`'s primary key, so saving a food whose
     * name already exists edits that row rather than creating a rival. Don't add a twin for
     * "custom foods": there is one table and one concept. [deleteMyFood] is the same soft delete
     * by name alone, for the library screen, which holds no suggestion to pass.
     */
    suspend fun setFavorite(suggestion: FoodSuggestion, favorite: Boolean)

    /**
     * Every food the user owns, by name — what the food search leads with, ahead of
     * [COMMON_FOODS], and what the food library lists. Unlike [observeSuggestions] this is neither
     * merged with recents nor capped: the panel's window is what keeps the add-entry sheet short,
     * and a food the user authored has to stay findable however many they have.
     */
    fun observeMyFoods(): Flow<List<ScannedProduct>>

    /** Un-stars a food by name — [setFavorite]'s soft delete, for the library screen. Anything
     * already logged from it stays in the diary: a `favorite_food` row never was the log. */
    suspend fun deleteMyFood(name: String)

    /** Moves a food to a new name. Name is its identity, so this retires the old row rather than
     * updating a column — see `FavoriteFoodDao.rename`. */
    suspend fun renameMyFood(oldName: String, newName: String)

    /** The newest [MAX_SAVED_MEALS] saved meals, each with its items. */
    fun observeSavedMeals(): Flow<List<SavedMeal>>

    suspend fun saveMeal(name: String, items: List<SavedMealItem>)

    /** Every saved meal, newest first. The panel's newest-[MAX_SAVED_MEALS] window is what keeps
     * the add-entry sheet short; this is the read that can reach past it, for the library screen
     * where a meal saved months ago is still deletable. */
    fun observeAllSavedMeals(): Flow<List<SavedMeal>>

    suspend fun deleteSavedMeal(id: Long)

    suspend fun renameSavedMeal(id: Long, name: String)

    /** The newest [MAX_RECIPES] recipes, each with its ingredients. Recipes and saved meals share
     * a table but never a list — see `SavedMealDao`. */
    fun observeRecipes(): Flow<List<Recipe>>

    suspend fun saveRecipe(name: String, servings: Int, items: List<SavedMealItem>)

    /** Twin of [observeAllSavedMeals], and for the same reason. */
    fun observeAllRecipes(): Flow<List<Recipe>>

    suspend fun deleteRecipe(id: Long)

    suspend fun renameRecipe(id: Long, name: String)

    /** Dense daily nutrition for the last [TREND_WINDOW_DAYS], oldest first, ending today — the
     * Progress tab's Nutrition series. */
    fun observeDailyNutrition(): Flow<List<DayNutrition>>

}
