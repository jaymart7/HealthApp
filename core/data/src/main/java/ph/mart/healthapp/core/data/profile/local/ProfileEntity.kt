package ph.mart.healthapp.core.data.profile.local

import androidx.room3.Entity
import androidx.room3.PrimaryKey

/** Single-row table — there is exactly one profile per install, fixed at id 0. Enum fields are
 * stored by name (String) rather than via a TypeConverter; mapping happens in the repository. */
@Entity(tableName = "profile")
internal data class ProfileEntity(
    @PrimaryKey val id: Int = 0,
    val sex: String,
    val age: Int,
    val heightCm: Double,
    val weightKg: Double,
    val activityLevel: String,
    val goal: String,
    val targetWeightKg: Double?,
    val dietaryPreference: String?,
    val preferredUnit: String,
    val calorieOverrideKcal: Int?,
    val proteinOverrideG: Int?,
    val carbsOverrideG: Int?,
    val fatOverrideG: Int?,
    val mealRemindersOn: Boolean,
    val weighInReminderOn: Boolean,
    val photoReminderOn: Boolean,
    val waterRemindersOn: Boolean,
    val waterGoalGlasses: Int,
    val addExerciseToBudget: Boolean = true,
    val fastingGoalHours: Int = 16,
    val fastingRemindersOn: Boolean = false,
    val darkThemeOn: Boolean? = null,
)
