package ph.mart.healthapp.core.data.food.local

import androidx.room3.Embedded
import androidx.room3.Entity
import androidx.room3.PrimaryKey
import ph.mart.healthapp.core.data.food.Nutrients

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
    @Embedded val nutrients: Nutrients = Nutrients(),
    /** The plate this meal was logged from, on disk — null for every entry that reached the diary
     * any other way. Only the camera flow writes one, and only [MAX_MEAL_PHOTOS][ph.mart.healthapp.core.data.food.MAX_MEAL_PHOTOS]
     * of them survive: an aged-out photo nulls this column and leaves the meal itself untouched. */
    val photoPath: String? = null,
    val isDeleted: Boolean = false,
)
