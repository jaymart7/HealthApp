package ph.mart.healthapp.core.data.food.local

import androidx.room3.Entity
import androidx.room3.PrimaryKey

/** Parent row of a saved meal. Soft delete lives here only — [SavedMealItemEntity] rows of a
 * deleted meal are simply never grouped in, since the join is driven by this table. */
@Entity(tableName = "saved_meal")
internal data class SavedMealEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long,
    val isDeleted: Boolean = false,
)

/** One food inside a saved meal. [mealId] is a plain column, not a foreign key: the parent/child
 * join is a Kotlin fold over two small lists (same reasoning as `mergeSuggestions`), so Room never
 * needs the relation. */
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
)
