package ph.mart.healthapp.core.data.food.local

import androidx.room3.Entity
import androidx.room3.PrimaryKey

/** [calories] is independently editable (not derived from the macro fields) — matches the
 * prototype's Confirmation screen, where an AI estimate's calorie stepper and macro fields are
 * nudged separately. */
@Entity(tableName = "food_entry")
internal data class FoodEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val mealType: String,
    val date: Long,
    val loggedAt: Long,
    val portionAmount: Double,
    val portionUnit: String,
    val calories: Int,
    val proteinG: Int,
    val carbsG: Int,
    val fatG: Int,
    val fiberG: Int = 0,
    val sugarG: Int = 0,
    val sodiumMg: Int = 0,
    val isDeleted: Boolean = false,
)
