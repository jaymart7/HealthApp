package ph.mart.healthapp.core.data.profile

import kotlinx.coroutines.flow.Flow
import ph.mart.healthapp.core.data.fasting.DEFAULT_FAST_GOAL_HOURS
import ph.mart.healthapp.core.data.health.DEFAULT_STEP_GOAL
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
    val supplementRemindersOn: Boolean = false,
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
    /** The name of the mascot the user picked in Profile → Appearance; null means the default, the
     * same reading [darkThemeOn]'s null has. Held as a String rather than the enum because
     * `MascotCharacter` lives in `:core:designsystem`, which this module does not depend on. */
    val mascotName: String? = null,
    /** The name of the mascot colour picked in the same place, held as a String for the same
     * reason [mascotName] is. Null means the default, and it applies to every buddy at once — the
     * character and the colour are two choices, not five colours. */
    val mascotPaletteName: String? = null,
    /**
     * The daily step target, in steps. Deliberately *not* snapshotted per day, unlike
     * [ph.mart.healthapp.core.data.fasting.FastSession.goalHours]: `step_day` rows belong to the
     * watch and are REPLACEd wholesale on every re-sync, so a target stored beside them would be
     * overwritten by the next import. Raising the goal therefore re-scores past days, and the
     * Progress stat that counts them says it means today's goal.
     */
    val stepGoal: Int = DEFAULT_STEP_GOAL,
    /**
     * Which Home cards show, and in what order, as the comma-separated string
     * `ph.mart.healthapp.core.designsystem.component.homeCardLayout` parses (a `-` prefix hides
     * one). Held as a String rather than the card list for the same reason [mascotName] is: the
     * card vocabulary lives in `:core:designsystem`, which this module does not depend on. Null
     * means the default order with nothing hidden — the same reading [mascotName]'s null has, and
     * what "Reset to default" writes back.
     */
    val homeLayout: String? = null,
)

interface ProfileRepository {
    fun observeProfile(): Flow<Profile?>
    suspend fun saveProfile(profile: Profile)
}
