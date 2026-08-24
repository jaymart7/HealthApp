package ph.mart.healthapp.core.data.profile

import kotlin.math.roundToInt

private val ACTIVITY_MULTIPLIER = mapOf(
    ActivityLevel.Sedentary to 1.2,
    ActivityLevel.Light to 1.375,
    ActivityLevel.Moderate to 1.55,
    ActivityLevel.Very to 1.725,
)

private val GOAL_ADJUSTMENT_KCAL = mapOf(
    Goal.Lose to -500,
    Goal.Maintain to 0,
    Goal.Build to 300,
)

const val FEMALE_CALORIE_FLOOR = 1200
const val MALE_CALORIE_FLOOR = 1500

data class DailyTargets(val calories: Int, val proteinG: Int, val carbsG: Int, val fatG: Int, val floor: Int)

/**
 * Mifflin–St Jeor BMR -> TDEE -> goal-adjusted calories, clamped to the safety floor, then a
 * fixed 30/40/30 protein/carbs/fat split at 4/4/9 kcal per gram. Ported 1:1 from the prototype's
 * `calcDailyTargets()` (`theme.js`) — same formula, same floor, same split.
 */
fun calculateDailyTargets(profile: Profile): DailyTargets {
    val isMale = profile.sex == Sex.Male
    val bmr = if (isMale) {
        10 * profile.weightKg + 6.25 * profile.heightCm - 5 * profile.age + 5
    } else {
        10 * profile.weightKg + 6.25 * profile.heightCm - 5 * profile.age - 161
    }
    val tdee = bmr * (ACTIVITY_MULTIPLIER[profile.activityLevel] ?: 1.2)
    val floor = if (isMale) MALE_CALORIE_FLOOR else FEMALE_CALORIE_FLOOR
    val calories = (tdee + (GOAL_ADJUSTMENT_KCAL[profile.goal] ?: 0)).roundToInt().coerceAtLeast(floor)
    return DailyTargets(
        calories = calories,
        proteinG = (calories * 0.30 / 4).roundToInt(),
        carbsG = (calories * 0.40 / 4).roundToInt(),
        fatG = (calories * 0.30 / 9).roundToInt(),
        floor = floor,
    )
}

/** The targets actually shown to the user: computed live from [profile], with any manual
 * Confirm-step overrides layered on top — never a second cached copy of the computed value. */
fun Profile.dailyTargets(): DailyTargets {
    val base = calculateDailyTargets(this)
    return DailyTargets(
        calories = calorieOverrideKcal ?: base.calories,
        proteinG = proteinOverrideG ?: base.proteinG,
        carbsG = carbsOverrideG ?: base.carbsG,
        fatG = fatOverrideG ?: base.fatG,
        floor = base.floor,
    )
}
