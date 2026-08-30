package ph.mart.healthapp.core.data.profile

import kotlinx.coroutines.flow.Flow
import ph.mart.healthapp.core.data.fasting.DEFAULT_FAST_GOAL_HOURS
import ph.mart.healthapp.core.data.water.DEFAULT_WATER_GOAL_GLASSES

enum class Sex { Male, Female }
enum class ActivityLevel { Sedentary, Light, Moderate, Very }
enum class Goal { Lose, Maintain, Build }
enum class DietaryPreference { None, Vegetarian, Vegan, Other }
enum class UnitSystem { Metric, Imperial }

/** The user's onboarding profile — the single source of truth for Mifflin–St Jeor inputs across
 * Home, Progress, and Profile. [calorieOverrideKcal]/[proteinOverrideG]/[carbsOverrideG]/
 * [fatOverrideG] are explicit manual adjustments made on the Confirm step; see [dailyTargets]. */
data class Profile(
    val sex: Sex,
    val age: Int,
    val heightCm: Double,
    val weightKg: Double,
    val activityLevel: ActivityLevel,
    val goal: Goal,
    val targetWeightKg: Double? = null,
    val dietaryPreference: DietaryPreference? = null,
    val preferredUnit: UnitSystem = UnitSystem.Metric,
    val calorieOverrideKcal: Int? = null,
    val proteinOverrideG: Int? = null,
    val carbsOverrideG: Int? = null,
    val fatOverrideG: Int? = null,
    val mealRemindersOn: Boolean = true,
    val weighInReminderOn: Boolean = true,
    val photoReminderOn: Boolean = false,
    val waterRemindersOn: Boolean = false,
    val waterGoalGlasses: Int = DEFAULT_WATER_GOAL_GLASSES,
    /** The intermittent-fasting target, in hours. Snapshotted onto each fast at start — see
     * [ph.mart.healthapp.core.data.fasting.FastSession]. */
    val fastingGoalHours: Int = DEFAULT_FAST_GOAL_HOURS,
    /** Off by default, like the photo and water reminders: a notification nobody asked for is
     * worse than one they have to go and find. */
    val fastingRemindersOn: Boolean = false,
    /** Whether logged exercise raises the day's shown calorie budget — see
     * [ph.mart.healthapp.core.data.exercise.budgetKcal]. On by default; off for users whose
     * activity level already accounts for their workouts. */
    val addExerciseToBudget: Boolean = true,
    /** null means follow the system setting; it only becomes explicit once the user touches the
     * Dark mode switch in Profile. A plain `false` default would force light on a device that is
     * in dark mode. */
    val darkThemeOn: Boolean? = null,
)

interface ProfileRepository {
    fun observeProfile(): Flow<Profile?>
    suspend fun saveProfile(profile: Profile)
}
