package ph.mart.healthapp.core.data.profile

import androidx.annotation.StringRes
import kotlin.math.roundToInt
import ph.mart.healthapp.core.data.R

private val ACTIVITY_MULTIPLIER = mapOf(
    ActivityLevel.Sedentary to 1.2,
    ActivityLevel.Light to 1.375,
    ActivityLevel.Moderate to 1.55,
    ActivityLevel.Very to 1.725,
)

/** Internal rather than file-private because [ph.mart.healthapp.core.data.profile.energyCheckIn]
 * prices its recommendation off the same adjustment this does — a second copy is a second answer. */
internal val GOAL_ADJUSTMENT_KCAL = mapOf(
    Goal.Lose to -500,
    Goal.Maintain to 0,
    Goal.Build to 300,
)

const val FEMALE_CALORIE_FLOOR = 1200
const val MALE_CALORIE_FLOOR = 1500

/** The clamp on a *manual* calorie target, which is a different thing from the safety floor: the
 * floor warns, this bounds. Both ends only ever come from a stepper, so it sits at the edges the
 * way [ph.mart.healthapp.core.data.water.WATER_GOAL_GLASSES] does rather than validating after. */
val CALORIE_TARGET_KCAL = 800..6000

/** Shown wherever a manual target drops under the Mifflin–St Jeor floor. Warn, never block — and
 * one copy, because a safety warning that drifts between the two screens that show it is worse
 * than the layering smell of user-facing copy in `:core:data`. */
@StringRes val CALORIE_FLOOR_WARNING = R.string.data_calorie_floor_warning

data class DailyTargets(val calories: Int, val proteinG: Int, val carbsG: Int, val fatG: Int, val floor: Int)

val DailyTargets.belowFloor: Boolean get() = calories < floor

/** The fixed 30/40/30 protein/carbs/fat split at 4/4/9 kcal per gram, for whatever calorie figure
 * is actually in force — computed or manual. */
private fun targetsFor(calories: Int, floor: Int) = DailyTargets(
    calories = calories,
    proteinG = (calories * 0.30 / 4).roundToInt(),
    carbsG = (calories * 0.40 / 4).roundToInt(),
    fatG = (calories * 0.30 / 9).roundToInt(),
    floor = floor,
)

/**
 * Mifflin–St Jeor BMR -> TDEE -> goal-adjusted calories, clamped to the safety floor, then the
 * [targetsFor] split. Ported 1:1 from the prototype's `calcDailyTargets()` (`theme.js`) — same
 * formula, same floor, same split.
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
    return targetsFor(calories, floor)
}

/** The targets actually shown to the user: computed live from [Profile], with any manual overrides
 * layered on top — never a second cached copy of the computed value.
 *
 * A manual calorie target reprices the split rather than sitting above the computed one: 1800 kcal
 * printed over a macro bar summing to 2400 is two numbers that disagree. Per-macro overrides still
 * win on top of the repriced figures, and with no calorie override this is exactly
 * [calculateDailyTargets]. */
fun Profile.dailyTargets(): DailyTargets {
    val base = calculateDailyTargets(this)
    val split = calorieOverrideKcal?.let { targetsFor(it, base.floor) } ?: base
    return DailyTargets(
        calories = split.calories,
        proteinG = proteinOverrideG ?: split.proteinG,
        carbsG = carbsOverrideG ?: split.carbsG,
        fatG = fatOverrideG ?: split.fatG,
        floor = base.floor,
    )
}
