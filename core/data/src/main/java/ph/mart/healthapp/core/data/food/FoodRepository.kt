package ph.mart.healthapp.core.data.food

import android.graphics.Bitmap
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
    val nutrients: Nutrients = Nutrients(),
    /** The plate, on disk. Set by [FoodRepository.addEntry] when the camera flow hands it a
     * bitmap, carried through an edit, and never written by any other logging path. */
    val photoPath: String? = null,
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
    val nutrients: Nutrients = Nutrients(),
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
// Stays in Kotlin, and must: this exact string is written into `food_entry.name` and is what
// `FoodEntryDao.observeRecent` excludes by. A resource would change what a device already holds.
const val QUICK_ADD_NAME = "Quick add"

/**
 * [foodCount] and [foodsWithMicronutrients] are what keep the graded nutrient panel honest. `0`
 * means unknown-or-none in every nutrient field in this app, so a day totalling 2 mg of iron might
 * be a genuinely iron-poor day or six foods the built-in list has no figure for. The panel says
 * which by naming how many of the day's foods carried data — see [Nutrients.hasMicronutrients].
 */
data class DiaryTotals(
    val calories: Int,
    val proteinG: Int,
    val carbsG: Int,
    val fatG: Int,
    val nutrients: Nutrients = Nutrients(),
    val foodCount: Int = 0,
    val foodsWithMicronutrients: Int = 0,
)

/** Diary aggregation is a pure fold over the (small, single-day) entry list — not a stored
 * entity or a second Room query. */
fun List<FoodEntry>.dailyTotals(): DiaryTotals = fold(DiaryTotals(0, 0, 0, 0)) { acc, entry ->
    DiaryTotals(
        calories = acc.calories + entry.calories,
        proteinG = acc.proteinG + entry.proteinG,
        carbsG = acc.carbsG + entry.carbsG,
        fatG = acc.fatG + entry.fatG,
        nutrients = acc.nutrients + entry.nutrients,
        foodCount = acc.foodCount + 1,
        foodsWithMicronutrients = acc.foodsWithMicronutrients +
            if (entry.nutrients.hasMicronutrients) 1 else 0,
    )
}

/**
 * How many meal photos are kept. The images are the only part of this app that grows without a
 * ceiling — a row of numbers costs bytes, a plate costs tens of kilobytes — so the newest this many
 * survive and the rest are dropped on the next write. The *meals* are never pruned: only the
 * picture ages out.
 */
const val MAX_MEAL_PHOTOS = 500

/** What a meal photo is stored at, on its long edge. A capture arrives at 1280 (see
 * `MAX_CAPTURE_EDGE`), which is the right size for one bitmap on screen and the wrong size for five
 * hundred on disk; 768 still fills the gallery's full-frame viewer without upscaling. */
const val MEAL_PHOTO_EDGE = 768

/**
 * How many rows the diary's history search hands back. A cap rather than the whole table, the rule
 * every read in `FoodEntryDao` follows — and high enough that the list is a history rather than a
 * window, since nothing above it counts.
 */
const val MAX_HISTORY_RESULTS = 200

/**
 * Wraps a search term as a LIKE "contains" pattern, escaping the three characters SQLite reads as
 * syntax: `%` and `_` are its wildcards and `\` is the escape character named by
 * `FoodEntryDao.searchByName`'s `ESCAPE` clause. Without this, searching for "100%" matches every
 * row in the table.
 *
 * Here rather than in the impl because it is the one piece of this query that is pure, and
 * `LikeContainsTest` is what keeps it and the `ESCAPE` clause in step.
 */
internal fun likeContains(query: String): String =
    "%" + query.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%"

interface FoodRepository {
    fun observeTodayEntries(): Flow<List<FoodEntry>>

    /** One day's entries — the diary, which can be pointed at any past day. */
    fun observeEntries(dateEpochDay: Long): Flow<List<FoodEntry>>

    /**
     * Logs one entry, keeping [photo] if there is one.
     *
     * The bitmap rather than a path, because writing the file is this layer's job: it is the layer
     * that knows where meal photos live, what they are scaled to, and how many are kept. Every
     * caller but the camera flow omits it, which is the whole rule for what gets a picture.
     */
    suspend fun addEntry(entry: FoodEntry, photo: Bitmap? = null)

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

    /**
     * Logged entries whose name contains [query], newest first and capped at
     * [MAX_HISTORY_RESULTS] — the diary's history search, which is the one read in this app that
     * looks across days. A blank [query] is the newest rows, not an error: the screen opens on it.
     *
     * A suspend one-shot, unlike every other read here, because its input is a text field — see
     * `FoodEntryDao.searchByName`.
     */
    suspend fun searchEntries(query: String): List<FoodEntry>

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

    /** The meals that still have their photo, newest first, capped at [MAX_MEAL_PHOTOS] — the
     * meal-photo history on the Progress tab's Food page. */
    fun observeMealPhotos(): Flow<List<FoodEntry>>

    /** Dense daily nutrition for the last [TREND_WINDOW_DAYS], oldest first, ending today — the
     * Progress tab's Nutrition series. */
    fun observeDailyNutrition(): Flow<List<DayNutrition>>

}
