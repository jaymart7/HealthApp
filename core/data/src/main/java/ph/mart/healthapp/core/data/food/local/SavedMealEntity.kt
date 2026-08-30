package ph.mart.healthapp.core.data.food.local

import androidx.room3.Entity
import androidx.room3.PrimaryKey

/**
 * Parent row of a saved meal *or* a recipe — [servings] is the discriminator: null means a saved
 * meal (a snapshot of a diary section, re-logged as one row per item), non-null means a recipe (a
 * dish authored from scratch, logged as a single row scaled to one serving). The two share this
 * table and [SavedMealItemEntity] because the only structural difference between them is that
 * column; the reads are kept apart by two queries rather than two tables, so neither list can
 * evict the other from its own newest-N window.
 *
 * Soft delete lives here only — [SavedMealItemEntity] rows of a deleted parent are simply never
 * grouped in, since the join is driven by this table. That is also why the items table needs no
 * discriminator of its own.
 */
@Entity(tableName = "saved_meal")
internal data class SavedMealEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long,
    val isDeleted: Boolean = false,
    val servings: Int? = null,
)

/** One food inside a saved meal, or one ingredient of a recipe. [mealId] is a plain column, not a
 * foreign key: the parent/child join is a Kotlin fold over two small lists (same reasoning as
 * `mergeSuggestions`), so Room never needs the relation. */
@Entity(tableName = "saved_meal_item")
internal data class SavedMealItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val mealId: Long,
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
)
